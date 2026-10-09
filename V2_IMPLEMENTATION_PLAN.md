# QR Scanner & Generator v2.0 — Implementation Plan

## Release invariants
- Keep application ID `com.aakash.qrscanner`.
- Keep the existing release signing keystore and alias. Never commit signing secrets.
- Upgrade `versionCode` from 1 to 2 and `versionName` from 1.0.0 to 2.0.0 only when the v2 release candidate is ready.
- Preserve existing Room history and user settings; use additive database migrations only.
- Keep core scan/generate/history features available offline and keep QR contents on-device.
- Keep only the existing small banner ad placement. No app-open, interstitial, or rewarded ads.
- Avoid new permissions unless a feature demonstrably requires them.

## Workstreams

### P0 — Release safety and architecture
- Create a dedicated v2 branch and keep master/v1 releasable.
- Audit scanner, generator, history schema, navigation, permissions, and ad placement before refactoring.
- Add unit tests for payload parsing, barcode validation, URL safety, and generator outputs.
- Confirm existing Room schema is preserved and plan migrations before adding fields.

### P1 — Universal scanning
- Keep bundled ML Kit barcode scanning for offline operation.
- Explicitly configure supported QR and 1D/2D formats; cover QR, EAN-8, EAN-13, UPC-A, UPC-E, Code 128, Code 39, Code 93, Codabar, ITF, PDF417, Aztec, Data Matrix.
- Add continuous/batch scan mode with pause/resume and duplicate suppression.
- Add camera zoom controls, focus/torch state, haptic feedback, and clear empty/error states.
- Add gallery scanning using bounded image decoding to avoid excessive memory use.
- Parse results by barcode format and content type, with safe URL preview before opening.

### P2 — Barcode generation
- Add a distinct Barcode tab/mode alongside QR generation.
- Support Code 128 and Code 39 for general alphanumeric labels; EAN-13, EAN-8, UPC-A, UPC-E, ITF and Codabar only with strict format validation.
- Validate lengths, numeric requirements and check digits before encoding.
- Use proper quiet zones, sufficient module width, high-contrast output, and human-readable text where appropriate.
- Clearly label formats that require GS1 membership, assigned identifiers, or industry-specific compliance; do not imply arbitrary codes are valid retail identifiers.
- Add printable label layouts and export/share. SVG export must be implemented as vector output, not a renamed PNG.

### P3 — QR design studio
- Support foreground/background colours, selected dot/corner styles, optional centre logo and frames.
- Maintain error correction appropriate to logo use and preserve quiet zones.
- Render off the main thread and verify generated QR codes can be decoded before export where practical.
- Export PNG and true SVG; add image-size limits and validation.
- Provide reusable templates for links, Wi-Fi, contact cards, business cards and menus.

### P4 — Saved library
- Add search, filters, favourites, folders, rename, multi-select, and bulk actions.
- Add versioned JSON export/import with schema validation, size limits and safe error handling.
- Preserve old history with Room migrations; do not clear user data during upgrades.
- Optional app lock must use Android biometric/device credential APIs and must not store biometric data.

### P5 — Smart formats and safety
- Improve URL canonicalization and block unsafe schemes from automatic opening.
- Add previews for URLs, email, telephone, SMS, Wi-Fi, contacts, geo, calendar and supported payment links.
- Treat decoded QR/barcode text as untrusted data. Never auto-run, auto-dial, or silently join Wi-Fi.
- Product lookup is optional and must disclose that it sends the barcode value to an external provider. Core scanning must not require network access.

### P6 — Quality, accessibility and release
- Test permission denied/permanently denied, process recreation, rotation, lifecycle, no-camera, low-memory and large-image cases.
- Add tests for every generator format, check digit validation, history migrations, import/export and unsafe links.
- Run debug/release builds, lint, unit tests, APK signature verification, 16 KB alignment, APK/AAB package/version checks and install/upgrade tests on emulator and a physical device.
- Update privacy policy, store listing, screenshots and release notes to match actual shipped behavior.
- Do not publish until critical tests pass.

## Initial v2 acceptance checklist
- [ ] Existing v1 install can update in place with same signing key and retains history.
- [ ] All supported QR and barcode formats are documented and tested.
- [ ] Invalid barcode lengths/check digits are rejected with actionable errors.
- [ ] Gallery scan handles large images without out-of-memory crashes.
- [ ] Batch scan has duplicate suppression and can pause/resume.
- [ ] QR customisation never removes required quiet zone or silently lowers readability.
- [ ] Export/import is versioned, bounded, and validated.
- [ ] No new unnecessary Android permissions or network uploads of QR history.
- [ ] Banner-only advertising policy remains intact.
- [ ] Release build, lint, tests, signature, alignment and upgrade path all pass.
