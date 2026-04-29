# Requirements Document

## Introduction

This feature is a custom Android keyboard application built as an Input Method Editor (IME) using Android's `InputMethodService`. The keyboard is styled after Google's Gboard — clean, Material Design-inspired UI with smooth interactions. It covers the full basic keyboard experience: QWERTY letter layout, a numbers/symbols layer, shift/caps lock, backspace, space, and enter/return. The project also includes a GitHub Actions CI/CD pipeline that builds and packages the APK, with Gradle dependency and build output caching to keep build times fast.

## Glossary

- **Keyboard**: The Android custom Input Method Editor (IME) application built in this project.
- **IME**: Input Method Editor — the Android system component that provides on-screen keyboard input via `InputMethodService`.
- **InputConnection**: The Android API interface through which the Keyboard sends text and key events to the target application.
- **KeyView**: A single key widget rendered on the keyboard layout.
- **KeyboardView**: The full keyboard panel rendered by the IME, containing all KeyViews.
- **QWERTY_Layer**: The primary keyboard layer showing the standard QWERTY alphabetic layout.
- **Symbol_Layer**: The secondary keyboard layer showing numbers, punctuation, and common symbols.
- **Shift_State**: The current capitalisation mode of the Keyboard — one of: `off`, `single` (next character only), or `caps_lock` (all characters).
- **CI_Pipeline**: The GitHub Actions workflow that builds, tests, and packages the APK.
- **Gradle_Cache**: The cached Gradle wrapper, dependencies, and build outputs stored between CI_Pipeline runs.

---

## Requirements

### Requirement 1: QWERTY Letter Input

**User Story:** As a user, I want a standard QWERTY keyboard layout, so that I can type alphabetic text in any Android app.

#### Acceptance Criteria

1. THE Keyboard SHALL display the QWERTY_Layer as the default layer on launch.
2. THE QWERTY_Layer SHALL contain all 26 letters of the English alphabet arranged in the standard QWERTY row order (Q–P, A–L, Z–M).
3. WHEN a letter KeyView is tapped, THE Keyboard SHALL commit the corresponding character to the InputConnection of the focused application.
4. WHEN the Shift_State is `single`, THE Keyboard SHALL commit the tapped letter as an uppercase character and then return Shift_State to `off`.
5. WHILE the Shift_State is `caps_lock`, THE Keyboard SHALL commit every tapped letter as an uppercase character.
6. WHILE the Shift_State is `off`, THE Keyboard SHALL commit every tapped letter as a lowercase character.

---

### Requirement 2: Shift and Caps Lock

**User Story:** As a user, I want shift and caps lock controls, so that I can type uppercase letters when needed.

#### Acceptance Criteria

1. THE QWERTY_Layer SHALL display a Shift key.
2. WHEN the Shift key is tapped once while Shift_State is `off`, THE Keyboard SHALL set Shift_State to `single`.
3. WHEN the Shift key is tapped once while Shift_State is `single`, THE Keyboard SHALL set Shift_State to `off`.
4. WHEN the Shift key is double-tapped within 400ms, THE Keyboard SHALL set Shift_State to `caps_lock`.
5. WHEN the Shift key is tapped while Shift_State is `caps_lock`, THE Keyboard SHALL set Shift_State to `off`.
6. THE KeyboardView SHALL render the Shift key with a visually distinct indicator for each Shift_State value (`off`, `single`, `caps_lock`).

---

### Requirement 3: Backspace

**User Story:** As a user, I want a backspace key, so that I can delete characters I have typed.

#### Acceptance Criteria

1. THE QWERTY_Layer SHALL display a Backspace key.
2. WHEN the Backspace key is tapped, THE Keyboard SHALL delete the single character immediately before the cursor in the InputConnection.
3. WHEN the Backspace key is held for more than 400ms, THE Keyboard SHALL begin deleting characters continuously at a rate of one character per 50ms until the key is released.
4. IF the InputConnection has no characters before the cursor, THEN THE Keyboard SHALL perform no action when the Backspace key is tapped.

---

### Requirement 4: Space Key

**User Story:** As a user, I want a space key, so that I can insert spaces between words.

#### Acceptance Criteria

1. THE QWERTY_Layer SHALL display a Space key that is visually wider than a standard letter KeyView.
2. WHEN the Space key is tapped, THE Keyboard SHALL commit a single space character to the InputConnection.

---

### Requirement 5: Enter / Return Key

**User Story:** As a user, I want an enter/return key, so that I can submit forms, start new lines, or trigger actions in the focused app.

#### Acceptance Criteria

