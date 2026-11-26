# Markdown Text Cleaner

Android app that flattens whitespace without removing Markdown syntax. Clean text from the Android share sheet, the text-selection menu, or by pasting into the app. Processing is local.

## Android development

The buildable source is in `android/`. The ZIP is a historical prototype export, not a build input. With JDK 17, Android SDK Platform 34, Build Tools 34.0.0, and Gradle 8.6:

```sh
cd android
gradle testDebugUnitTest assembleDebug
```

GitHub Actions runs the same checks and publishes the debug APK as a **test artifact**, not a phone release. The hosted runner's debug certificate is not a stable update identity.

## Updating a phone

For the same Android package, an in-place upgrade requires the original signing key and a non-decreasing version code. Increase the version code for every promoted update. If the prototype's signer is unavailable, keep the existing install until its owner explicitly chooses a migration; do not uninstall to work around a signature mismatch. Keep private signing keys out of the repository.

Existing paired Wireless Debugging can deliver a trusted APK when the phone and desktop can reach each other. Don't use ADB over an open TCP port, install arbitrary workflow artifacts, or enable debugging automatically. Deploy only a verified, locally approved APK to the intended device and check the installed version afterwards.
