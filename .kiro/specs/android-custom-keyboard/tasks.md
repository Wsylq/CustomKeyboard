# Implementation Plan: Android Custom Keyboard

## Overview

Implement a custom Android IME (Input Method Editor) styled after Gboard, using Kotlin and the `InputMethodService` framework. The implementation proceeds in layers: project scaffolding and data models first, then the pure logic layer (fully testable on JVM), then the Android view layer, and finally the CI/CD pipeline. Each phase wires into the previous one so there is no orphaned code.

## Tasks

- [x] 1. Set up Android project structure and build configuration
  - Create a new Android application module with `minSdk 26`, `targetSdk 34`, and Kotlin support
  - Add `InputMethodService` declaration to `AndroidManifest.xml` with the required `<service>` entry, `BIND_INPUT_METHOD` permission, and `<meta-data>` pointing to a method XML resource
  - Create the IME method XML resource file (`res/xml/method.xml`) declaring the input method subtype
  - Add Kotest and Kotest Property Testing dependencies (`io.kotest:kotest-runner-junit5` and `io.kotest:kotest-property`) to the module's `build.gradle.kts` for JVM unit tests
  - Configure JUnit Platform for unit tests in `build.gradle.kts`
  - _Requirements: 8.2, 8.3, 8.4_

- [x] 2. Implement core data models and keyboard layout definitions
  - [x] 2.1 Define `Key`, `ActionType`, `ShiftState`, and `KeyboardLayer` sealed classes and enums in Kotlin
    - Implement `Key.Letter`, `Key.Symbol`, `Key.Action` as described in the design
    - Implement `ShiftState` enum (`OFF`, `SINGLE`, `CAPS_LOCK`) and `KeyboardLayer` enum (`QWERTY`, `SYMBOL`)
    - _Requirements: 1.3, 1.4, 1.5, 1.6, 2.1, 6.1_

  - [x] 2.2 Implement `QwertyLayout` object with all rows
    - Define the four rows: top row (Q–P), home row (A–L), shift row (Shift, Z–M, Backspace), and bottom row (SwitchToSymbols, Space, Enter)
    - Expose `allLetterKeys: List<Key.Letter>` helper for property tests
    - _Requirements: 1.1, 1.2, 2.1, 3.1, 4.1, 5.1, 6.1_

  - [x] 2.3 Implement `SymbolLayout` object with all rows
    - Define the four rows: digits row, punctuation row 1, punctuation row 2, punctuation row 3, and bottom row (SwitchToQwerty, Space, Backspace)
    - Expose `allSymbolKeys: List<Key.Symbol>` helper for property tests
    - _Requirements: 6.3, 6.5, 6.7_

  - [ ]* 2.4 Write unit tests for layout definitions
    - Verify `QwertyLayout` contains all 26 `Letter` keys in correct QWERTY order
    - Verify `QwertyLayout` contains `Action(SHIFT)`, `Action(BACKSPACE)`, `Action(SPACE)`, `Action(ENTER)`, `Action(SWITCH_TO_SYMBOLS)`
    - Verify `SymbolLayout` contains all required digit and punctuation `Symbol` keys
    - Verify `SymbolLayout` contains `Action(SWITCH_TO_QWERTY)`, `Action(BACKSPACE)`, `Action(SPACE)`
    - _Requirements: 1.2, 6.3, 6.5, 6.7_

- [x] 3. Implement `KeyboardController` — shift state and layer switching
  - [x] 3.1 Define `InputActions` and `ViewActions` interfaces
    - `InputActions`: `commitText(text: String)`, `deleteCharBefore()`, `performEditorAction(actionCode: Int)`
    - `ViewActions`: `updateShiftIndicator(state: ShiftState)`, `switchLayer(layer: KeyboardLayer)`, `showKeyPreview(key: Key)`, `dismissKeyPreview()`, `updateEnterLabel(imeOptions: Int)`
    - _Requirements: 1.3, 2.6, 5.2, 5.3_

  - [x] 3.2 Implement `KeyboardController` class with shift state machine
    - Implement `onShiftTapped()` with the full transition table: OFF→SINGLE, SINGLE→OFF, double-tap within 400ms→CAPS_LOCK, CAPS_LOCK→OFF
    - Use `SystemClock.uptimeMillis()` for double-tap timing
    - Call `viewActions.updateShiftIndicator()` on every state change
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 3.3 Implement layer switching in `KeyboardController`
    - Handle `Key.Action(SWITCH_TO_SYMBOLS)` → set `currentLayer = SYMBOL`, call `viewActions.switchLayer(SYMBOL)`
    - Handle `Key.Action(SWITCH_TO_QWERTY)` → set `currentLayer = QWERTY`, call `viewActions.switchLayer(QWERTY)`
    - Dismiss any visible key preview before switching layers
    - _Requirements: 6.2, 6.6_

  - [ ]* 3.4 Write unit tests for shift state transitions and layer switching
    - Test each shift transition: OFF→SINGLE, SINGLE→OFF, CAPS_LOCK→OFF
    - Test double-tap within 400ms sets CAPS_LOCK; double-tap outside 400ms does not
    - Test layer switching: QWERTY→SYMBOL on `SWITCH_TO_SYMBOLS`, SYMBOL→QWERTY on `SWITCH_TO_QWERTY`
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 6.2, 6.6_

