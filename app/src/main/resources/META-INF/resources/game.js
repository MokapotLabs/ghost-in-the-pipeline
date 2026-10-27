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
// Render only this room. A continuous camera yaw connects the same 90° rule turns.
// Near-plane clipping keeps walls stable while the camera passes a corner.
function draw(o, yaw = (o.direction * Math.PI) / 2, turnLabel = "") {
  const W = 960,
    H = 600,
    focal = 200,
    halfHeight = 0.6;
  ctx.fillStyle = "#050e1b";
  ctx.fillRect(0, 0, W, H);
  const cos = Math.cos(yaw),
    sin = Math.sin(yaw);
  const camera = ([x, y, z]) => [x * cos - z * sin, y, x * sin + z * cos];
  function clip(points) {
    const result = [];
    for (let i = 0; i < points.length; i++) {
      const a = points[i],
        b = points[(i + 1) % points.length];
      if (a[2] >= 0.05) result.push(a);
      if (a[2] >= 0.05 !== b[2] >= 0.05) {
        const t = (0.05 - a[2]) / (b[2] - a[2]);
        result.push(a.map((v, j) => v + (b[j] - v) * t));
      }
    }
    return result;
  }
  const project = ([x, y, z]) => [480 + (focal * x) / z, 300 - (focal * y) / z];
  function face(points, fill, stroke = "#69ede5", width = 2) {
    const visible = clip(points.map(camera));
    if (visible.length >= 3) polygon(visible.map(project), fill, stroke, width);
  }
  function wire(a, b, color, width = 1) {
    a = camera(a);
    b = camera(b);
    if (a[2] < 0.05 && b[2] < 0.05) return;
    if (a[2] < 0.05) {
      const t = (0.05 - a[2]) / (b[2] - a[2]);
      a = a.map((v, j) => v + (b[j] - v) * t);
    } else if (b[2] < 0.05) {
      const t = (0.05 - b[2]) / (a[2] - b[2]);
      b = b.map((v, j) => v + (a[j] - v) * t);
    }
    line(...project(a), ...project(b), color, width);
  }
  face(
    [
      [-1, halfHeight, -1],
      [1, halfHeight, -1],
      [1, halfHeight, 1],
      [-1, halfHeight, 1],
    ],
    "#08111d",
    "#30505c",
  );
  face(
    [
      [-1, -halfHeight, -1],
      [1, -halfHeight, -1],
      [1, -halfHeight, 1],
      [-1, -halfHeight, 1],
    ],
    "#11182a",
    "#38606e",
  );
  for (let v = -1; v <= 1.01; v += 0.25) {
    wire([v, -halfHeight, -1], [v, -halfHeight, 1], "#7879bf55");
    wire([-1, -halfHeight, v], [1, -halfHeight, v], "#7879bf55");
  }
  // N/E/S/W world planes, with a doorway cut from each observed opening.
  const planes = [
    (u, y) => [u, y, -1],
    (u, y) => [1, y, u],
    (u, y) => [-u, y, 1],
    (u, y) => [-1, y, -u],
  ];
  // World north points into the screen at yaw zero.
  const world = (p) => [p[0], p[1], -p[2]];
  const ordered = planes
    .map((plane, d) => ({ plane, d, depth: camera(world(plane(0, 0)))[2] }))
    .sort((a, b) => b.depth - a.depth);
  for (const { plane, d } of ordered) {
    const p = (u, y) => world(plane(u, y));
    const edge = o.edges[d];
    const rect = (a, b, bottom, top, color, stroke = "#69ede5", width = 2) =>
      face(
        [p(a, bottom), p(b, bottom), p(b, top), p(a, top)],
        color,
        stroke,
        width,
      );
    const grad = ctx.createLinearGradient(0, 0, 960, 600);
    grad.addColorStop(0, "#1d4048");
    grad.addColorStop(1, "#102832");
    if (edge === "#" || edge === "?") {
      rect(
        -1,
        1,
        -halfHeight,
        halfHeight,
        grad,
        edge === "?" ? "#346d7b" : "#9cffe7",
      );
      for (const u of [-0.5, 0, 0.5])
        wire(p(u, -halfHeight), p(u, halfHeight), "#568f9870");
      for (const y of [-0.3, 0, 0.3]) wire(p(-1, y), p(1, y), "#568f9870");
    } else {
      rect(-1, -0.74, -halfHeight, halfHeight, grad);
      rect(0.74, 1, -halfHeight, halfHeight, grad);
      rect(-0.74, 0.74, 0.45, halfHeight, "#16323e");
      rect(-0.74, 0.74, -halfHeight, 0.45, "#040e1b", "#4d9aa6", 1);
      if (edge === "E") {
        rect(-0.28, 0.28, -0.4, 0.4, "#82ffe51c", "#a6ffde", 3);
        wire(p(0, -0.1), p(0, 0.15), "#8fffe0", 3);
        wire(p(0, 0.15), p(-0.09, 0.04), "#8fffe0", 3);
        wire(p(0, 0.15), p(0.09, 0.04), "#8fffe0", 3);
      }
      const center = camera(p(0, edge === "E" ? 0.52 : 0));
      if (center[2] > 0.25 && Math.abs(center[0] / center[2]) < 1.8) {
        const [x, y] = project(center);
        ctx.fillStyle = edge === "E" ? "#a6ffde" : "#6397a5";
        ctx.font = edge === "E" ? "bold 16px monospace" : "10px monospace";
        ctx.textAlign = "center";
        ctx.fillText(edge === "E" ? "E X I T" : "PASSAGE", x, y);
      }
    }
  }
  const vignette = ctx.createRadialGradient(480, 300, 170, 480, 300, 570);
  vignette.addColorStop(0, "#00000000");
  vignette.addColorStop(1, "#00000077");
  ctx.fillStyle = vignette;
  ctx.fillRect(0, 0, W, H);
  if (turnLabel) {
    ctx.fillStyle = "#05101ce8";
    ctx.fillRect(200, 534, 560, 48);
    ctx.strokeStyle = "#5ee8e788";
    ctx.strokeRect(200, 534, 560, 48);
  }
  ctx.fillStyle = "#85c6cf";
  ctx.font = turnLabel ? "bold 18px monospace" : "10px monospace";
  ctx.textAlign = "center";
  ctx.fillText(
    turnLabel ||
      "G H O S T   /   L E V E L  " + String(o.level).padStart(2, "0"),
    480,
    565,
  );
  drawMap(o, yaw);
}
let turnFrame = 0,
  cameraYaw = null;
