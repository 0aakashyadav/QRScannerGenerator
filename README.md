# QR Scanner & Generator

A privacy-focused native Android QR utility app.

## Included features
- Camera QR scanning with automatic detection and flashlight
- Gallery/image QR scanning without storage permission
- Plain text, website, Wi-Fi, phone, email, SMS, vCard/contact and location QR generation
- High-resolution QR PNG saving and Android Sharesheet sharing
- Local Room history for scanned and generated QR codes
- Copy/share/delete/clear history
- Light/dark/system theme preference persisted locally
- Minimal banner ads outside the camera scanner
- Google UMP consent flow and privacy choices where required
- Debug builds use Google's official test AdMob IDs
- Release builds use the supplied production AdMob IDs
- No login, no backend and no QR-content upload

## Stable application ID
`com.aakash.qrscanner`

## Open in Android Studio
Open the `QRScannerGenerator` directory in Android Studio and allow Gradle Sync to resolve the declared dependencies.

The supplied environment did not contain the Android SDK/Gradle distribution cache, so the project could not be compiled here. Android Studio should perform the dependency resolution/build on the laptop.

## Release signing
Copy `signing.properties.example` to `signing.properties`, fill in your private keystore values, and build a signed release APK/AAB. Never commit the keystore or `signing.properties`.

## Ad configuration
- Debug: Google's official test App ID and test banner unit.
- Release: the production App ID and banner unit supplied for this project.
- No interstitial, rewarded, rewarded-interstitial or app-open ads are implemented.

## QA before publishing
Test on a physical Android phone: camera permission, camera start/stop, torch, repeated scans, gallery scanning, all generator types, invalid inputs, save/share, history persistence, rotation, dark/light/system theme, back navigation, offline QR operations, and both debug/release ad behavior.
