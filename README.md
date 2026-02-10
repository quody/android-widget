# Nimipäivät — Finnish Name Days Widget

A minimal Android home screen widget that shows today's Finnish name days. Glance at your home screen, see whose nimipäivä it is, congratulate them.

Built with Jetpack Glance. Runs entirely offline. Zero permissions. Zero data collection.

## Prerequisites

- **JDK 17** — download from [Adoptium](https://adoptium.net/) or install via your package manager
- **Android SDK** — either install [Android Studio](https://developer.android.com/studio) (which bundles the SDK) or install the [command-line tools](https://developer.android.com/studio#command-line-tools-only) standalone
- **An Android phone** running Android 8.0 (API 26) or higher, with a USB cable

### Install JDK 17

```bash
# macOS (Homebrew)
brew install openjdk@17

# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# Fedora
sudo dnf install java-17-openjdk-devel

# Verify
java -version   # should show 17.x
```

### Install Android SDK (command-line only, no Android Studio)

If you don't want to install Android Studio, you can set up the SDK manually:

```bash
# 1. Download command-line tools from https://developer.android.com/studio#command-line-tools-only
# 2. Unzip to a directory, e.g. ~/android-sdk

export ANDROID_HOME=~/android-sdk
mkdir -p "$ANDROID_HOME/cmdline-tools"
# Move the unzipped 'cmdline-tools' contents into 'latest':
mv cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"

# 3. Accept licenses and install required SDK components
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
    "platforms;android-35" \
    "build-tools;35.0.0" \
    "platform-tools"

# 4. Add to your shell profile (~/.bashrc, ~/.zshrc, etc.)
export ANDROID_HOME=~/android-sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

If you use Android Studio, the SDK is typically at `~/Android/Sdk` (Linux), `~/Library/Android/sdk` (macOS), or `%LOCALAPPDATA%\Android\Sdk` (Windows). Set `ANDROID_HOME` accordingly.

## Build

Clone the repo and build the debug APK. Gradle will automatically download all dependencies (Kotlin, Jetpack Glance, Gson, etc.) on the first run.

```bash
git clone <repo-url> nimipaivat
cd nimipaivat

# Build debug APK (downloads all dependencies automatically)
./gradlew assembleDebug
```

The first build takes a few minutes as Gradle downloads ~300 MB of dependencies and caches them in `~/.gradle/`. Subsequent builds are fast.

The output APK is at:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Run tests

```bash
./gradlew testDebugUnitTest
```

## Install on an Android phone

### 1. Enable Developer Options on your phone

1. Open **Settings > About phone**
2. Tap **Build number** 7 times until you see "You are now a developer"
3. Go back to **Settings > System > Developer options**
4. Enable **USB debugging**

### 2. Connect your phone

Plug your phone into your computer via USB. When prompted on the phone, tap **Allow** to authorize USB debugging.

Verify the connection:

```bash
adb devices
```

You should see your device listed (e.g. `XXXXXXXXX  device`). If it says `unauthorized`, check for the authorization prompt on your phone.

### 3. Install the APK

**Option A — Build and install in one step:**

```bash
./gradlew installDebug
```

This builds the APK and installs it directly onto the connected device.

**Option B — Install a pre-built APK:**

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 4. Add the widget to your home screen

The app has no launcher icon (it's a widget-only app). To use it:

1. Long-press on an empty area of your home screen
2. Tap **Widgets**
3. Find **Nimipäivät** in the widget list
4. Drag it onto your home screen
5. Choose your language (Finnish or Swedish-Finnish) in the configuration screen
6. Tap **Tallenna** (Save)

The widget displays today's name days and updates automatically at midnight. Tap the widget to force a refresh.

### Widget sizes

Resize the widget by long-pressing it and dragging the handles:

| Size | Content |
|------|---------|
| Small (2x1) | Date + today's names |
| Medium (3x2) | + tomorrow's preview |
| Large (4x2) | + day of week, week number |

## Install from CI

If you don't want to build locally, download the debug APK from GitHub Actions:

1. Push code to GitHub
2. Go to the **Actions** tab in the repository
3. Open the latest **Build & Test** workflow run
4. Download the **nimipaivat-debug** artifact
5. Unzip it to get `app-debug.apk`
6. Transfer to your phone and install:
   ```bash
   adb install app-debug.apk
   ```

## Project structure

```
app/src/main/
├── assets/namedays.json            # 366 days of Finnish + Swedish name days
├── java/com/example/nimipaivat/
│   ├── data/
│   │   ├── NameDayRepository.kt    # Reads bundled JSON
│   │   └── model/NameDay.kt        # Data class
│   ├── widget/
│   │   ├── NimipaivatWidget.kt     # GlanceAppWidget (3 responsive sizes)
│   │   ├── NimipaivatWidgetReceiver.kt
│   │   ├── WidgetContent.kt        # Glance composables
│   │   ├── WidgetConfigActivity.kt # Language toggle
│   │   ├── WidgetPreferences.kt    # DataStore prefs
│   │   ├── RefreshAction.kt        # Tap-to-refresh
│   │   └── BootReceiver.kt         # Post-reboot update
│   └── util/
│       └── DateUtils.kt            # Finnish date formatting
└── res/
    └── xml/nimipaivat_widget_info.xml
```

## Tech stack

- Kotlin 2.0, Jetpack Glance 1.1, Material You theming
- Min SDK 26 (Android 8.0), Target SDK 35 (Android 15)
- Fully offline — no network permission, no analytics, no ads
