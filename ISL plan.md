# SanketSetu — Product Revamp Master Plan

## 1. Executive decision

### Proposed product name

**SanketSetu**  
**Tagline:** *Learn ISL. Practice with confidence. Communicate more clearly.*

“SanketSetu” means a bridge through signs. It is more credible and memorable than “ISL English-Gujarati Translator,” while leaving room for learning, practice, and carefully scoped communication assistance.

### Product direction

Convert the current college demonstration into a **hybrid ISL learning and communication-support app**:

1. **Learning foundation:** curated ISL signs, short lessons, English/Gujarati meanings, examples, and offline access.
2. **Practice:** guided recall, quizzes, pronunciation/text prompts, saved signs, and progress.
3. **Recognition assistance:** validated recognition of a limited set of supported static signs, with confidence and uncertainty shown honestly.
4. **Communication support:** reusable phrase cards and quick visual references for everyday situations.

The app must not claim to translate unrestricted Indian Sign Language, interpret conversations, or replace a qualified interpreter until those capabilities are independently validated. The product should assist communication and learning, not make unsafe or overconfident decisions for users.

## 2. Current repository snapshot

### Current technology and structure

- Android application in Kotlin with XML layouts and multiple `AppCompatActivity` screens.
- CameraX dependencies are present and `CameraActivity` displays a camera preview.
- TensorFlow Lite dependencies and a root `gesture_model.tflite` exist, but the Android classifier loads `model.tflite` from assets; the repository evidence does not show a correctly wired, verified model asset.
- `Classifier.kt` preprocesses a bitmap but returns a constant instead of the model output; `CameraActivity` does not pass the captured image to `Classifier.predict`.
- The camera capture path sends a hard-coded `"5"` result to `GestureActivity`.
- A separate Python pipeline uses MediaPipe landmarks and Random Forest (`model/train_model.py`, `model/ui.py`), but it is not the same inference contract as the Android TensorFlow Lite path.
- Learning content is hard-coded across `BasicActivity`, `IntermediateActivity`, `AdvancedActivity`, and `TranslationData`.
- Images and videos are addressed through resource-name conventions and dynamic lookups.
- There are many report, proposal, research-paper, and presentation artifacts in the repository/root and `Output` directory.
- Test coverage is effectively a starter template; there is no demonstrated end-to-end camera/model test suite.

### Existing user journey

`SplashActivity` → `MainActivity` → `DifficultyActivity` → one of:

- Beginner → `BasicActivity` → `GestureActivity`
- Intermediate → `IntermediateActivity` → `GestureActivity`
- Advanced → `AdvancedActivity` → `GestureActivity`
- Camera icon → `CameraActivity` → capture → `GestureActivity`
- About → `AboutActivity`

### What visibly works today

- App launches through a splash screen and reaches a home screen.
- About page opens and attempts email/dialer intents.
- Difficulty cards navigate to three sign lists.
- Sign grids render content from hard-coded lists.
- Detail screens can show a drawable or loop a bundled video.
- English/Gujarati text is displayed from a local map for many entries.
- Camera permission and a CameraX preview are attempted.
- The application can operate offline for bundled catalogue media and translations.

## 3. Report and presentation claims versus evidence

### Claims currently made

The report, presentation, proposal, and research paper describe the project as:

- “ISL Translator Application: A Real-Time Android Application for English and Gujarati Sign Language Learning and Recognition.”
- A real-time TensorFlow Lite/CNN gesture-recognition system.
- A camera that processes live input and identifies hand signs.
- English and Gujarati translation support.
- High training/validation accuracy, including approximately 98% training and 95% validation claims in the research material.
- A communication/accessibility solution rather than only a learning catalogue.
- Offline static translation plus live recognition.

### Evidence that contradicts or limits those claims

- `CameraActivity.takePhoto()` ignores image pixels and routes to a fixed digit value.
- `Classifier.predict()` creates an input array, calls the interpreter with the wrong conceptual data flow, derives an index that is not returned, and returns a constant `count`.
- The classifier expects a 28×28 grayscale-style input while the separate Python model uses 42 normalized landmark values; these are different model contracts.
- The camera path does not show confidence, “no hand,” “uncertain,” or inference failure states.
- The app displays one recognized result after a capture; it does not perform continuous live recognition or translation.
- “English/Gujarati translation” is a static label-to-text dictionary, not language translation or sign-language interpretation.
- The report’s accuracy claims are not reproducibly tied to a documented held-out dataset, Android model artifact, device matrix, or production evaluation report.
- The report and presentation describe future or aspirational functionality as if it were implemented.

### Documentation correction policy