const reducedMotion = matchMedia("(prefers-reduced-motion: reduce)");
function renderFrame(previous, current) {
  cancelAnimationFrame(turnFrame);
  const o = current.observation,
    target = (o.direction * Math.PI) / 2;
  const turning =
    previous &&
    previous.generation === current.generation &&
    previous.level === current.level &&
    previous.observation.room === o.room &&
    previous.observation.direction !== o.direction;
  if (!turning || reducedMotion.matches || cameraYaw === null) {
    cameraYaw = target;
    draw(o);
    return;
  }
  const startYaw = cameraYaw;
  let delta = Math.atan2(
    Math.sin(target - startYaw),
    Math.cos(target - startYaw),
  );
  // Keep the intended turn direction even if an earlier animation was interrupted.
  if (current.latestAction === "TURN_RIGHT" && delta < 0) delta += 2 * Math.PI;
  if (current.latestAction === "TURN_LEFT" && delta > 0) delta -= 2 * Math.PI;
  const from = previous.observation.facing,
    label =
      from +
      "  →  " +
      o.facing +
      (current.latestAction === "TURN_LEFT"
        ? "   /   TURNING LEFT"
        : "   /   TURNING RIGHT");
  const started = performance.now();
  function tick(now) {
    const t = reducedMotion.matches ? 1 : Math.min(1, (now - started) / 280);
    const eased = t * t * (3 - 2 * t);
    cameraYaw = startYaw + delta * eased;
    draw(o, cameraYaw, t < 1 ? label : "");
    if (t < 1) turnFrame = requestAnimationFrame(tick);
    else cameraYaw = target;
  }
  turnFrame = requestAnimationFrame(tick);
}
function drawMap(o, yaw = (o.direction * Math.PI) / 2) {
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
  map.rotate(yaw);
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
  const previous = frame;
  const previousCollisions = frame ? frame.collisions : 0;
  frame = f;
  $("player").options[0].textContent = "AI · " + f.model;
  const o = f.observation,
    k = f.generation + ":" + f.revision;
  if (lastKey !== k) {
    renderFrame(previous, f);
    if (
      f.collisions > previousCollisions &&
      !matchMedia("(prefers-reduced-motion: reduce)").matches
    ) {
      canvas.animate(
        [
          { filter: "brightness(1.4) hue-rotate(35deg)" },
          { filter: "brightness(1)" },
        ],
        { duration: 140 },
      );
    }
    lastKey = k;
  }
  $("mode").textContent = f.active
    ? f.player === "TEV1"
      ? f.model.toUpperCase() + " AT THE KEYS"
      : f.player + " AT THE KEYS"
    : "HUMAN AT THE KEYS";
  $("level").textContent =
    "LEVEL " + String(f.level).padStart(2, "0") + " / 03";
  $("dimensions").textContent = o.size + " × " + o.size + " ROOMS";
  $("facing").textContent = o.facing + " " + ["↑", "→", "↓", "←"][o.direction];
  $("room").textContent = "ROOM " + String(o.room + 1).padStart(2, "0");
  $("message").textContent = f.reason.startsWith("PROVIDER_")
    ? "The AI couldn’t finish its turn. You can take the keys."
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
      ? "AI · " + (f.model || "tev1:0.8b")
      : f.player === "HUMAN"
        ? "NEXT MOVE IS YOURS"
        : f.player + " · BASELINE";
  $("decision-action").textContent =
    f.latestAction === "START"
      ? "MAKE A LITTLE HISTORY."
      : f.latestAction.replaceAll("_", " ");
  $("latency").textContent =
    f.player === "TEV1"
      ? f.latencyMs.toLocaleString() + " ms · decision " + f.decisions
      : "↑ Forward   ← → Turn";
  if (f.player === "TEV1" && f.decisions === 0) {
    $("decision-action").textContent = f.active
      ? "CHOOSING FIRST ACTION…"
      : "NO DECISION";
    $("latency").textContent = "—";
  }
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
      " total moves · " +
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
        reason: f.reason.startsWith("PROVIDER_")
          ? "AI TURN FAILED"
          : {
              DECISION_LIMIT: "OUT OF TURNS",
              TIME_LIMIT: "TIME’S UP",
              WON: "ESCAPED",
              LOST: "ROOM EXHAUSTED",
            }[f.reason] || f.reason,
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
              " total moves · " +
              r.rooms +
              " total rooms",
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
  if (e.ctrlKey || e.metaKey || e.altKey) return;
  if (e.key === "Enter" && frame && frame.status !== "PLAYING") {
    e.preventDefault();
    reset();
    return;
  }
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
