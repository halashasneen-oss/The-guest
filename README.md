# The Guest | الزائر

**The Guest** is a native Android psychological-horror game built with Kotlin, XML Views and Canvas. The complete gameplay loop works offline and does not request sensitive permissions.

## Version 1.0 scope

- Five replayable rooms: entrance, living room, kitchen, bedroom and basement.
- Normalized 0..1 world coordinates, delta-time movement and collision resolution.
- Virtual joystick, interaction controls and landscape immersive mode.
- Dynamic Canvas lighting and a hidden four-stage tension system.
- `HorrorDirector` pacing with eligibility rules, weighted selection, cooldowns and one-shot events.
- Native audio: MediaPlayer ambience plus SoundPool spatial effects for knocks, footsteps, drops and whispers.
- Persistent physical room state and temporary perceived state.
- Five story memories and an unreliable-memory narrative thread.
- Behaviour tracking and two endings: Truth and Denial.
- Main menu, Continue, New Game, Settings, Credits and Pause.
- Autosave/restore through versioned JSON in SharedPreferences.
- Master/effects/ambient volume, vibration and subtitle controls.

## Architecture

The project deliberately avoids a general-purpose game engine. Gameplay remains split across focused systems:

- `GameLoop` — Choreographer-driven update/render scheduling with clamped delta time.
- `GameCanvasView` — gameplay orchestration and input-facing state.
- `RoomRenderer` — cached Paint/Path/RectF Canvas rendering without room rules.
- `CollisionSystem` — normalized collision and sliding.
- `InteractionSystem` — nearest eligible hotspot selection.
- `LightingSystem` — darkness/player-light overlay.
- `AudioManager` / `AudioMath` — native playback and stereo spatialization.
- `TensionSystem` — hidden 0–100 tension and stage thresholds.
- `HorrorDirector` — paced horror-event selection.
- `RoomStateManager` — persistent physical/perceived room flags.
- `EndingResolver` — ending determination from accumulated player behaviour.
- `GameRepository` / `GameSaveCodec` — versioned offline save/restore.

## Build requirements

- Java 17
- Android SDK 35
- Gradle 8.10.2
- `minSdk 26`
- `targetSdk 35`

The repository intentionally does not depend on a committed Gradle wrapper JAR. CI provisions the pinned Gradle version with `gradle/actions/setup-gradle`.

### Local verification

```bash
gradle testDebugUnitTest assembleDebug bundleRelease
```

The CI workflow runs tests, builds the debug APK and release AAB, enforces a 40 MB artifact budget, and uploads both build artifacts.

## Signed release

Pushing a `v*` tag runs `.github/workflows/release.yml`. The workflow expects these GitHub Actions secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

The keystore is decoded only into the runner's temporary directory. Signing credentials are never committed to the repository.

## Privacy and connectivity

The core game is fully offline. The manifest requests no Camera, Microphone, Contacts, Storage, Location or Internet permission.

## Milestones

Milestones 0–13 are implemented: architecture audit, core engine, entrance, audio, tension, horror director, living room, kitchen, bedroom, basement, endings, menus/settings, polish and release automation.