All reports, slides, screenshots, About text, store copy, and diagrams must be rewritten to:

- Separate **implemented**, **planned**, and **research prototype** capabilities.
- State the supported sign set and known limitations.
- Replace “real-time translator” with “learning and communication-support app” unless live recognition passes release gates.
- Remove unsupported accuracy numbers and use reproducible evaluation results only.
- Never imply that a recognition result is guaranteed, medically reliable, or equivalent to human interpretation.

## 4. Complete flaw inventory

### A. Functional and correctness flaws

- Fake camera recognition: the capture result is hard-coded to digit 5.
- A special “capture zero” control bypasses recognition and opens digit zero directly.
- The model is initialized but not used for the captured frame.
- `ImageProxy` is not converted, closed, or lifecycle-managed as a model input.
- The classifier’s output contract is incorrect and its predicted index is discarded.
- Model filename/location and model format are inconsistent across repository files.
- No explicit label map guarantees model output index → supported sign identity.
- No confidence threshold, temporal smoothing contract, or uncertain-result state exists in Android.
- Camera initialization failures are reduced to short Toast messages.
- Permission denial simply finishes the camera screen without a recovery path or rationale.
- The camera executor and model interpreter have no complete lifecycle/resource ownership design.
- Resource lookup can silently produce missing images or placeholder content.
- Several content identifiers are inconsistent or typo-prone (`diaeases`, filenames with spaces, duplicated entries).
- Duplicate advanced translation records exist in `TranslationData`.
- Unknown translations degrade to “Translation not found” instead of a curated content validation failure.
- Back navigation, selected state, and return paths are not designed as a coherent product flow.
- Static lists and media are not validated at build time.

### B. Product and scope flaws

- The product name promises translation beyond current capability.
- Learning levels are arbitrary catalogue buckets rather than a curriculum.
- There is no learner objective, onboarding, baseline assessment, or progress model.
- There is no practice loop: users browse media but cannot meaningfully demonstrate learning.
- There is no search, category filter, favorites, history, or recently viewed list.
- There is no phrase composition, communication board, or useful everyday workflow.
- Gujarati content has no editorial provenance, language review, or variant policy.
- Recognition is framed as certainty and does not explain limitations.
- The app does not define its intended users: learner, hearing family member, teacher, or communication partner.

### C. UX, visual, and accessibility flaws

- Activity-per-screen navigation creates a fragmented mental model.
- “Difficulty” is presented before a user understands what the product does.
- The camera action is an icon/control with insufficient explanation of its limitations.
- Grid cards lack clear text labels and content context.
- No loading, empty, offline, retry, error, or model-unavailable states are designed.
- Toasts are transient and inaccessible as the primary error communication.
- Fixed layouts, hard-coded text, and scattered colors/dimensions reduce consistency.
- No demonstrated screen-reader labels, touch-target audit, contrast audit, dynamic text support, or reduced-motion behavior.
- Splash delay adds friction without delivering product value.
- Contact details are hard-coded in source and should not be treated as product functionality.

### D. Engineering and maintainability flaws

- Content, presentation, navigation, and resource identifiers are coupled inside Activities.
- No repository/use-case/view-model boundary exists.
- No typed content schema or single source of truth exists.
- Legacy and modern UI dependencies are mixed unnecessarily.
- No dependency/version or release hygiene plan is visible.
- Release minification is disabled.
- Camera/model work is not demonstrably off the main thread end-to-end.
- No structured logging, crash reporting, performance metrics, or privacy boundary is defined.
- No automated validation protects media names, translation coverage, model labels, or navigation.
- Test coverage does not represent the critical user journeys.

### E. ML/data flaws

- Dataset provenance, consent, class balance, signer diversity, lighting/background variation, and split methodology are not documented.
- Random image splits may leak signer/background characteristics and inflate metrics.
- Static landmark recognition is not equivalent to dynamic ISL recognition.
- One hand and 2D landmark assumptions limit real-world robustness.
- No calibration or threshold selection process is documented.
- No confusion matrix, per-class recall, rejection rate, latency, or device energy data is exposed.
- No model versioning or compatibility contract exists between training and app release.
- There is no safe behavior for unsupported signs, multiple hands, occlusion, poor lighting, or no hand.

## 5. Feature disposition

