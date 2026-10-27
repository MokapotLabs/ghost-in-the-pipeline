// Exercise the real renderer and animation clock without a browser dependency.
const assert = require("node:assert/strict");
const fs = require("node:fs");
const vm = require("node:vm");
let coordinates = [],
  labels = [],
  rotations = [],
  callbacks = new Map(),
  sequence = 0;
const gradient = { addColorStop() {} };
const context = new Proxy(
  {},
  {
    get: (_, key) => {
      if (key.startsWith("create")) return () => gradient;
      return (...args) => {
        if (["moveTo", "lineTo", "fillRect", "strokeRect"].includes(key)) {
          assert(args.every(Number.isFinite), `${key}: nonfinite coordinate`);
          coordinates.push([key, ...args]);
        }
        if (key === "fillText") labels.push(args[0]);
        if (key === "rotate") rotations.push(args[0]);
      };
    },
    set: () => true,
  },
);
const media = { matches: false };
const sandbox = {
  document: { getElementById: () => ({ getContext: () => context }) },
  matchMedia: () => media,
  performance: { now: () => 0 },
  requestAnimationFrame: (fn) => {
    callbacks.set(++sequence, fn);
    return sequence;
  },
  cancelAnimationFrame: (id) => callbacks.delete(id),
};
vm.createContext(sandbox);
const source = fs.readFileSync(
  "app/src/main/resources/META-INF/resources/game.js",
  "utf8",
);
vm.runInContext(
  source.slice(0, source.indexOf("const ghostAudio =")),
  sandbox,
);
const run = (expression) => vm.runInContext(expression, sandbox);
function step(time) {
  const pending = [...callbacks.values()];
  callbacks.clear();
  pending.forEach((fn) => fn(time));
}
run(`var observation = {level:1,size:3,room:0,direction:0,facing:'N',edges:'##.?',memory:[[0,1,'##.?',7]]};
     var before = {generation:'one',level:1,observation};
     var after = {...before,latestAction:'TURN_RIGHT',observation:{...observation,direction:1,facing:'E'}};
     renderFrame(null,before);`);
const initial = JSON.stringify(coordinates);
coordinates = [];
run("renderFrame(before,after)");
assert.equal(callbacks.size, 1);
step(140);
assert(
  Math.abs(run("cameraYaw") - Math.PI / 4) < 1e-9,
  "camera passes through a real intermediate heading",
);
assert.notEqual(
  JSON.stringify(coordinates),
  initial,
  "the room geometry moves",
);
assert(
  labels.some((x) => x.includes("N  →  E")),
  "direction cue is visible",
);
assert(
  Math.abs(rotations.at(-1) - Math.PI / 4) < 1e-9,
  "map arrow tracks camera",
);
assert(!labels.includes("E X I T"), "no exit is invented from unseen topology");
step(280);
assert.equal(callbacks.size, 0);
assert.equal(run("cameraYaw"), Math.PI / 2);
run("renderFrame(after,before)");
step(140);
run(`renderFrame(after,{...before,generation:'reset'})`);
assert.equal(callbacks.size, 0, "reset cancels an in-flight turn");
assert.equal(run("cameraYaw"), 0);
media.matches = true;
run("renderFrame(before,after)");
assert.equal(callbacks.size, 0, "reduced motion snaps directly");
assert.equal(run("cameraYaw"), Math.PI / 2);
media.matches = false;
run(`var west = {...before,observation:{...observation,direction:3,facing:'W'}};
     renderFrame(null,west); renderFrame(west,{...before,latestAction:'TURN_RIGHT'});`);
step(140);
assert(
  Math.abs(run("cameraYaw") - (7 * Math.PI) / 4) < 1e-9,
  "west to north turns through the short clockwise arc",
);
step(280);
// Every heading clips safely, including looking exactly into corners.
run(`var walkStart = {...before, status:'PLAYING',observation:{...observation,direction:2,facing:'S'}};
     var arrived = {...walkStart,latestAction:'FORWARD',observation:{...walkStart.observation,room:3,edges:'...#',memory:[[0,1,'##.#',7],[3,1,'...#',6]]}};
     renderFrame(null,walkStart); renderFrame(walkStart,arrived);`);
assert.equal(callbacks.size, 1, "successful forward movement animates");
coordinates = [];
step(110);
assert(
  labels.some((x) => x.includes("ENTERING ROOM 04")),
  "step cue uses the committed room",
);
assert.equal(run("cameraYaw"), Math.PI, "a step preserves orientation");
const middleStep = JSON.stringify(coordinates);
coordinates = [];
step(220);
assert.notEqual(
  JSON.stringify(coordinates),
  middleStep,
  "approach geometry settles at room center",
);
assert.equal(callbacks.size, 0);
run(`renderFrame(walkStart,{...walkStart,latestAction:'FORWARD'})`);
assert.equal(
  callbacks.size,
  0,
  "a blocked forward action never animates entry",
);
media.matches = true;
run("renderFrame(walkStart,arrived)");
assert.equal(callbacks.size, 0, "reduced motion also disables step animation");
media.matches = false;
for (let degrees = 0; degrees < 360; degrees++)
  run(`draw(observation,${(degrees * Math.PI) / 180})`);
const observedBefore = run('JSON.stringify(observation)');
labels = [];
run(`frame={navigation:{signal:'INTENSE',signalTrend:'NO_MOVEMENT'}}; visualCluesEnabled=true; draw(observation);`);
assert(labels.includes('SIGNAL · INTENSE'), 'visual HUD uses the canonical signal');
labels = [];
run('visualCluesEnabled=false; draw(observation)');
assert(!labels.includes('SIGNAL · INTENSE'), 'visual clues can be independently disabled');
assert.equal(run('JSON.stringify(observation)'), observedBefore, 'presentation does not mutate observations');
console.log(
  "PASS: turns, steps, compass, wraparound, reset, reduced motion, collisions, and 360 clipped headings",
);
