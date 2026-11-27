package := "com.symphonysimper.launcher"
home := package + "/.MainActivity"

studio_jbr := `for d in "$(grep -o '/nix/store/[a-z0-9]*-android-studio-unwrapped[^/" ]*' "$(readlink -f "$(command -v android-studio)" 2>/dev/null)" 2>/dev/null | head -1)/jbr" /opt/android-studio/jbr "$HOME/android-studio/jbr" "/Applications/Android Studio.app/Contents/jbr/Contents/Home"; do [ -x "$d/bin/java" ] && echo "$d" && break; done; true`
export JAVA_HOME := env("JAVA_HOME", studio_jbr)

keep_installed := "-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true"

[private]
default:
    @just --list --unsorted

# Build the optimised release APK
build:
    ./gradlew :app:assembleRelease

# Install the release build on the connected phone (judge speed and smoothness on this one)
install:
    ./gradlew :app:installRelease

# Install the debug build
debug:
    ./gradlew :app:installDebug

# Publish a GitHub release of main as it is on GitHub
[confirm("Publish a release of origin/main?")]
release:
    git fetch -q origin main
    git push origin origin/main:refs/heads/release
    @echo "The Release workflow is building it: gh run list --workflow release.yml"

# Save the launcher's settings from the phone to a file
backup file="launcher-settings.json":
    adb shell input keyevent KEYCODE_HOME
    @sleep 1
    adb shell dumpsys activity {{home}} export-settings | sed -n '/^{/,/^}/p' > {{file}}
    @echo "Saved to {{file}}"

# Put the launcher's settings back on the phone from a file
restore file="launcher-settings.json":
    adb shell input keyevent KEYCODE_HOME
    @sleep 1
    adb shell dumpsys activity {{home}} import-settings "$(base64 -w0 {{file}})"

# Show the launcher's memory and frame stats
stats:
    adb shell dumpsys meminfo {{package}} | sed -n '/App Summary/,/TOTAL SWAP/p'
    adb shell dumpsys gfxinfo {{package}} | sed -n '/Total frames/,/Number Frame deadline missed/p'

# Follow the launcher's log
log:
    adb logcat --pid="$(adb shell pidof {{package}})"

# Regenerate the baseline profile; settings are backed up first and restored after
profile: (backup "build/profile-backup.json")
    ./gradlew :app:generateBaselineProfile {{keep_installed}}
    just restore build/profile-backup.json
    @echo "Set the wallpaper and default home app again if they were reset."

# Run the startup and scrolling benchmarks; settings are backed up first and restored after
bench: (backup "build/bench-backup.json")
    ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest {{keep_installed}} \
        -Pandroid.testInstrumentationRunnerArguments.class={{package}}.baselineprofile.LauncherBenchmarks
    just restore build/bench-backup.json