| Current feature/control | Status | Decision |
|---|---|---|
| Splash screen with fixed delay | Working but low value | Remove delay; use a launch screen only while loading real state |
| About page | Working but overclaims | Keep, rewrite as transparent product/help/about content |
| Beginner/intermediate/advanced grids | Partially useful | Redesign as curriculum, categories, and search |
| Static images | Useful | Keep after content, licensing, naming, and accuracy review |
| Bundled gesture videos | Useful | Keep after media QA, captions/controls, and content review |
| English/Gujarati labels | Useful | Keep with reviewed translations and localization resources |
| Camera preview | Useful foundation | Keep and rebuild around real inference states |
| Capture photo button | Misdleading for “live” recognition | Replace with clearly labeled single-sign practice or validated live mode |
| Hard-coded zero/five controls | Fake/demo-only | Remove completely |
| TensorFlow Lite classifier | Unfinished/wrongly wired | Repair only after model contract is established |
| Python Streamlit recognizer | Research utility, not product | Keep outside app as reproducible evaluation tool; do not present as Android behavior |
| Difficulty labels | Weak product model | Replace with learning path/progress categories |
| Contact email/phone | Not core product value | Move to support/about configuration; remove personal hard-coded data |
| Claims of unrestricted translation | Unsafe and inaccurate | Remove |
| Accuracy claims in documents | Unverified | Remove until independently reproduced |
| Future voice, continuous translation, and multi-sign interpretation | Not implemented | Defer to research roadmap; never present as shipped |

## 6. Target product definition

### Primary users

1. Beginners learning practical ISL vocabulary.
2. Family members, classmates, teachers, and caregivers who want a reliable reference.
3. Users who need quick visual phrase support in limited everyday contexts.
4. Evaluators who need a credible, reproducible accessibility-oriented product demonstration.

### Product promise

“SanketSetu helps people learn a curated set of Indian Sign Language signs, practice recall, and use verified visual phrase references. On supported devices, it can provide recognition assistance for a limited, explicitly documented sign set.”

### Non-goals for the first production release

- Full ISL grammar interpretation.
- Open-ended sentence translation.
- Automatic interpretation of conversations.
- Medical, emergency, legal, or safety-critical decisions based solely on recognition.
- Claiming that English/Gujarati text is a complete representation of ISL.
- Cloud upload of camera frames without explicit consent and privacy design.

## 7. Proposed information architecture and navigation

### Bottom navigation

- **Home:** continue learning, daily practice, quick actions, recently used phrases.
- **Learn:** curriculum, categories, search, filters, sign detail.
- **Practice:** flashcards, recall, quizzes, progress, review mistakes.
- **Communicate:** phrase cards, favorites, situations, optional recognition assistance.
- **Profile/Settings:** language, accessibility, privacy, downloads, help, model version.

### Core routes

1. First launch → value proposition → privacy/camera explanation → optional goal and language selection.
2. Home → recommended lesson or resume practice.
3. Learn → category/lesson → sign detail → save/practice/share visual reference.
4. Practice → prompt → user response → confidence-aware feedback → next item → summary.
5. Recognize → camera education → permission → live/single-sign capture → quality checks → result/uncertain state → retry or view sign.
6. Communicate → situation → phrase cards → large readable sign/text display → favorite/share.
7. Settings → language, text size, contrast, haptics, permissions, privacy, about.

### Required states

- First run, returning user, loading, offline, empty search, no hand detected, multiple hands, poor lighting, unsupported sign, low confidence, model unavailable, camera denied, camera unavailable, media missing, corrupted content, and recoverable inference failure.

## 8. UX and design system plan

### Visual direction

- Calm, high-contrast, accessible visual language; avoid decorative “AI magic” claims.
- One primary action per screen and explicit text labels for camera/practice actions.
- Large sign media, short explanations, clear English/Gujarati hierarchy.
- Consistent spacing, typography, colors, cards, buttons, icons, and feedback states.
- Use real content imagery and inclusive representation; avoid stock imagery that implies capability.

### Accessibility

- Minimum touch targets and contrast checked systematically.
- Content descriptions for all meaningful images and camera controls.
- Screen-reader order and semantic labels tested.
- Dynamic font scaling and landscape/large-screen layouts supported.
- Captions/transcripts and playback controls for videos.
- Do not rely on color, motion, or Toasts alone to communicate results.

### Recognition UX

- Explain supported signs before permission is requested.
- Show hand-position/lighting guidance without pretending it guarantees accuracy.
- Show “Recognizing,” “No hand detected,” “Not confident,” and “Supported sign: …” as distinct states.
- Display confidence only if calibrated and explain it in plain language.
- Always provide retry, manual browse, and report-wrong-result actions.

## 9. Technical architecture and code optimization plan

### Target Android structure

