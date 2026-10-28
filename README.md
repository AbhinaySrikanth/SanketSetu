# SanketSetu

An offline Android app to learn a curated set of Indian Sign Language (ISL) signs, practise them, and show phrase cards to other people. English and Gujarati meanings. Formerly "ISL English-Gujarati App".

**It is not a translator.** It does not interpret sentences or conversations and does not replace an interpreter.

## What is implemented

| Area | Status |
|---|---|
| Learn: 172 signs in 11 lessons (pictures and videos), search, saved signs, recently viewed | Implemented |
| Practice: spaced-repetition flashcards, multiple-choice quizzes, local progress | Implemented |
| Communicate: 4 situations of phrase cards with a large display mode | Implemented |
| Camera check for digits 0-9 | Experimental. Pipeline runs on the emulator; not yet tested with a real hand or phone (see MODEL_CARD.md). If a model file is missing the screen says it is unavailable. |
| Daily goal, day streak, review of missed signs, optional 6 pm reminder (only on days you have not practised) | Implemented |
| Half-speed playback for sign videos | Implemented |
| Phrase board: pin phrases, swipe between phrases in a situation, speak aloud with the device's own English/Gujarati voice (spoken language, not a sign) | Implemented. Gujarati voice depends on the device. |
| Sign-it practice: camera asks for a digit and scores only a stable correct match | Experimental, same limits as the camera check |
| Settings: Gujarati on/off, text size, high contrast, goal, reminder, reset, privacy | Implemented |
| Continuous or sentence-level recognition, text-to-speech, cloud features | Not implemented, not planned for this release |

Gujarati text has not had a native-speaker review. Previously reported accuracy figures are not reproducible and are not claimed.

## Structure

- `data/` sign catalog (single source of truth, media are compile-time `R` references), user store, spaced repetition, session state
- `ml/` recognition: MediaPipe hand landmarks -> 42 floats -> TFLite digit model -> `RecognitionPolicy` (threshold + smoothing)
- `ui/` single-activity Compose app: `Nav.kt` holds the route map and back rules, `screens/` one file per area
- `model/` research scripts (training, evaluation). Not part of the app.

## Build

```
gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Requires JDK 17 and the Android SDK (platform 36). Release builds use R8 and resource shrinking; signing is not configured in the repository.