1. THE QWERTY_Layer SHALL display an Enter key.
2. WHEN the Enter key is tapped, THE Keyboard SHALL send the action code corresponding to the `imeOptions` of the current InputConnection (e.g., `IME_ACTION_DONE`, `IME_ACTION_SEARCH`, `IME_ACTION_SEND`, or a newline character when no specific action is set).
3. THE KeyboardView SHALL display a label on the Enter key that reflects the current `imeOptions` action (e.g., "Done", "Search", "Send", or a return arrow icon).

---

### Requirement 6: Numbers and Symbols Layer

**User Story:** As a user, I want access to numbers and symbols, so that I can type non-alphabetic characters without leaving the keyboard.

#### Acceptance Criteria

1. THE QWERTY_Layer SHALL display a layer-switch key labelled "?123".
2. WHEN the "?123" key is tapped, THE Keyboard SHALL switch the KeyboardView to display the Symbol_Layer.
3. THE Symbol_Layer SHALL contain the digits 0–9 and a set of common punctuation and symbols including: `! @ # $ % ^ & * ( ) - _ = + [ ] { } ; : ' " , . < > / ? \ |`.
4. WHEN a key on the Symbol_Layer is tapped, THE Keyboard SHALL commit the corresponding character to the InputConnection.
5. THE Symbol_Layer SHALL display a layer-switch key labelled "ABC".
6. WHEN the "ABC" key is tapped, THE Keyboard SHALL switch the KeyboardView back to the QWERTY_Layer.
7. THE Symbol_Layer SHALL display a Backspace key and a Space key with the same behaviour defined in Requirements 3 and 4.

---

### Requirement 7: Gboard-Style Visual Design

**User Story:** As a user, I want the keyboard to look and feel like Google's Gboard, so that the experience is familiar and visually polished.

#### Acceptance Criteria

1. THE KeyboardView SHALL use a dark background colour (`#263238` or equivalent Material Blue Grey 900) for the keyboard panel.
2. THE KeyView SHALL render each key with a slightly lighter rounded-rectangle background (`#37474F` or equivalent Material Blue Grey 800) and a minimum touch target size of 44dp × 44dp.
3. THE KeyView SHALL display key labels in white (`#FFFFFF`) using a sans-serif typeface at a minimum size of 14sp.
4. WHEN a KeyView is pressed, THE KeyboardView SHALL display a visual press feedback (darkened key background or ripple effect) within 16ms of the touch event.
5. THE KeyboardView SHALL display a popup preview of the tapped key character above the KeyView for letter and symbol keys, dismissing automatically after 800ms.
6. THE KeyboardView SHALL maintain consistent key sizing and spacing across all screen widths by scaling key widths proportionally to the available display width.

---

### Requirement 8: GitHub Actions CI/CD Pipeline

**User Story:** As a developer, I want a GitHub Actions workflow that builds the APK automatically, so that every push is verified and a distributable APK is produced.

#### Acceptance Criteria

1. THE CI_Pipeline SHALL trigger on every push to the `main` branch and on every pull request targeting the `main` branch.
2. WHEN triggered, THE CI_Pipeline SHALL check out the repository source code.
3. WHEN triggered, THE CI_Pipeline SHALL set up a Java Development Kit (JDK) version 17.
4. WHEN triggered, THE CI_Pipeline SHALL execute `./gradlew assembleDebug` to produce a debug APK.
5. IF the `assembleDebug` task exits with a non-zero code, THEN THE CI_Pipeline SHALL mark the workflow run as failed and halt further steps.
6. WHEN the `assembleDebug` task succeeds, THE CI_Pipeline SHALL upload the generated APK file as a workflow artifact retained for 7 days.

---

### Requirement 9: Gradle Build Caching in CI

**User Story:** As a developer, I want Gradle dependencies and build outputs cached in GitHub Actions, so that subsequent builds complete faster than a cold build.

#### Acceptance Criteria

1. THE CI_Pipeline SHALL cache the Gradle wrapper files located at `~/.gradle/wrapper` using a cache key derived from the operating system and the contents of `gradle/wrapper/gradle-wrapper.properties`.
2. THE CI_Pipeline SHALL cache the Gradle dependency files located at `~/.gradle/caches` using a cache key derived from the operating system and the hash of all `*.gradle*` and `gradle-wrapper.properties` files in the repository.
3. WHEN a cache entry matching the current key exists, THE CI_Pipeline SHALL restore the cached files before executing any Gradle tasks.
4. WHEN no matching cache entry exists, THE CI_Pipeline SHALL execute Gradle tasks without a cache restore and save a new cache entry after the build completes.
5. THE CI_Pipeline SHALL pass `--build-cache` to the `assembleDebug` Gradle invocation to enable Gradle's remote and local build output cache.
6. WHEN a cached build is used, THE CI_Pipeline SHALL complete the `assembleDebug` step in less time than a cold build on the same runner.
