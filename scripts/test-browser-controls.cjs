// Disposable UI-only fixture server. No model, maze solver or real game session is used.
const { chromium, webkit } = require("playwright");
const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const assert = require("node:assert/strict");
const root = path.resolve(__dirname, "..");
const assets = path.join(root, "app/src/main/resources/META-INF/resources");
const original = JSON.parse(
  fs.readFileSync(path.join(root, "evidence/haunted-08b-off.json")),
).trials[0];
const sockets = new Set();
let frame,
  actions,
  resets,
  generation = 0;
function fresh() {
  actions = [];
  resets = 0;
  frame = {
    ready: true,
    generation: "ui-" + ++generation,
    segment: "fixture",
    revision: 1,
    seed: 1983,
    observation: structuredClone(original.initialObservation),
    navigation: structuredClone(original.initialNavigation),
    agentClues: false,
    level: 1,
    status: "PLAYING",
    message: "UI regression fixture",
    active: false,
    pausing: false,
    reason: "HUMAN_TURN",
    player: "HUMAN",
    moves: 0,
    actions: 0,
    collisions: 0,
    repeats: 0,
    explored: 1,
    exitsFound: 0,
    decisions: 0,
    latestAction: "START",
    latencyMs: 0,
    model: "tev1:0.8b",
    models: { TEV1: "tev1:0.8b", TEV1_4B: "tev1:latest" },
  };
}
const server = http.createServer(async (req, res) => {
  if (req.url === "/api/game/events") {
    res.writeHead(200, {
      "Content-Type": "text/event-stream",
      "Cache-Control": "no-cache",
    });
    const timer = setInterval(
      () => res.write("data: " + JSON.stringify(frame) + "\n\n"),
      150,
    );
    req.on("close", () => clearInterval(timer));
    return;
  }
  if (req.url.startsWith("/api/game")) {
    let data = "";
    for await (const chunk of req) data += chunk;
    const body = data ? JSON.parse(data) : {};
    if (req.method === "POST" && req.url === "/api/game/actions") {
      actions.push(body.action);
      frame.revision++;
      frame.latestAction = body.action;
      if (body.action === "TURN_RIGHT")
        frame.observation.direction = (frame.observation.direction + 1) % 4;
      if (body.action === "TURN_LEFT")
        frame.observation.direction = (frame.observation.direction + 3) % 4;
      frame.observation.facing = ["N", "E", "S", "W"][
        frame.observation.direction
      ];
    } else if (req.method === "POST" && req.url === "/api/game/play") {
      frame.active = true;
      frame.player = body.player;
      frame.model = frame.models[body.player] || body.player;
    } else if (req.method === "POST" && req.url === "/api/game/pause") {
      frame.pausing = true;
      setTimeout(() => {
        frame.active = false;
        frame.pausing = false;
        frame.reason = "PAUSED";
      }, 220);
    } else if (req.method === "POST" && req.url === "/api/game") {
      resets++;
      frame.active = false;
      frame.status = "PLAYING";
      frame.reason = "HUMAN_TURN";
      frame.player = "HUMAN";
      frame.latestAction = "START";
      frame.revision++;
    }
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify(frame));
    return;
  }
  const pathname = req.url.split("?")[0];
  const file = pathname === "/" ? "index.html" : pathname.slice(1);
  if (
    !["index.html", "game.js", "sound.js", "style.css", "portal.png"].includes(
      file,
    )
  ) {
    res.writeHead(404);
    res.end();
    return;
  }
  res.writeHead(200, {
    "Content-Type": file.endsWith(".js")
      ? "application/javascript"
      : file.endsWith(".css")
        ? "text/css"
        : file.endsWith(".png")
          ? "image/png"
          : "text/html",
  });
  res.end(fs.readFileSync(path.join(assets, file)));
});
server.on("connection", (s) => {
  sockets.add(s);
  s.on("close", () => sockets.delete(s));
});
async function run() {
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  const results = [];
  for (const [name, engine] of [
    ["WebKit", webkit],
    ["Chromium", chromium],
  ]) {
    fresh();
    const browser = await engine.launch({ headless: true });
    try {
      const page = await browser.newPage({
          viewport: { width: 1280, height: 720 },
        }),
        errors = [];
      page.on("pageerror", (e) => errors.push(e.message));
      await page.goto("http://127.0.0.1:" + server.address().port);
      await page
        .getByRole("option", { name: "tev1:latest · 4B", exact: true })
        .waitFor({ state: "attached" });
      assert.equal(await page.locator(".intro").count(), 0, "hero removed");
      assert.equal(
        await page.locator("header .brand img").getAttribute("src"),
        "portal.png",
      );
      assert.equal(
        await page.locator('link[rel="icon"]').getAttribute("href"),
        "portal.png",
      );
      await page.waitForFunction(
        () => document.querySelector(".brand img").naturalWidth > 0,
      );
      const layout = await page.evaluate(() => ({
        top: document.querySelector(".machine").getBoundingClientRect().top,
        height: document.querySelector("#maze").getBoundingClientRect().height,
        bottom: document.querySelector(".control-panel").getBoundingClientRect()
          .bottom,
        width: document.documentElement.scrollWidth,
        pageHeight: document.documentElement.scrollHeight,
      }));
      assert(
        layout.top < 80 &&
          layout.height >= 400 &&
          layout.bottom <= 720 &&
          layout.width <= 1280 &&
          layout.pageHeight <= 720,
        "recording layout must fit 1280×720",
      );
      const favicon = await page.request.get(
        "http://127.0.0.1:" + server.address().port + "/portal.png",
      );
      assert.equal(favicon.status(), 200);
      const iconChecks = await page.evaluate(() =>
        [16, 42].map((size) => {
          const c = document.createElement("canvas");
          c.width = c.height = size;
          const ctx = c.getContext("2d");
          ctx.drawImage(document.querySelector(".brand img"), 0, 0, size, size);
          const pixels = ctx.getImageData(0, 0, size, size).data;
          let lit = 0;
          for (let i = 0; i < pixels.length; i += 4)
            if (pixels[i + 1] > 100 && pixels[i + 1] > pixels[i] * 1.3) lit++;
          return { size, lit };
        }),
      );
      assert(
        iconChecks.every((c) => c.lit > 4),
        "portal remains visible at favicon and navbar sizes",
      );
      await page.locator("#help-open").click();
      await page.locator("#help[open]").waitFor();
      await page.keyboard.press("w");
      assert.equal(actions.length, 0, "help must not move the player");
      await page.keyboard.press("Escape");
      await page.locator("#help:not([open])").waitFor({ state: "attached" });
      await page.waitForFunction(() => document.activeElement.id === "maze");
      assert.equal(
        await page.evaluate(() => document.activeElement.id),
        "maze",
      );
      await page.locator("#help-open").click();
      await page.locator("#help-close").click();
      await page.locator("#help:not([open])").waitFor({ state: "attached" });
      await page.waitForFunction(() => document.activeElement.id === "maze");
      assert.equal(
        await page.locator("#audio-toggle").getAttribute("aria-pressed"),
        "false",
      );
      await page.locator("#audio-toggle").click();
      assert.equal(await page.locator("#audio-toggle").innerText(), "SOUND ON");
      await page.locator("#audio-toggle").click();
      assert.equal(
        await page.locator("#audio-toggle").innerText(),
        "SOUND OFF",
      );
      await page.locator("#visual-clues").uncheck();
      assert.equal(
        await page.locator(".viewport").getAttribute("data-haunting"),
        "",
      );
      await page.locator("#visual-clues").check();
      await page.evaluate(() => {
        window.optionMutations = 0;
        new MutationObserver((events) => {
          window.optionMutations += events.length;
        }).observe(document.getElementById("player"), {
          subtree: true,
          childList: true,
          characterData: true,
          attributes: true,
        });
      });
      await page.waitForTimeout(1100);
      assert.equal(
        await page.evaluate(() => window.optionMutations),
        0,
        "idle frame stream must not rebuild the native picker",
      );
      for (const player of ["TEV1_4B", "EXPLORER", "RANDOM", "TEV1"]) {
        await page.locator("#player").selectOption(player);
        assert.equal(await page.locator("#player").inputValue(), player);
        assert.equal(
          await page.evaluate(() => document.activeElement.id),
          "maze",
          "selection returns movement focus",
        );
        const before = actions.length;
        const posted = page.waitForResponse(
          (r) =>
            r.url().endsWith("/api/game/actions") &&
            r.request().method() === "POST",
        );
        await page.keyboard.press("ArrowRight");
        await posted;
        await page.waitForFunction(
          (count) =>
            document.querySelector("#decision-action").textContent ===
            "TURN RIGHT",
          before,
        );
        assert.equal(actions.length, before + 1);
        assert.equal(actions.at(-1), "TURN_RIGHT");
      }
      await page.locator("#visual-clues").focus();
      const before = actions.length;
      await page.keyboard.press("w");
      await page.waitForTimeout(100);
      assert.equal(
        actions.length,
        before + 1,
        "clue-checkbox focus must not steal movement keys",
      );
      await page.locator("#seed").fill("2026");
      const count = actions.length;
      await page.locator("#seed").press("ArrowUp");
      await page.waitForTimeout(80);
      assert.equal(
        actions.length,
        count,
        "seed editing must not move the player",
      );
      await page.locator("#player").selectOption("EXPLORER");
      await page.locator("#play").click();
      await page.locator("#player:disabled").waitFor();
      assert(await page.locator("#pause").isEnabled());
      await page.locator("#pause").click();
      await page.waitForFunction(
        () => document.querySelector("#status").textContent === "PAUSING",
      );
      await page.locator("#player:enabled").waitFor();
      await page.locator("#player").selectOption("TEV1_4B");
      await page.locator("#play").click();
      await page.locator("#player:disabled").waitFor();
      frame.decisions = 3;
      frame.latencyMs = 1850;
      frame.latestAction = "FORWARD";
      await page.waitForTimeout(250);
      assert.equal(await page.locator("#mode").innerText(), "tev1:latest");
      assert.equal(await page.locator("#status").innerText(), "RUNNING");
      assert.match(
        await page.locator("#latency").innerText(),
        /1[,.\s]?850 ms/,
      );
      await page.locator("#pause").click();
      await page.locator("#player:enabled").waitFor();
      frame.reason = "PROVIDER_OR_PIPELINE_FAILURE: Test";
      await page.waitForTimeout(250);
      assert.equal(await page.locator("#status").innerText(), "AI FAILED");
      frame.reason = "DECISION_LIMIT";
      await page.waitForTimeout(250);
      assert.equal(await page.locator("#status").innerText(), "COMPLETED");
      const boxes = await page
        .locator(".clue-controls input")
        .evaluateAll((nodes) =>
          nodes.map((n) => ({
            width: n.getBoundingClientRect().width,
            height: n.getBoundingClientRect().height,
            appearance: getComputedStyle(n).appearance,
          })),
        );
      assert(
        boxes.every(
          (b) => b.width === 16 && b.height === 16 && b.appearance === "none",
        ),
        "Safari checkbox appearance is explicitly sized",
      );
      frame.status = "WON";
      await page.waitForTimeout(250);
      await page.locator("#seed").focus();
      await page.locator("#seed").press("Enter");
      await page.waitForTimeout(80);
      assert.equal(
        resets,
        0,
        "Enter while editing a control must not reset the game",
      );
      await page.locator("#help-open").focus();
      await page.keyboard.press("Enter");
      await page.locator("#help[open]").waitFor();
      assert.equal(resets, 0, "Help Enter must not reset a terminal game");
      await page.keyboard.press("Escape");
      await page.locator("#help:not([open])").waitFor({ state: "attached" });
      await page.locator("#reset").focus();
      await page.keyboard.press("Enter");
      await page.locator("#overlay[hidden]").waitFor({ state: "attached" });
      assert.equal(resets, 1);
      assert.equal(
        await page.locator("#audio-toggle").getAttribute("aria-pressed"),
        "false",
      );
      assert.deepEqual(errors, []);
      results.push({
        engine: name,
        version: browser.version(),
        result: "PASS",
        checks: [
          "no picker mutations during frame updates",
          "all four choices commit",
          "movement focus restored",
          "checkbox keys work",
          "seed editing isolated",
          "pause returns control",
          "16px checkbox appearance",
          "editable Enter isolated",
          "1280×720 game layout",
          "PNG branding/favicon decode",
          "Help dismissal and movement isolation",
          "optional glow/audio switches",
          "actual model/action/latency and run statuses",
          "new game reset",
        ],
      });
      console.log(name, "PASS");
    } finally {
      await browser.close();
    }
  }
  if (process.env.BROWSER_TEST_OUT)
    fs.writeFileSync(
      process.env.BROWSER_TEST_OUT,
      JSON.stringify(
        {
          result: "PASS",
          scope: "Disposable UI-only fixture; not inference evidence",
          results,
        },
        null,
        2,
      ) + "\n",
    );
}
run()
  .catch((e) => {
    console.error(e);
    process.exitCode = 1;
  })
  .finally(() => {
    for (const socket of sockets) socket.destroy();
    server.close();
  });