- Kotlin-first, single-activity navigation with composable or consistently modern UI; do not mix patterns without a reason.
- `data/`: content records, local data source, media manifest, preferences, model metadata.
- `domain/`: learning, search, practice, recognition, and phrase use cases.
- `ui/`: navigation, screens, state holders/view models, reusable components.
- `camera/`: permission, analyzer, frame conversion, lifecycle, quality checks.
- `ml/`: model loader, preprocessor, inference engine, label map, threshold/calibration policy.
- `analytics/` only for privacy-preserving, consented operational events.

### Content architecture

Create a typed content record containing:

- Stable ID, display English, reviewed Gujarati, category, level/path, image/video references, caption/usage notes, supported recognition label, source/provenance, review status, and version.

Use one repository for all screens. Validate at build/test time:

- Every referenced asset exists.
- Every content item has required translations and metadata.
- No duplicate IDs or labels.
- Model labels match supported recognition records.
- No filenames with spaces/typos or resource-name coupling.

### Performance and reliability

- Run camera analysis and inference off the main thread.
- Throttle or analyze only the latest frame; close every `ImageProxy`.
- Reuse model/interpreter resources safely and release them with lifecycle ownership.
- Downsample/convert frames once and avoid unnecessary bitmap copies.
- Cache thumbnails and lazy-load media.
- Use structured error states instead of broad catches and transient Toast-only failures.
- Add baseline/profileable performance checks for startup, scroll, media playback, and recognition latency.

## 10. Recognition pipeline repair plan

### Choose one production model contract

Do not combine the current 28×28 pixel classifier and the 42-value MediaPipe landmark model accidentally. Select and document one model for the first release:

- input tensor shape/type/range;
- preprocessing and orientation/mirroring;
- label order and model version;
- output scores and confidence calibration;
- supported classes and explicit unsupported state;
- expected device latency and memory.

### End-to-end flow

Camera frame → rotation/mirror normalization → hand/quality detection → preprocessing → model inference → label/score mapping → threshold/rejection → temporal smoothing → UI state → optional user feedback.

### Minimum acceptance behavior

- Never return a hard-coded label.
- Never display a result when no valid hand or confidence threshold is met.
- Do not infer continuous sentences from a single-sign model.
- Test across held-out signers, backgrounds, lighting, phone models, distances, orientations, and unsupported gestures.
- Report accuracy, per-class recall, confusion matrix, rejection rate, p50/p95 latency, memory, and battery impact.
- Version the model and retain the exact evaluation artifact used for release.

## 11. Product features that serve the application

### Phase 1 — trustworthy learning product

- Curated categories and lessons instead of arbitrary difficulty grids.
- Search, filters, favorites, recently viewed, and offline-first content.
- Sign detail with image/video, English/Gujarati meaning, usage note, and related signs.
- Flashcards, multiple-choice quizzes, self-rating, spaced review, and local progress.
- Phrase cards for practical situations: greetings, school, travel, help, health disclaimer, shopping, and introductions.
- Settings for language, text size, contrast, captions, haptics, and privacy.

### Phase 2 — validated recognition assistance

- Single-sign practice mode with guided camera framing.
- Live or capture-based recognition only after the pipeline is correct.
- Confidence-aware results and uncertainty/retry states.
- Practice score based on supported classes, not a claim of language fluency.
- Opt-in wrong-result feedback stored locally unless the user explicitly consents to sharing.

### Phase 3 — communication workflows

- Custom phrase boards and pinned phrases.
- Text-to-speech for typed English/Gujarati where appropriate and clearly labeled as spoken-language output.
- Conversation-side visual cards, not automatic interpretation.
- Optional teacher/content authoring workflow.
- Carefully scoped multi-sign research prototype only after grammar, dataset, privacy, and evaluation work.

## 12. Repository and implementation work breakdown

### Documentation first

- Add a repository `PRODUCT-REVAMP.md` containing this audit in implementation-ready form.
- Add a truthful README with setup, architecture, supported signs, limitations, model evaluation, and screenshots.
- Rewrite report, presentation, proposal, research paper, About copy, and in-app terminology.
- Add a model card and dataset card with provenance, limitations, evaluation protocol, and license notes.

### Foundation implementation

- Replace hard-coded activity lists with typed content data.
- Introduce a single navigation/state approach.
- Build design tokens and reusable accessible components.
- Implement search, categories, favorites, progress, and offline persistence.
- Add content/media validation and automated tests.

### Recognition implementation

- Select/export the approved model.
- Add a deterministic preprocessing/inference test using known fixtures.
- Wire CameraX analyzer to the model, including frame closure and lifecycle.
- Add result-state UI and fallback to catalogue/practice.
- Add a reproducible evaluation command and model version metadata.

### Production hardening

