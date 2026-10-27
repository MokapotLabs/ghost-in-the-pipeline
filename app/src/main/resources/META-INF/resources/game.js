"use strict";
const $ = (id) => document.getElementById(id),
  canvas = $("maze"),
  ctx = canvas.getContext("2d"),
  map = $("map").getContext("2d");
let frame = null,
  busy = false,
  lastKey = "",
  recentResults = [],
  lastCompleted = "";
const esc = (s) =>
  String(s).replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
async function api(path = "", body) {
  const r = await fetch("/api/game" + path, {
    method: body === undefined ? "GET" : "POST",
    headers: { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!r.ok)
    throw new Error(
      r.status === 409
        ? "Wait for this turn to finish."
        : "The game could not complete that request.",
    );
  return r.json();
}
function polygon(points, fill, stroke = "#54e6ef", width = 2) {
  ctx.beginPath();
  points.forEach(([x, y], i) => (i ? ctx.lineTo(x, y) : ctx.moveTo(x, y)));
  ctx.closePath();
  ctx.fillStyle = fill;
  ctx.fill();
  if (stroke) {
    ctx.strokeStyle = stroke;
    ctx.lineWidth = width;
    ctx.stroke();
  }
}
function line(x1, y1, x2, y2, color, width = 1) {
  ctx.beginPath();
  ctx.moveTo(x1, y1);
  ctx.lineTo(x2, y2);
  ctx.strokeStyle = color;
  ctx.lineWidth = width;
  ctx.stroke();
}
function draw(o) {
  const W = 960,
    H = 600;
  ctx.clearRect(0, 0, W, H);
  let bg = ctx.createLinearGradient(0, 0, 0, H);
  bg.addColorStop(0, "#030b15");
  bg.addColorStop(0.5, "#10202d");
  bg.addColorStop(1, "#121127");
  ctx.fillStyle = bg;
  ctx.fillRect(0, 0, W, H);
  const d = o.direction,
    front = o.edges[d],
    left = o.edges[(d + 3) % 4],
    right = o.edges[(d + 1) % 4];
  let fog = ctx.createRadialGradient(480, 290, 10, 480, 300, 440);
  fog.addColorStop(0, "#224c5266");
  fog.addColorStop(0.6, "#0a1b2533");
  fog.addColorStop(1, "#00000000");
  ctx.fillStyle = fog;
  ctx.fillRect(0, 0, W, H);
  // Original room geometry. Openings dissolve into fog; no unseen cell data is used.
  polygon(
    [
      [0, 0],
      [960, 0],
      [680, 180],
      [280, 180],
    ],
    "#08111d",
    "#30505c",
  );
  polygon(
    [
      [0, 600],
      [960, 600],
      [680, 420],
      [280, 420],
    ],
    "#11182a",
    "#38606e",
  );
  for (let i = -5; i <= 5; i++)
    line(480 + i * 195, 600, 480 + i * 32, 420, "#5d8d9b38");
  for (let y of [442, 478, 530, 598]) {
    line(0, y, 960, y, "#7879bf38");
  }
  const wallFill = ctx.createLinearGradient(0, 0, 400, 0);
  wallFill.addColorStop(0, "#102531");
  wallFill.addColorStop(1, "#173943");
  const wallFillR = ctx.createLinearGradient(650, 0, 960, 0);
  wallFillR.addColorStop(0, "#17333d");
  wallFillR.addColorStop(1, "#0b202d");
  function side(isLeft, edge) {
    const flip = (ps) => ps.map(([x, y]) => [isLeft ? x : 960 - x, y]);
    if (edge === "#") {
      polygon(
        flip([
          [0, 0],
          [280, 180],
          [280, 420],
          [0, 600],
        ]),
        isLeft ? wallFill : wallFillR,
        "#69ede5",
        2,
      );
      for (let t of [0.25, 0.5, 0.75]) {
        let x = 280 * t;
        line(
          isLeft ? x : 960 - x,
          180 * t,
          isLeft ? x : 960 - x,
          600 - 180 * t,
          "#4498a590",
          1,
        );
      }
      for (let t of [0.25, 0.5, 0.75]) {
        line(
          isLeft ? 0 : 960,
          600 * t,
          isLeft ? 280 : 680,
          180 + 240 * t,
          "#57a6b477",
          1,
        );
      }
    } else {
      polygon(
        flip([
          [0, 0],
          [280, 180],
          [280, 420],
          [0, 600],
        ]),
        "#040e1b",
        "#346d7b",
        2,
      );
      polygon(
        flip([
          [0, 0],
          [76, 49],
          [76, 551],
          [0, 600],
        ]),
        "#12313a",
        "#70e8e2",
        2,
      );
      polygon(
        flip([
          [236, 152],
          [280, 180],
          [280, 420],
          [236, 448],
        ]),
        "#193d45",
        "#70e8e2",
        2,
      );
      line(isLeft ? 76 : 884, 106, isLeft ? 236 : 724, 193, "#40919a", 2);
      line(isLeft ? 76 : 884, 494, isLeft ? 236 : 724, 407, "#40919a", 2);
      if (edge === "E") exitPortal(isLeft ? 154 : 806, 310, 0.5);
    }
  }
  side(true, left);
  side(false, right);
  if (front === "#") {
    let grad = ctx.createLinearGradient(280, 180, 680, 420);
    grad.addColorStop(0, "#1d4048");
    grad.addColorStop(1, "#102832");
    polygon(
      [
        [280, 180],
        [680, 180],
        [680, 420],
        [280, 420],
      ],
      grad,
      "#9cffe7",
      2,
    );
    for (let x of [380, 480, 580]) line(x, 180, x, 420, "#568f9870");
    for (let y of [240, 300, 360]) line(280, y, 680, y, "#568f9870");
    // Subtle original glyph: a trace of the machine, not a copied game asset.
    ctx.fillStyle = "#7fe2cf22";
    ctx.fillRect(465, 267, 30, 4);
    ctx.fillRect(478, 255, 4, 30);
  } else {
    polygon(
      [
        [280, 180],
        [680, 180],
        [680, 420],
        [280, 420],
      ],
      "#05101c",
      "#95ffe5",
      2,
    );
    polygon(
      [
        [280, 180],
        [332, 210],
        [332, 390],
        [280, 420],
      ],
      "#16323e",
      "#4d9aa6",
      1,
    );
    polygon(
      [
        [680, 180],
        [628, 210],
        [628, 390],
        [680, 420],
      ],
      "#132b37",
      "#4d9aa6",
      1,
    );
    line(332, 210, 628, 210, "#3a7684");
    line(332, 390, 628, 390, "#3a7684");
    if (front === "E") exitPortal(480, 300, 1);
    else {
      let glow = ctx.createRadialGradient(480, 300, 10, 480, 300, 190);
      glow.addColorStop(0, "#19424a65");
      glow.addColorStop(1, "#07111b00");
      ctx.fillStyle = glow;
      ctx.fillRect(332, 210, 296, 180);
      ctx.fillStyle = "#6397a5";
      ctx.font = "10px monospace";
      ctx.textAlign = "center";
      ctx.fillText("UNEXPLORED", 480, 309);
    }
  }
  // Perspective floor lights and generous dark vignette make the chamber readable.
  line(0, 600, 280, 420, "#5ee8e7", 2);
  line(960, 600, 680, 420, "#5ee8e7", 2);
  for (let x of [340, 620]) {
    polygon(
      [
        [x - 15, 532],
        [x + 12, 532],
        [x + 5, 537],
        [x - 24, 537],
      ],
      "#d9b5ff",
      null,
    );
  }
  ctx.fillStyle = "#85c6cf";
  ctx.font = "10px monospace";
  ctx.textAlign = "center";
  ctx.fillText(
    "G H O S T   /   L E V E L  " + String(o.level).padStart(2, "0"),
    480,
    565,
  );
  drawMap(o);
}
function exitPortal(x, y, s) {
  ctx.save();
  ctx.translate(x, y);
  ctx.scale(s, s);
  let glow = ctx.createRadialGradient(0, 0, 10, 0, 0, 120);
  glow.addColorStop(0, "#82ffe55f");
  glow.addColorStop(1, "#82ffe500");
  ctx.fillStyle = glow;
  ctx.fillRect(-125, -130, 250, 260);
  ctx.strokeStyle = "#a6ffde";
  ctx.lineWidth = 3;
  ctx.strokeRect(-54, -80, 108, 160);
  ctx.fillStyle = "#8fffe0";
  ctx.font = "bold 16px monospace";
  ctx.textAlign = "center";
  ctx.fillText("E X I T", 0, -100);
  ctx.font = "35px monospace";
  ctx.fillText("↑", 0, 15);
  ctx.restore();
}
function drawMap(o) {
  let n = o.size,
    c = 180 / n,
    x0 = 18,
    y0 = 18;
  map.clearRect(0, 0, 216, 216);
  map.fillStyle = "#0c121b";
  map.fillRect(0, 0, 216, 216);
  for (let r = 0; r < n * n; r++) {
    let x = x0 + (r % n) * c,
      y = y0 + Math.floor(r / n) * c;
    map.fillStyle = "#26313c";
    map.fillRect(x + c / 2 - 1, y + c / 2 - 1, 2, 2);
  }
  for (let [r, v, edges] of o.memory) {
    let x = x0 + (r % n) * c,
      y = y0 + Math.floor(r / n) * c;
    map.fillStyle = r === o.room ? "#22433f" : "#14242b";
    map.fillRect(x + 2, y + 2, c - 4, c - 4);
    for (let d = 0; d < 4; d++) {
      if (edges[d] === ".") continue;
      map.strokeStyle =
        edges[d] === "E" ? "#ff71c5" : edges[d] === "?" ? "#3a4854" : "#72bfb7";
      map.lineWidth = edges[d] === "E" ? 3 : 1;
      map.setLineDash(edges[d] === "?" ? [2, 3] : []);
      map.beginPath();
      if (d === 0) {
        map.moveTo(x, y);
        map.lineTo(x + c, y);
      }
      if (d === 1) {
        map.moveTo(x + c, y);
        map.lineTo(x + c, y + c);
      }
      if (d === 2) {
        map.moveTo(x, y + c);
        map.lineTo(x + c, y + c);
      }
      if (d === 3) {
        map.moveTo(x, y);
        map.lineTo(x, y + c);
      }
      map.stroke();
      map.setLineDash([]);
    }
    if (r !== o.room && v > 1) {
      map.fillStyle = "#c687b4";
      map.font = Math.max(8, c * 0.26) + "px monospace";
      map.textAlign = "center";
      map.fillText(v, x + c / 2, y + c * 0.65);
    }
  }
  let x = x0 + ((o.room % n) + 0.5) * c,
    y = y0 + (Math.floor(o.room / n) + 0.5) * c;
  map.save();
  map.translate(x, y);
  map.rotate((o.direction * Math.PI) / 2);
  map.fillStyle = "#9bffe2";
  map.beginPath();
  map.moveTo(0, -c * 0.24);
  map.lineTo(c * 0.18, c * 0.18);
  map.lineTo(0, c * 0.08);
  map.lineTo(-c * 0.18, c * 0.18);
  map.closePath();
  map.fill();
  map.restore();
}
function update(f) {
  if (!f.ready) return;
  frame = f;
  const o = f.observation,
    k = f.generation + ":" + f.revision;
  if (lastKey !== k) {
    draw(o);
    lastKey = k;
  }
  $("mode").textContent = f.active
    ? f.player === "TEV1"
      ? "TEV1 AT THE KEYS"
      : f.player + " AT THE KEYS"
    : "HUMAN AT THE KEYS";
  $("level").textContent =
    "LEVEL " + String(f.level).padStart(2, "0") + " / 03";
  $("dimensions").textContent = o.size + " × " + o.size + " ROOMS";
  $("facing").textContent = o.facing + " " + ["↑", "→", "↓", "←"][o.direction];
  $("room").textContent = "ROOM " + String(o.room + 1).padStart(2, "0");
  $("message").textContent = f.reason.startsWith("PROVIDER_")
    ? "AI stopped — inference or pipeline failed. Human play is available."
    : f.message;
  $("entries").innerHTML = Array.from(
    { length: 7 },
    (_, i) => '<i class="' + (i < o.entriesLeft ? "" : "empty") + '"></i>',
  ).join("");
  $("entry-caption").textContent =
    o.entriesLeft <= 2
      ? "Careful. This room is nearly exhausted."
      : "Seven chances. Make them count.";
  $("entries").style.filter = o.entriesLeft <= 2 ? "hue-rotate(170deg)" : "";
  for (let id of ["moves", "explored", "repeats"])
    $(id).textContent = String(f[id]).padStart(2, "0");
  $("decision-label").textContent =
    f.player === "TEV1"
      ? "TEV1 · " + (f.model || "tev1:0.8b")
      : f.player === "HUMAN"
        ? "NEXT MOVE IS YOURS"
        : f.player + " · BASELINE";
  $("decision-action").textContent =
    f.latestAction === "START"
      ? "MAKE A LITTLE HISTORY."
      : f.latestAction.replaceAll("_", " ");
  $("latency").textContent =
    f.player === "TEV1"
      ? f.latencyMs.toLocaleString() + " ms · actual inference"
      : "↑ Forward   ← → Turn";
  $("thinking").hidden = !(f.active && f.player === "TEV1");
  $("pause").disabled = !f.active || f.pausing;
  $("pause").textContent = f.pausing ? "PAUSING…" : "Ⅱ PAUSE";
  $("play").disabled = f.active || f.status !== "PLAYING";
  $("player").disabled = f.active;
  document
    .querySelectorAll("[data-action]")
    .forEach((b) => (b.disabled = f.active || busy || f.status !== "PLAYING"));
  $("status").textContent =
    f.status === "PLAYING"
      ? f.active
        ? "EXPLORING"
        : "FIND THE EXIT"
      : f.status;
  $("overlay").hidden = f.status === "PLAYING";
  if (f.status !== "PLAYING") {
    $("overlay-kicker").textContent =
      f.status === "WON" ? "SIGNAL RECEIVED" : "THE MAZE REMEMBERS";
    $("overlay-title").textContent =
      f.status === "WON" ? "GHOST, RELEASED." : "ROOM EXHAUSTED.";
    $("overlay-copy").textContent =
      f.moves +
      " moves · " +
      f.explored +
      " rooms explored · " +
      f.repeats +
      " revisits";
  }
  if (!f.active && f.player !== "HUMAN" && f.reason !== "READY") {
    let id = f.segment || f.generation + ":" + f.player + ":" + f.decisions;
    if (lastCompleted !== id) {
      lastCompleted = id;
      recentResults.push({
        player: f.player,
        reason: f.reason,
        moves: f.moves,
        decisions: f.decisions,
        rooms: f.explored,
      });
      recentResults = recentResults.slice(-3);
      $("run-result").hidden = false;
      $("run-result").innerHTML =
        "<strong>THE GHOST’S FIELD NOTES</strong><br>" +
        recentResults
          .map(
            (r) =>
              esc(r.player) +
              " · " +
              esc(r.reason) +
              " · " +
              r.decisions +
              " decisions · " +
              r.moves +
              " moves · " +
              r.rooms +
              " rooms",
          )
          .join("<br>");
    }
  }
}
async function reset() {
  busy = true;
  try {
    update(
      await api("", {
        seed: Number($("seed").value),
        level: 1,
        singleLevel: false,
      }),
    );
  } catch (e) {
    $("message").textContent = e.message;
  } finally {
    busy = false;
  }
}
async function action(a) {
  if (!frame || frame.active || busy || frame.status !== "PLAYING") return;
  busy = true;
  try {
    update(await api("/actions", { action: a, revision: frame.revision }));
  } catch (e) {
    $("message").textContent = e.message;
  } finally {
    busy = false;
    if (frame) update(frame);
  }
}
document
  .querySelectorAll("[data-action]")
  .forEach((b) => (b.onclick = () => action(b.dataset.action)));
document.addEventListener("keydown", (e) => {
  if (["INPUT", "SELECT", "TEXTAREA"].includes(e.target.tagName)) return;
  let a = {
    ArrowUp: "FORWARD",
    w: "FORWARD",
    W: "FORWARD",
    ArrowLeft: "TURN_LEFT",
    a: "TURN_LEFT",
    A: "TURN_LEFT",
    ArrowRight: "TURN_RIGHT",
    d: "TURN_RIGHT",
    D: "TURN_RIGHT",
  }[e.key];
  if (a) {
    e.preventDefault();
    if (!e.repeat) action(a);
  }
});
$("reset").onclick = reset;
$("overlay-reset").onclick = reset;
$("play").onclick = async () => {
  try {
    update(
      await api("/play", {
        player: $("player").value,
        revision: frame.revision,
      }),
    );
  } catch (e) {
    $("message").textContent = e.message;
  }
};
$("pause").onclick = async () => {
  try {
    update(await api("/pause", {}));
  } catch (e) {
    $("message").textContent = e.message;
  }
};
$("scanlines").onclick = () => {
  const off = document.body.classList.toggle("no-crt");
  $("scanlines").textContent = off ? "CRT OFF" : "CRT ON";
  $("scanlines").setAttribute("aria-pressed", String(!off));
};
(async () => {
  try {
    let f = await api();
    if (f.ready) update(f);
    else await reset();
    const events = new EventSource("/api/game/events");
    events.onmessage = (e) => update(JSON.parse(e.data));
    events.onerror = () => {
      $("signal").textContent = "RECONNECTING";
    };
    events.onopen = () => {
      $("signal").textContent = "SIGNAL STABLE";
    };
  } catch (e) {
    $("message").textContent =
      "Cannot reach the game. Start the Java application and reload.";
  }
})();