- [x] 4. Implement `KeyboardController` — key commit and enter action
  - [x] 4.1 Implement `onKeyTapped(key: Key)` for letter and symbol keys
    - For `Key.Letter`: apply shift state to determine case, call `inputActions.commitText()`, transition SINGLE→OFF after commit
    - For `Key.Symbol`: call `inputActions.commitText(key.char.toString())` with no case transformation
    - For `Key.Action(SPACE)`: call `inputActions.commitText(" ")`
    - _Requirements: 1.3, 1.4, 1.5, 1.6, 4.2, 6.4_

  - [ ]* 4.2 Write property test for letter commit respects shift state (Property 1)
    - **Property 1: Letter commit respects shift state**
    - **Validates: Requirements 1.3, 1.4, 1.5, 1.6**
    - Generator: `Arb.element(QwertyLayout.allLetterKeys)` × `Arb.enum<ShiftState>()`
    - Verify committed char case and resulting shift state match the expected transition table
    - Tag: `// Feature: android-custom-keyboard, Property 1: Letter commit respects shift state`

  - [ ]* 4.3 Write property test for symbol key commit matches character (Property 2)
    - **Property 2: Symbol key commit matches character**
    - **Validates: Requirements 6.4**
    - Generator: `Arb.element(SymbolLayout.allSymbolKeys)`
    - Verify `commitText` called with `key.char.toString()`
    - Tag: `// Feature: android-custom-keyboard, Property 2: Symbol key commit matches character`

  - [x] 4.4 Implement enter key action mapping in `KeyboardController`
    - Implement `onStartInput(imeOptions: Int)` to store current imeOptions and call `viewActions.updateEnterLabel(imeOptions)`
    - Implement `onKeyTapped` for `Key.Action(ENTER)`: map imeOptions to action code and call `inputActions.performEditorAction(actionCode)`
    - Use the mapping table: `IME_ACTION_DONE`, `IME_ACTION_SEARCH`, `IME_ACTION_SEND`, `IME_ACTION_GO`, `IME_ACTION_NEXT`, default → `IME_ACTION_NONE`
    - _Requirements: 5.2, 5.3_

  - [ ]* 4.5 Write property test for enter key action code matches imeOptions (Property 3)
    - **Property 3: Enter key action code matches imeOptions**
    - **Validates: Requirements 5.2**
    - Generator: `Arb.element(listOf(IME_ACTION_DONE, IME_ACTION_SEARCH, IME_ACTION_SEND, IME_ACTION_GO, IME_ACTION_NEXT, EditorInfo.IME_ACTION_NONE))`
    - Verify `performEditorAction` called with the expected action code from the mapping table
    - Tag: `// Feature: android-custom-keyboard, Property 3: Enter key action code matches imeOptions`

  - [ ]* 4.6 Write property test for enter key label matches imeOptions (Property 4)
    - **Property 4: Enter key label matches imeOptions**
    - **Validates: Requirements 5.3**
    - Generator: same imeOptions set as Property 3
    - Verify `updateEnterLabel` called with the expected label string from the mapping table
    - Tag: `// Feature: android-custom-keyboard, Property 4: Enter key label matches imeOptions`

- [x] 5. Implement backspace with repeat in `KeyboardController`
  - [x] 5.1 Implement `onBackspaceDown()` and `onBackspaceUp()` with repeat logic
    - On `onBackspaceDown()`: call `inputActions.deleteCharBefore()` once, then schedule repeat via `Handler.postDelayed` with 400ms initial delay and 50ms repeat interval
    - On `onBackspaceUp()`: cancel any pending repeat callbacks
    - Null-check `InputConnection` before each delete call; handle empty input gracefully (no crash, no repeat scheduling)
    - _Requirements: 3.2, 3.3, 3.4_

  - [ ]* 5.2 Write unit tests for backspace behaviour
    - Test single tap calls `deleteCharBefore` exactly once
    - Test hold > 400ms triggers repeat at ~50ms intervals using a fake `Handler` / test clock
    - Test backspace on empty input performs no action
    - _Requirements: 3.2, 3.3, 3.4_

