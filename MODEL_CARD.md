# Model card: digit_landmarks_v1

- **Task:** classify one hand's ISL digit sign, 0-9.
- **Input:** 42 floats, the 21 MediaPipe hand landmarks as `[x0,y0,...,x20,y20]`, normalised image coordinates (0-1) of the upright, non-mirrored frame. No wrist-relative normalisation.
- **Output:** 10 softmax scores, index = digit. Scores are not calibrated, so the app never shows them as percentages.
- **Asset:** `app/src/main/assets/digit_landmarks_v1.tflite` (same file as `model/gesture_model.tflite`).
- **App policy:** a frame counts only if the top score is at least 0.85; a digit is shown only when it wins 4 of the last 6 frames; no hand or more than one hand is reported separately.
- **Hand landmarker:** `app/src/main/assets/hand_landmarker.task`, Google's official MediaPipe float16 model (SHA-256 `fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1`), downloaded from `storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/`. Check its licence before distributing the app.
- **ABIs:** MediaPipe ships no x86_64 library, so the APK is built for arm64-v8a and armeabi-v7a only.

## Evidence so far

`model/evaluate_tflite.py` runs the model on a random 40 images per class of `model/Dataset` after MediaPipe landmark extraction. On the last run: 400 images, 92 with no hand found, 308 evaluated, 98.7% correct. This is an in-distribution sanity check, **not** a held-out-signer evaluation: the split is random, signer and background are unknown, and the training provenance of the .tflite is not documented in the repository.

## Known gaps (release blockers for the camera feature)

- No held-out signer/background evaluation, per-class recall, confusion matrix, latency or battery data.
- Mirroring/handedness of the training photos is unknown; front-camera frames are fed unmirrored.
- Raw (not wrist-relative) coordinates make the model sensitive to where the hand sits in the frame.
- Not tested on a real phone or with a real hand: on the emulator the pipeline runs end to end and correctly reports "No hand detected" on the virtual scene, but no digit has been recognised yet.
