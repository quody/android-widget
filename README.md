# Nimipäivät — Finnish Name Days Widget

A minimal Android home screen widget that shows today's Finnish name days. Glance at your home screen, see whose nimipäivä it is, congratulate them.

Built with Jetpack Glance. Runs entirely offline. Zero permissions. Zero data collection.

## Prerequisites

- **JDK 17** — download from [Adoptium](https://adoptium.net/) or install via your package manager
- **Android SDK** — either install [Android Studio](https://developer.android.com/studio) (which bundles the SDK) or install the [command-line tools](https://developer.android.com/studio#command-line-tools-only) standalone
- **An Android phone** running Android 8.0 (API 26) or higher, with a USB cable

### Install JDK 17

<details>
<summary><strong>macOS</strong></summary>

```bash
brew install openjdk@17
```

Or download the `.pkg` installer from [Adoptium](https://adoptium.net/).
</details>

<details>
<summary><strong>Linux</strong></summary>

```bash
# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# Fedora
sudo dnf install java-17-openjdk-devel
```
</details>

<details>
<summary><strong>Windows</strong></summary>

1. Download the JDK 17 `.msi` installer from [Adoptium](https://adoptium.net/)
2. Run the installer — check **Set JAVA_HOME variable** when prompted
3. Open a new terminal and verify:
   ```cmd
   java -version
   ```
</details>

Verify on any platform: `java -version` should show `17.x`.

### Install Android SDK

The easiest path is to install [Android Studio](https://developer.android.com/studio), which bundles the SDK, `adb`, and an emulator. If you prefer a minimal install without the IDE, expand the section for your OS below.

<details>
<summary><strong>macOS / Linux — command-line tools only</strong></summary>

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
</details>

<details>
<summary><strong>Windows — command-line tools only</strong></summary>

1. Download the **Command line tools only** zip from https://developer.android.com/studio#command-line-tools-only
2. Create a folder, e.g. `C:\Android\sdk`
3. Unzip the download and move its contents so you have `C:\Android\sdk\cmdline-tools\latest\bin\sdkmanager.bat`
4. Open **Command Prompt** (or PowerShell) and run:
   ```cmd
   set ANDROID_HOME=C:\Android\sdk
   %ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager --licenses
   %ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
   ```
5. Add permanent environment variables via **Settings > System > About > Advanced system settings > Environment Variables**:
   - Set `ANDROID_HOME` to `C:\Android\sdk`
   - Add `C:\Android\sdk\platform-tools` to your `Path`
</details>

If you use Android Studio, the SDK is typically at `~/Android/Sdk` (Linux), `~/Library/Android/sdk` (macOS), or `%LOCALAPPDATA%\Android\Sdk` (Windows). Set `ANDROID_HOME` accordingly.

## Build

Clone the repo and build the debug APK. Gradle will automatically download all dependencies (Kotlin, Jetpack Glance, Gson, etc.) on the first run.

**macOS / Linux:**

```bash
git clone <repo-url> nimipaivat
cd nimipaivat
./gradlew assembleDebug
```

**Windows (Command Prompt):**

```cmd
git clone <repo-url> nimipaivat
cd nimipaivat
gradlew.bat assembleDebug
```

The first build takes a few minutes as Gradle downloads ~300 MB of dependencies and caches them in `~/.gradle/` (or `%USERPROFILE%\.gradle\` on Windows). Subsequent builds are fast.

The output APK is at:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Run tests

```bash
./gradlew testDebugUnitTest        # macOS / Linux
gradlew.bat testDebugUnitTest      # Windows
```

### Running from Android Studio

This is a widget-only app — it has no launcher activity. If you click **Run** in Android Studio, you'll get a **"Default Activity not found"** error. To fix this:

1. Go to **Run > Edit Configurations...**
2. Select the **app** configuration
3. Under **Launch Options**, change **Launch** from "Default Activity" to **Nothing**
4. Click **OK** and run again

The app will install silently. Add the widget to your home screen afterwards (see step 4 below).

## Install on an Android phone

### 1. Enable Developer Options on your phone

1. Open **Settings > About phone**
2. Tap **Build number** 7 times until you see "You are now a developer"
3. Go back to **Settings > System > Developer options**
4. Enable **USB debugging**

### 2. Connect your phone

Plug your phone into your computer via USB. When prompted on the phone, tap **Allow** to authorize USB debugging.

**Windows driver note:** Most phones need a USB driver on Windows. Samsung devices need [Samsung USB Driver](https://developer.samsung.com/android-usb-driver). Google Pixel works with the [Google USB Driver](https://developer.android.com/studio/run/win-usb) (installable via SDK Manager). Other manufacturers usually provide drivers on their support sites.

Verify the connection:

```bash
adb devices
```

You should see your device listed (e.g. `XXXXXXXXX  device`). If it says `unauthorized`, check for the authorization prompt on your phone.

### 3. Install the APK

**Option A — Build and install in one step:**

```bash
./gradlew installDebug              # macOS / Linux
gradlew.bat installDebug            # Windows
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
