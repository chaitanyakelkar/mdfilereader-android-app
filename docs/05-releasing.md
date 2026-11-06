# 5. Releasing

Commands are for PowerShell, run from the project folder.

## One-time setup

### 1. Create the signing key

Run this outside the project folder, and enter a strong password when asked:

```powershell
keytool -genkeypair -v -keystore mdreader-release.jks -alias mdreader -keyalg RSA -keysize 4096 -validity 10000
```

Back up the `.jks` file and its password, for example in a password manager. Updates must be
signed with the same key, so if it's lost, people have to uninstall the app to get new versions.

### 2. Point the build at the key

Copy `keystore.properties.example` to `keystore.properties` and fill it in. Git ignores
`keystore.properties`. Never commit it or the key.

Automated builds can set these environment variables instead: `MDREADER_STORE_FILE`,
`MDREADER_STORE_PASSWORD`, `MDREADER_KEY_ALIAS` and `MDREADER_KEY_PASSWORD`.

## Each release

### 1. Set the version

In `app/build.gradle.kts`, raise `versionCode` by at least 1 and update `versionName`. Commit
this change to `main` through a pull request.

### 2. Test and build

```powershell
./gradlew.bat testDebugUnitTest connectedDebugAndroidTest lintDebug assembleRelease
```

The tests need a connected device or emulator. The signed APK is at
`app/build/outputs/apk/release/app-release.apk`. If the file is named
`app-release-unsigned.apk`, the build didn't find your key.

### 3. Check the APK

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --print-certs app\build\outputs\apk\release\app-release.apk
```

The certificate shouldn't say `Android Debug`. Then install the APK on a real phone, and check
these before publishing:

- Open a file with **Open file**, and from a file manager with **Open with**
- Reopen a recent file after closing the app
- Check a table, a code block and an image

An APK signed with a different key can't install over an existing copy, so uninstall any test
build first.

### 4. Publish on GitHub

```powershell
Copy-Item app\build\outputs\apk\release\app-release.apk MDReader.apk
```

```powershell
gh release create v1.0.0 MDReader.apk --target main --title "MD Reader 1.0.0" --generate-notes
```

Git ignores APK files, so the copy won't be committed by accident.

## Play Store

Play requires an app bundle, built with `./gradlew.bat bundleRelease`. The output is at
`app/build/outputs/bundle/release/app-release.aab`. Check the current requirements in Play
Console, including any closed testing rules for new personal accounts.
