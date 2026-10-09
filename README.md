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
- No login, no backend and no QR-content upload

## Stable application ID
`com.aakash.qrscanner`

## Open in Android Studio
Open the `QRScannerGenerator` directory in Android Studio and allow Gradle Sync to resolve the declared dependencies.

The supplied environment did not contain the Android SDK/Gradle distribution cache, so the project could not be compiled here. Android Studio should perform the dependency resolution/build on the laptop.

## Release signing
Copy `signing.properties.example` to `signing.properties`, fill in your private keystore values, and build a signed release APK/AAB. Never commit the keystore or `signing.properties`.

## Monetization

Version 2 development builds are ad-free. Advertising will only be reconsidered after core features, privacy behavior, and release quality are complete.


## QA before publishing
Test on a physical Android phone: camera permission, camera start/stop, torch, repeated scans, gallery scanning, all generator types, invalid inputs, save/share, history persistence, rotation, dark/light/system theme, back navigation, offline QR operations, and both debug/release ad behavior.
