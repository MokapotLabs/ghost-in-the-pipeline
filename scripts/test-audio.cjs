const assert = require("node:assert/strict");
const {
  GhostAudio,
} = require("../app/src/main/resources/META-INF/resources/sound.js");
let timers = new Map(),
  next = 0,
  contexts = [];
function context() {
  const nodes = [];
  const parameter = () => ({
    value: 0,
    setValueAtTime(value) {
      this.value = value;
    },
    linearRampToValueAtTime() {},
  });
  const node = () => ({
    connect() {},
    disconnect() {
      this.disconnected = true;
    },
  });
  const c = {
    state: "suspended",
    currentTime: 0,
    destination: {},
    nodes,
    createGain() {
      return { ...node(), gain: parameter() };
    },
    createOscillator() {
      const o = {
        ...node(),
        frequency: parameter(),
        start() {},
        stop(at) {
          if (at === undefined) this.stopped = true;
        },
      };
      nodes.push(o);
      return o;
    },
    async resume() {
      this.state = "running";
    },
    async suspend() {
      this.state = "suspended";
    },
    async close() {
      this.state = "closed";
    },
  };
  contexts.push(c);
  return c;
}
const audio = new GhostAudio({
  createContext: context,
  setTimer: (fn, ms) => {
    timers.set(++next, { fn, ms });
    return next;
  },
  clearTimer: (id) => timers.delete(id),
});
const frame = (
  signal = "FAINT",
  generation = "one",
  status = "PLAYING",
  exit = false,
  exitsFound = 0,
) => ({
  ready: true,
  generation,
  status,
  level: 1,
  exitsFound,
  navigation: { signal },
  observation: { memory: [[0, 1, exit ? "E###" : "####", 7]] },
});
function fire(id) {
  const t = timers.get(id);
  timers.delete(id);
  t.fn();
}
(async () => {
  audio.update(frame());
  assert.equal(contexts.length, 0);
  assert.equal(timers.size, 0, "muted by default");
  await audio.setEnabled(true);
  assert.equal(audio.context.state, "running");
  assert.equal(audio.master.gain.value, 0.035);
  assert.equal(timers.get(audio.timer).ms, 4600);
  fire(audio.timer);
  assert.equal(audio.context.nodes[0].frequency.value, 180);
  const old = audio.context;
  await audio.setEnabled(false);
  assert.equal(timers.size, 0);
  assert.equal(old.state, "closed");
  assert(old.nodes.every((n) => n.stopped && n.disconnected));
  await audio.setEnabled(true);
  audio.update(frame("INTENSE"));
  assert.equal(timers.get(audio.timer).ms, 850);
  audio.reset();
  assert.equal(timers.size, 0);
  audio.update(frame("INTENSE"));
  assert.equal(timers.size, 0, "old reset generation remains silent");
  audio.update(frame("INTENSE", "two"));
  assert.equal(timers.size, 1);
  audio.update(frame("INTENSE", "two", "PLAYING", true));
  assert.equal(audio.nodes.size, 2, "discovery has two quiet tones");
  audio.update(frame("INTENSE", "two", "LOST", true));
  assert.equal(audio.context, null);
  assert.equal(timers.size, 0, "game over cleans up");
  audio.update(frame("STRONG", "three"));
  await audio.unlock();
  audio.update(frame("INTENSE", "three", "WON", true, 1));
  assert.equal(audio.nodes.size, 3, "crossing has a distinct chime");
  const end = audio.endTimer;
  audio.update(frame("INTENSE", "three", "WON", true, 1));
  assert.equal(audio.endTimer, end, "duplicate frames do not clip the chime");
  fire(end);
  assert.equal(audio.context, null);
  assert.equal(audio.nodes.size, 0);
  assert.equal(timers.size, 0);
  audio.update(frame("STRONG", "four"));
  await audio.unlock();
  audio.destroy();
  assert.equal(audio.context, null);
  assert.equal(timers.size, 0);
  const nativeSet = global.setTimeout,
    nativeClear = global.clearTimeout;
  try {
    global.setTimeout = function (callback, delay) {
      "use strict";
      assert.equal(
        this,
        undefined,
        "timer adapter must not bind an AudioEngine receiver",
      );
      return ++next;
    };
    global.clearTimeout = function (timer) {
      "use strict";
      assert.equal(this, undefined);
    };
    const browserTimers = new GhostAudio({ createContext: context });
    browserTimers.update(frame());
    await browserTimers.setEnabled(true);
    browserTimers.destroy();
  } finally {
    global.setTimeout = nativeSet;
    global.clearTimeout = nativeClear;
  }
  console.log(
    "PASS: opt-in audio, conservative gain, frequency/spacing, discovery/crossing, mute, reset, game-over and destroy cleanup",
  );
})().catch((e) => {
  console.error(e);
  process.exitCode = 1;
});
