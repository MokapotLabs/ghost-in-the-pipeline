"use strict";
/** Original, quiet Web Audio tones. No samples, microphone or external assets. */
class GhostAudio {
  constructor({
    createContext = () =>
      new (window.AudioContext || window.webkitAudioContext)(),
    setTimer = (callback, delay) => setTimeout(callback, delay),
    clearTimer = (timer) => clearTimeout(timer),
  } = {}) {
    this.createContext = createContext;
    this.setTimer = setTimer;
    this.clearTimer = clearTimer;
    this.enabled = false;
    this.context = null;
    this.master = null;
    this.timer = null;
    this.endTimer = null;
    this.nodes = new Set();
    this.frame = null;
    this.band = null;
    this.suppressedGeneration = null;
  }
  async setEnabled(enabled) {
    this.enabled = enabled;
    if (!enabled) {
      this.stop(true);
      return;
    }
    await this.unlock();
  }
  async unlock() {
    if (!this.enabled) return;
    if (this.frame && this.frame.status !== "PLAYING") return;
    if (!this.context || this.context.state === "closed") {
      this.context = this.createContext();
      this.master = this.context.createGain();
      this.master.gain.value = 0.035;
      this.master.connect(this.context.destination);
    }
    const context = this.context;
    if (context.state === "suspended") await context.resume();
    if (this.enabled && this.context === context) this.schedule();
  }
  clearTimers() {
    if (this.timer !== null) this.clearTimer(this.timer);
    if (this.endTimer !== null) this.clearTimer(this.endTimer);
    this.timer = this.endTimer = null;
  }
  stop(close = false) {
    this.clearTimers();
    this.band = null;
    for (const node of this.nodes) {
      try {
        node.osc.stop();
      } catch (_) {
        /* already ended */
      }
      node.osc.disconnect();
      node.gain.disconnect();
    }
    this.nodes.clear();
    if (close && this.context) {
      this.master?.disconnect();
      this.context.close().catch(() => {});
      this.context = this.master = null;
    }
  }
  reset() {
    this.suppressedGeneration = this.frame?.generation;
    this.stop(false);
  }
  suspend() {
    this.stop(false);
    this.context?.suspend().catch(() => {});
  }
  destroy() {
    this.enabled = false;
    this.stop(true);
    this.frame = null;
  }
  tone(frequency, offset = 0, duration = 0.075) {
    if (!this.enabled || !this.context || this.context.state !== "running")
      return;
    const context = this.context,
      at = context.currentTime + offset;
    const osc = context.createOscillator(),
      gain = context.createGain();
    osc.type = "triangle";
    osc.frequency.setValueAtTime(frequency, at);
    gain.gain.setValueAtTime(0, at);
    gain.gain.linearRampToValueAtTime(0.3, at + 0.008);
    gain.gain.linearRampToValueAtTime(0, at + duration);
    osc.connect(gain);
    gain.connect(this.master);
    const node = { osc, gain };
    this.nodes.add(node);
    osc.onended = () => {
      osc.disconnect();
      gain.disconnect();
      this.nodes.delete(node);
    };
    osc.start(at);
    osc.stop(at + duration + 0.01);
  }
  chime(notes) {
    notes.forEach((note, i) => this.tone(note, i * 0.13, 0.12));
  }
  schedule() {
    const f = this.frame;
    if (
      !this.enabled ||
      !this.context ||
      this.context.state !== "running" ||
      !f?.ready ||
      f.status !== "PLAYING" ||
      f.generation === this.suppressedGeneration ||
      this.timer !== null
    )
      return;
    const settings = {
      FAINT: [4600, 180],
      NOTICEABLE: [3000, 220],
      STRONG: [1700, 280],
      INTENSE: [850, 360],
    };
    const [interval, pitch] = settings[f.navigation?.signal] || settings.FAINT;
    this.timer = this.setTimer(() => {
      this.timer = null;
      if (!this.enabled || this.frame?.status !== "PLAYING") return;
      this.tone(pitch);
      this.schedule();
    }, interval);
  }
  update(f) {
    const previous = this.frame;
    this.frame = f;
    const fresh = !previous || previous.generation !== f.generation;
    if (fresh) {
      this.stop(false);
      this.suppressedGeneration = null;
    }
    if (!f.ready || !this.enabled) return;
    if (f.status !== "PLAYING") {
      if (!fresh && previous.status === f.status) return;
      this.stop(false);
      if (f.status === "WON" && this.context?.state === "running") {
        this.chime([660, 880, 1100]);
        this.endTimer = this.setTimer(() => this.stop(true), 650);
      } else this.stop(true);
      return;
    }
    if (!fresh && f.exitsFound > previous.exitsFound)
      this.chime([660, 880, 1100]);
    else if (
      !fresh &&
      f.level === previous.level &&
      !previous.observation.memory.some((r) => r[2].includes("E")) &&
      f.observation.memory.some((r) => r[2].includes("E"))
    )
      this.chime([440, 660]);
    if (this.band !== f.navigation?.signal) {
      if (this.timer !== null) this.clearTimer(this.timer);
      this.timer = null;
      this.band = f.navigation?.signal;
    }
    this.schedule();
  }
}
if (typeof module !== "undefined") module.exports = { GhostAudio };