- [x] 6. Checkpoint — Ensure all JVM unit and property tests pass
  - Run `./gradlew test` and confirm all tests in the `KeyboardController`, `QwertyLayout`, and `SymbolLayout` test suites pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Implement `KeyView` — single key rendering and touch events
  - [x] 7.1 Create `KeyView` custom `View` class
    - Draw rounded-rectangle background using `#37474F` (normal) and a darkened variant (pressed), with corner radius matching Gboard style
    - Draw key label in white (`#FFFFFF`), sans-serif, minimum 14sp; use icon drawable for action keys (Shift, Backspace, Enter return arrow)
    - Enforce minimum touch target of 44dp × 44dp
    - _Requirements: 7.2, 7.3_

  - [x] 7.2 Wire touch events from `KeyView` to `KeyboardController`
    - On `ACTION_DOWN`: notify controller (`onKeyTapped` for most keys; `onBackspaceDown` for Backspace; `onShiftTapped` for Shift)
    - On `ACTION_UP` / `ACTION_CANCEL`: call `onBackspaceUp()` if Backspace was held
    - Deliver visual press feedback (darkened background) within 16ms of `ACTION_DOWN`
    - _Requirements: 7.4_

- [x] 8. Implement `KeyboardView` — layout, layer switching, and key preview
  - [x] 8.1 Create `KeyboardView` custom `ViewGroup`
    - Set keyboard panel background to `#263238`
    - Inflate `KeyView` instances from the active layer's row/key data and lay them out in rows
    - Scale key widths proportionally to available display width so layout is consistent across screen sizes
    - _Requirements: 7.1, 7.6_

  - [x] 8.2 Implement layer switching in `KeyboardView`
    - Implement `ViewActions.switchLayer(layer)`: remove all child `KeyView`s and re-inflate from the new layer's layout data
    - Implement `ViewActions.updateShiftIndicator(state)`: update the Shift `KeyView`'s visual indicator for `OFF`, `SINGLE`, and `CAPS_LOCK` states
    - Implement `ViewActions.updateEnterLabel(imeOptions)`: update the Enter `KeyView`'s label text or icon
    - _Requirements: 2.6, 5.3, 6.2, 6.6_

  - [x] 8.3 Implement key popup preview overlay
    - Implement `ViewActions.showKeyPreview(key)`: display a popup overlay above the tapped `KeyView` showing the key character
    - Implement `ViewActions.dismissKeyPreview()`: hide the overlay; auto-dismiss after 800ms using `Handler.postDelayed`
    - Dismiss immediately when layer switches (prevents stale overlays)
    - _Requirements: 7.5_

- [x] 9. Implement `CustomIMEService` and wire all components together
  - [x] 9.1 Create `CustomIMEService` extending `InputMethodService`
    - Override `onCreateInputView()`: instantiate `KeyboardController` with `InputActions` and `ViewActions` implementations, create and return `KeyboardView`
    - Override `onStartInputView(info: EditorInfo, restarting: Boolean)`: call `controller.onStartInput(info.imeOptions)` to update Enter key label
    - Implement `InputActions` using `currentInputConnection`, with null-checks before every call
    - _Requirements: 5.2, 5.3, 8.2_

  - [x] 9.2 Register `CustomIMEService` in `AndroidManifest.xml` and verify IME is selectable
    - Confirm `<service>` entry has `android:permission="android.permission.BIND_INPUT_METHOD"` and the correct `<intent-filter>` and `<meta-data>` elements
    - _Requirements: 1.1_

- [x] 10. Checkpoint — Ensure all tests pass and the APK builds
  - Run `./gradlew assembleDebug` and confirm the build succeeds with no errors
  - Run `./gradlew test` and confirm all unit and property tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Implement GitHub Actions CI/CD pipeline
  - [x] 11.1 Create `.github/workflows/build.yml` with trigger, JDK setup, and build steps
    - Set `on: push` and `on: pull_request` both targeting the `main` branch
    - Add `actions/checkout` step
    - Add `actions/setup-java` step with `java-version: '17'` and `distribution: 'temurin'`
    - Add Gradle build step running `./gradlew assembleDebug --build-cache`; do NOT set `continue-on-error: true`
    - Add `actions/upload-artifact` step to upload the generated APK with `retention-days: 7`
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

  - [x] 11.2 Add Gradle wrapper and dependency caching steps to the workflow
    - Add `actions/cache` step for `~/.gradle/wrapper` with key `${{ runner.os }}-gradle-wrapper-${{ hashFiles('gradle/wrapper/gradle-wrapper.properties') }}`
    - Add `actions/cache` step for `~/.gradle/caches` with key `${{ runner.os }}-gradle-deps-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}`
    - Place both cache restore steps before the Gradle build step
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 12. Final checkpoint — Ensure all tests pass
  - Run `./gradlew test` and confirm all unit and property tests pass
  - Verify the workflow YAML is valid: confirm trigger branches, JDK version, Gradle command, artifact retention, cache keys, and absence of `continue-on-error` on the build step
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at logical boundaries
- Property tests (Properties 1–4) validate universal correctness of the `KeyboardController` state machine using Kotest Property Testing
- Unit tests validate specific examples and edge cases
- The `KeyboardController` has no Android dependencies and runs entirely on the JVM — all logic tests are fast and do not require an emulator
- UI and instrumentation tests (key press feedback, popup preview, proportional sizing, shift indicator visuals) require an Android emulator and are not included as coding tasks here; they are covered by the design's instrumentation test strategy
