---
title: "Ghost in the Pipeline: an MSX maze, a Java framework, and an AI that keeps hitting the wall"
published: false
tags: devchallenge, hf26challenge, java, ai
---

*This is a submission for the [Hacktoberfest Open-Source AI Challenge Week 4: Ghost in the Machine](https://dev.to/challenges/hacktoberfest-week4-2026-10-26).*

## What I Built

I wanted to revisit the feeling of an old home computer: a few bright lines, a room that barely exists, and the conviction that the exit must be around the next corner.

Ghost in the Pipeline is a little first-person maze inspired by the MSX BASIC game [3D Maze Pro](https://github.com/antor44/3dmaze). You can play with the keyboard, then hand the controls to Tev1, a local 0.8B decision model. A simple Explorer and random movement are there to keep the experiment honest.

There is one slightly unforgiving rule: every room has seven entries. Wear out a room's allowance and the run ends. Find the exit and the next maze grows.

The historical code and assets have unresolved redistribution rights, so this is an independent implementation with original Java rules, generation and visuals. It is not an emulator or a BASIC translation.

## Demo

<!-- Before publishing: insert the manually recorded 60–90-second video. Follow docs/recording.md. -->

![Ghost in the Pipeline's original first-person maze](screenshots/game.png)

The game runs locally. Start it from the repository, use the arrow keys, and try the same seed with each player. The AI's label and latest decision time remain visible throughout.

## Code

[Ghost in the Pipeline on GitHub](https://github.com/MokapotLabs/ghost-in-the-pipeline)

<!-- Publish the reviewed application commits before publishing this post; the repository link alone does not mean those commits are already public. -->

## How I Built It

The Pipeline Framework, the open-source Java framework I've spent years developing, owns the execution. A typed recursive pipeline carries immutable game state through bounded turns. Its existing decision connector asks Tev1 through Ollama for one finite-choice action. Application logic validates the proposal and applies the same rules a human faces.

The model sees structured exploration memory: visited rooms, observed openings and exits, orientation, and entry allowances. It never receives a hidden map or a route from the solver. The solver only checks that a generated level has a valid escape before admitting it.

I used Codex with the TPF authoring and demo-implementation skills, preserved meaningful development commits, and checked both deterministic rules and real provider behavior. Native TPF replay records make it possible to inspect how the agent's turns stayed inside one execution. I did not add a new framework runtime or an artificial MCP layer.

And the model's performance? In nine fixed seed/level cases, Tev1 escaped **zero**. Random legal movement escaped **one**. A deterministic explorer using observed-room memory escaped **all nine**.

Tev1 0.8B repeatedly selected forward even when the observation described a wall. The rules rejected those moves. In a fresh paired follow-up, the installed 4B variant escaped **one of nine** cases and made 102 successful moves, versus four for 0.8B. It still collided 257 times and exhausted a room in six attempts. Neither model received a route, and no maze was changed.

Separate bounded probes rotated choice order and added redundant front/left/right descriptions of the same visible edges. 0.8B kept choosing forward. 4B consistently turned toward the passage with the relative descriptions. That points to observation-format sensitivity in the larger model; it does not fully explain the smaller model or establish useful full-maze exploration. The [paired report](model-comparison.md) retains the raw requests, outcomes and limitations. A valid action label is not evidence of useful navigation.

I then put those clearer labels into actual gameplay and reran both models on the same nine cases, preserving the original results. 0.8B remained at **zero escapes**. 4B improved from **one to three**, and wall rejections fell from **257 to 105**. It still timed out five times and exhausted a room once. The [new action records](relative-observations.md) show the tradeoff: better local movement and more exploration, with unreliable navigation. Both models are now selectable in the game menu, and a small forward-step animation helps the human keep their bearings.

## Why Does Open Innovation Matter?

After the initial dependencies and weights are downloaded, this game and its inference run on the laptop. There is no hosted model bill or cloud account required for the default setup. I can inspect the framework, replace the model binding, read every proposed action, and rerun the comparison.

More importantly, I can keep the failure. There is no hidden successful route behind the AI button. The open pieces make the architecture and the result inspectable, even when the result is a ghost staring determinedly at a wall.

## My Agent Session

Meaningful commits have local Entire checkpoints, including the implementation decisions and repairs. The public repository will include the concise architecture, evaluation and execution evidence.

<!-- Before publishing: review and redact exported development sessions, then insert a shareable session link. Raw checkpoints contain local paths and account context and are not approved for publication. No session has been uploaded automatically. -->

## The Spirit of It

The Spectrum, MSX and Commodore 64 are part of my computing history. Revisiting that era with modern Java was the point; handing the keys to a tiny model made it an experiment.

The memory I wanted to preserve wasn't a particular ROM. It was the feeling that a few lines on a screen could become somewhere you were lost.

This time, the human gets to watch someone else get lost too.

<!-- Optional: enter Best Use of Entire only after a reviewed session is actually shared and linked. No partner-category claim is made by this draft. -->
