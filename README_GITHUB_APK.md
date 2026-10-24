# BioSecure File Manager — GitHub APK build

## Build an APK on GitHub
1. Create a GitHub repository and upload/extract the contents of this project at the repository root.
2. Open the **Actions** tab and enable workflows if GitHub asks.
3. Select **Build Android APK** and choose **Run workflow**, or push to `main`/`master`.
4. When the run succeeds, open the workflow run and download `biosecure-file-manager-debug-apk` from **Artifacts**.

This workflow creates a **debug APK** for testing and installation. It is not a Play Store release-signed APK. Do not publish a debug APK as a production release.

The project is Kotlin/Jetpack Compose and requires a Gradle/Android SDK build environment. It is not a Java/XML-only App Builder project.
