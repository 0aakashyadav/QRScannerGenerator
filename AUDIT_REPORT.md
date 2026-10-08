# QR Scanner & Generator — Source Audit

Date: October 7, 2026

## Fixed in this audit

1. **Camera analyzer lifecycle** — scanner and analysis executor are now tied to the Compose lifecycle and cleaned up when the scanner screen leaves composition.
2. **Repeated live detections** — a single scan result is accepted per scanner session, preventing rapid duplicate history entries.
3. **Torch state** — torch is disabled when leaving the scanner result flow and only enabled while a camera is active.
4. **Gallery scanning** — uses the Android system picker and closes the temporary ML Kit scanner after completion.
5. **Input validation** — website, email and location inputs are validated before QR generation.
6. **Wi-Fi generation** — supports WPA, WEP and open networks with QR escaping for special characters and hidden-network state.
7. **vCard generation** — required name is enforced and special characters/newlines are escaped.
8. **QR encoding** — UTF-8 character set and bounded output size are explicitly configured; encoding failures are surfaced to the user.
9. **Image saving** — verifies the destination stream and PNG compression result instead of silently reporting success.
10. **Image sharing** — uses a unique cache filename and FileProvider, with failure handling when no sharing app exists.
11. **URL opening** — only exposes the website action for HTTP/HTTPS URLs and handles missing browsers.
12. **History safety** — clear-all now requires confirmation; history is capped at 500 records to avoid unbounded local growth.
13. **Theme persistence** — System/Light/Dark preference is stored in DataStore.
14. **Ad initialization** — ads are not loaded until the consent flow has completed and Mobile Ads has initialized; the scanner itself contains no banner.
15. **Ad test separation** — debug uses Google's official test IDs; release uses the supplied production IDs.
16. **Privacy choices** — UMP privacy options are exposed in Settings when Google requires them.
17. **Permissions** — only CAMERA and INTERNET are declared; gallery selection does not require storage permission.
18. **Release signing** — private signing material remains external to the repository.
19. **Documentation** — added privacy policy, build/release checklist, and this audit report.

## Requirements coverage

- Camera QR scan: implemented
- Automatic detection: implemented
- Flashlight: implemented
- Start/stop lifecycle: implemented
- Gallery scan: implemented
- Result display/copy/share: implemented
- Safe HTTP/HTTPS opening: implemented
- Common QR types: implemented through ML Kit classification and generator forms
- Plain text/URL/Wi-Fi/phone/email/SMS/contact/location generation: implemented
- Save/share generated QR: implemented
- Offline QR generation: implemented
- Local scan/generated history: implemented
- Individual delete/clear all: implemented
- No account/backend: implemented
- Light/dark/system theme: implemented
- Responsive Compose layouts: implemented
- Back navigation: implemented for modal workflows
- Accessibility basics: content descriptions and labeled controls included
- Minimal banner advertising: implemented outside scanner
- No interstitial/rewarded/app-open ads: intentionally not implemented

## Environment limitation

A full Gradle build could not be executed in the provided environment because the Android SDK and Gradle distribution/dependency caches were not available and outbound package download was unavailable. Source files were inspected and Kotlin parsing was attempted; unresolved Android/AndroidX references are expected outside an Android Gradle environment.

The remaining validation step is therefore a real Android Studio build followed by physical-device QA. This is not represented as completed in this report.
