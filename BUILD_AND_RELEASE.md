# Build and release checklist

## Debug
1. Open `QRScannerGenerator` in Android Studio.
2. Let Gradle Sync complete.
3. Connect an Android phone with USB debugging enabled.
4. Run the `app` debug configuration.
5. Confirm that the banner uses Google's test ad configuration.

## Physical-device QA
- Camera permission requested only from Scan QR.
- Denied camera permission shows a retry/settings path.
- Scanner starts and stops with the screen lifecycle.
- QR detection is restricted to QR codes and does not repeatedly save the same live result.
- Torch turns on/off only while the camera is active.
- Gallery QR scan works without storage permission.
- HTTP/HTTPS URLs can be opened; other schemes are not exposed through the website button.
- Text copy/share works.
- All QR generator types produce readable codes.
- Website, email and location inputs validate before generation.
- Wi-Fi escaping handles special characters.
- Generated PNG can be saved through the Android document picker.
- Generated PNG can be shared through FileProvider.
- History survives app restart and is capped at 500 records.
- Individual delete and clear-all confirmation work.
- Theme selection persists.
- Rotation/configuration changes do not crash the app.
- Offline scanning and generation work after installation.
- No unnecessary permissions appear in the manifest.

## Release
1. Create a private signing keystore.
2. Copy `signing.properties.example` to `signing.properties`.
3. Fill in the keystore path/passwords locally. Do not commit them.
4. Build a signed APK for direct distribution.
5. Build a signed AAB for stores requiring AAB.
6. Replace any placeholder store screenshots with real device screenshots.
7. Verify the production AdMob App ID and banner unit in the release variant.
8. Never click your own production ads during testing.

## Production ad policy
This app deliberately contains only banner ads on Home and Create QR screens. The scanner has no ad overlay, and no interstitial/rewarded/app-open ads are included.