- Configure release signing outside source control, minification/shrinking after verification, startup/performance profiling, crash reporting, privacy policy, permission rationale, accessibility tests, device tests, and staged rollout.
- Remove unused dependencies, dead Activities, demo controls, personal contact data, duplicate records, and misleading screenshots.

## 13. Validation and release gates

### Product gates

- A new user can understand the purpose without reading the report.
- Core learn and practice journeys work offline.
- Every visible control has a real outcome or is removed.
- No screen or document calls the app a full translator.
- Content is reviewed by an ISL-informed reviewer and Gujarati language reviewer.

### Engineering gates

- Debug and release builds succeed from a clean checkout.
- Unit tests cover content validation, label mapping, translation lookup, practice scoring, and recognition state transitions.
- UI tests cover onboarding, navigation, search, detail, practice, permission denial, and error recovery.
- Camera/model tests cover supported, unsupported, no-hand, low-confidence, rotation, and lifecycle cases.
- No fatal lint/build warnings introduced by the revamp.

### ML gates

- Exact dataset split and preprocessing are reproducible.
- Held-out signer evaluation is mandatory.
- Per-class metrics and rejection behavior are published.
- Real-device latency and memory meet an agreed target.
- Model output is never treated as certain when below threshold.

### Accessibility and privacy gates

- Screen-reader, contrast, text scaling, touch target, captions, and reduced-motion checks pass.
- Camera use is explicit, local by default, and explained.
- No personal data or camera frames are uploaded without consent.
- Logs exclude raw images and sensitive content.

### Release stages

1. Internal learning prototype.
2. Closed test with learners and communication partners.
3. Recognition beta for the documented sign set.
4. Public release of the learning/communication-support product.
5. Future research releases clearly separated from the stable product.

## 14. Suggested execution order

1. Freeze and document the current behavior; preserve evidence before deleting demo code.
2. Create `PRODUCT-REVAMP.md`, truthful README, model card, and corrected product language.
3. Remove hard-coded/fake controls and unsupported claims.
4. Build the content schema, validation, repository, navigation, and design system.
5. Deliver offline learning, search, practice, favorites, progress, and phrase cards.
6. Select and validate one recognition model; repair CameraX integration only after evaluation.
7. Add recognition UX with rejection and recovery states.
8. Run accessibility, usability, device, performance, privacy, and content reviews.
9. Update the report and presentation from measured product behavior.
10. Produce release artifacts, demo script, limitations statement, and staged deployment checklist.

## 15. Definition of success

SanketSetu is ready to be presented as a serious product when a new user can learn a sign, practice it, find it later, use a practical phrase card, and understand exactly what camera recognition can and cannot do. The build must be reliable offline, accessible, measurable, privacy-conscious, and honest in both the app and all academic material. Any capability that cannot be demonstrated, tested, and explained must be labeled as planned research or removed from the product story.

## 16. Evidence references

- `app/src/main/java/com/abhinay/ISLEnglishGujaratiApp/CameraActivity.kt`: hard-coded capture result and unused classifier path.
- `app/src/main/java/com/abhinay/ISLEnglishGujaratiApp/Classifier.kt`: incomplete/inconsistent TensorFlow Lite preprocessing and output handling.
- `app/src/main/java/com/abhinay/ISLEnglishGujaratiApp/BasicActivity.kt`, `IntermediateActivity.kt`, `AdvancedActivity.kt`: hard-coded catalogue lists and inconsistent identifiers.
- `app/src/main/java/com/abhinay/ISLEnglishGujaratiApp/GestureActivity.kt`: resource-name-driven media and fallback translation behavior.
- `app/src/main/java/com/abhinay/ISLEnglishGujaratiApp/TranslationData.kt`: large static translation map and duplicate advanced records.
- `app/src/main/res/layout/about_activity.xml`: claims of instant camera predictions and a complete offline guide.
- `model/train_model.py`, `model/ui.py`: separate MediaPipe/Random Forest research pipeline.
- `Project_Report.pdf`, `Presentation.pptx`, `Major Project Proposal.docx`, `Research Paper Major Project.pdf`: real-time translator, CNN/TFLite, accuracy, and communication claims requiring correction.

## Notes and constraints

- The selected direction is a **hybrid learning + communication app**, with both areas first-class but delivered in phases.
- Existing media and Gujarati text are inputs to curate and verify, not proof of correctness.
- Do not reuse reported accuracy as a product guarantee without reproducible held-out evaluation and real-device testing.
- Every current screen/control must ultimately be classified as **keep, repair, redesign, defer, or remove**.
- Do not implement unrestricted translation claims or hide uncertainty behind polished UI.
- This plan is the source of truth for the revamp; implementation should be tracked in SQL and validated against the release gates above.
