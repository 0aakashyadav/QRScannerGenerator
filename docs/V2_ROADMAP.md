# QR Scanner & Generator v2.0 Roadmap

## Goals
Deliver a redesigned, reliable, privacy-first Android QR and barcode toolkit while preserving upgrade compatibility with v1.

## Non-negotiable compatibility and privacy
- Keep application ID `com.aakash.qrscanner`.
- Preserve the existing release signing key; never commit keystores or signing.properties.
- Upgrade versionCode to 2 and versionName to 2.0.0 only for the v2 release candidate.
- Preserve existing Room/DataStore data; use explicit, tested Room migrations and never destructive migration for user history.
- Offline-first; no mandatory account, no QR upload, no backend requirement for core functions.
- Ask permissions only when the user invokes a feature that needs them.
- Keep ads to a small banner only; no app-open, interstitial, or rewarded ads.

## New UI direction
- Material 3 visual redesign with a clear Scan / Create / Saved / Tools navigation model.
- Home dashboard with prominent Scan and Create actions, recent items, favourites, and quick tools.
- Dedicated live scanner screen with viewfinder, zoom, torch, pause/resume, format hint, haptics toggle, and clear result feedback.
- Structured result screen that identifies content type and shows safe, relevant actions.
- QR Studio with live preview, templates, style controls, export and share.
- Responsive layouts, dark/light/system themes, accessibility semantics, large text support, empty/loading/error states.

## Scanning and decoding
- Camera scanning for all formats supported by the selected ML Kit model: QR, Aztec, Data Matrix, PDF417, Code 128/39/93, Codabar, EAN-8/13, ITF, UPC-A/E.
- Evaluate a ZXing fallback for formats not supported by ML Kit (for example MaxiCode and RSS variants), using a documented capability matrix and tests; do not claim unsupported formats.
- Scan from gallery images and imported image files with bounds/memory safeguards.
- Batch/multi-code scanning where decoder output supports it, scan-region controls, zoom, torch, pause/resume, duplicate suppression, haptics and optional beep.
- Manual input/paste decoder path and clear unsupported/invalid-code states.
- Camera lifecycle, orientation, low-light and focus handling; performance profiling on low-memory devices.

## QR and barcode creation
- QR content types: plain text, URL, Wi-Fi, phone, SMS, email, vCard/contact, geo/location, calendar event, and compatible payment URLs.
- 1D barcode creation: Code 128, Code 39, EAN-8/13, UPC-A/E, ITF and Codabar only where valid input rules and library support are implemented.
- QR Studio: foreground/background colors, supported module/eye styles, frames/captions, logo overlay with quiet-zone and contrast guidance, templates, error-correction selection where supported, high-resolution PNG and SVG export where library support permits.
- Always generate a plain fallback and validate styled outputs by decoding them before claiming success.
- Bulk generation from pasted rows/CSV, batch naming, printable sheets and CSV export.

## Saved library and productivity
- Search, filter, sort, favourites, tags/collections, rename generated items, deduplicate, individual and bulk delete.
- Export/import a versioned backup with validation, size limits and safe failure recovery.
- Preserve existing v1 history through tested Room migrations.
- Optional app lock using Android device authentication; protect secrets and do not expose Wi-Fi passwords in logs.

## Result actions and safety
- Safe URL normalization and preview; explicit user confirmation before opening external links.
- Warn on suspicious schemes and malformed URLs; never automatically execute scanned content.
- Copy/share text or QR image, open appropriate intents, add contact/calendar only after user confirmation.
- Treat payment QR codes as encoded data/URLs, not proof that a payment is legitimate.
- Product lookup is optional and must clearly disclose any external service and data sent; basic barcode scanning remains offline.

## Quality gates
- Unit tests for content encoders, input validation, URL safety, migrations, and barcode-format mapping.
- Instrumentation/UI tests for navigation, permissions, scanner states, generation, history, themes and backup/restore.
- Emulator smoke tests plus real-device camera tests; generated-code round-trip tests for each supported format.
- Run `:app:lintRelease`, `:app:testDebugUnitTest`, `:app:assembleRelease`, and `:app:bundleRelease`.
- Verify APK signature, 16 KB alignment, manifest permissions, release AdMob IDs, package/version, R8 behavior and SHA-256.
- Do not publish a release until tests pass and v1-to-v2 update/data preservation is verified.

## Phased implementation
1. Audit existing source, dependencies, database schema, navigation, permissions and tests; record a v1 baseline.
2. Implement the new navigation/design system and result-type model without breaking existing flows.
3. Add decoder capability matrix, scanner improvements and safe result actions.
4. Add QR Studio, barcode generation, bulk tools and export.
5. Add searchable collections, backup/restore and migration tests.
6. Harden, test on emulator and physical device, build signed APK/AAB, and prepare release notes.

## Explicit limitations
No single library guarantees every barcode symbology or every proprietary/industry-specific format. Support must be stated per verified decoder/encoder and validated with real sample codes. Product names/prices require an external database and are not inferable from a barcode alone. Dynamic/trackable QR analytics require a server and are excluded from the offline-first core.