# The Guest | الزائر

Offline psychological horror game for Android.

## Architecture Contract (Milestone 0)

- **UI:** Kotlin + XML Views. `GameActivity` hosts `GameCanvasView` and touch controls.
- **Rendering:** `GameCanvasView` renders the current room, player, collision/debug-independent scene primitives, and lighting overlay. It does not own gameplay rules.
- **Loop:** `GameLoop` is `Choreographer`-driven. It clamps delta time and delegates simulation updates before invalidating the canvas.
- **State flow:** immutable-ish model objects represent game state; engine systems mutate state through explicit update methods. Rendering consumes the latest state only.
- **Movement:** joystick input becomes a normalized direction; movement is delta-time based and resolved through `CollisionSystem` against normalized room geometry.
- **Coordinates:** all world geometry uses normalized coordinates (`0f..1f`) and is converted to pixels only at render/input boundaries.
- **Persistence:** later milestones will use a `GameRepository` backed by JSON in `SharedPreferences`, with an explicit `saveVersion`.
- **Events:** later milestones will use `HorrorDirector` to select eligible weighted events using room/progress/tension/cooldown history rather than raw randomness.
- **Audio:** later milestones will separate long ambience playback from short effects and spatial panning.
- **Lifecycle:** game loop and future audio/timers must stop on pause and resume exactly once.

## Milestone status

- Milestone 0: architecture audit complete. The repository was empty, so the project starts from a clean baseline.
- Milestone 1: core engine implemented; CI is the acceptance gate before Milestone 2.

## Milestone policy

Each milestone must compile and test successfully before the next milestone is started. No completed milestone may contain fake implementations or unfinished controls.
