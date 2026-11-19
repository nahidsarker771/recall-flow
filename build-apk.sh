#!/usr/bin/env bash
# RecallFlow — versioned APK builder (Gradle).
#
# Usage:
#   ./build-apk.sh          -> build VERSION, write ../apk/RecallFlow-v<ver>.apk, bump VERSION
#   ./build-apk.sh 1.0      -> build explicitly as 1.0
set -euo pipefail
cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

APP_NAME="RecallFlow"
OUT_DIR="$(cd .. && pwd)/apk"
VER_FILE="$PWD/VERSION"
[ -f "$VER_FILE" ] || echo "1.0" > "$VER_FILE"
CUR="$(tr -d '[:space:]' < "$VER_FILE")"
VER="${1:-$CUR}"
IFS='.' read -ra PARTS <<< "$VER"
MAJ="${PARTS[0]:-1}"; MIN="${PARTS[1]:-0}"; PAT="${PARTS[2]:-0}"
VC=$(( MAJ * 10000 + MIN * 100 + PAT ))
LAST=$(( ${#PARTS[@]} - 1 )); PARTS[$LAST]=$(( PARTS[$LAST] + 1 ))
NEXT="$(IFS=.; echo "${PARTS[*]}")"

[ -x "$JAVA_HOME/bin/java" ] || { echo "ERROR: JDK 17 not found at $JAVA_HOME"; exit 1; }
[ -d "$ANDROID_HOME/platforms/android-34" ] || { echo "ERROR: Android SDK 34 not found at $ANDROID_HOME"; exit 1; }
[ -f "$PWD/keystore.properties" ] || { echo "ERROR: keystore.properties missing"; exit 1; }
[ -f "$PWD/studyretention.jks" ] || { echo "ERROR: studyretention.jks missing"; exit 1; }

echo "==> Building $APP_NAME v$VER (versionCode $VC)"
echo "$VER" > "$VER_FILE"
mkdir -p "$OUT_DIR"

./gradlew --no-daemon assembleRelease

APK="app/build/outputs/apk/release/app-release.apk"
[ -f "$APK" ] || { echo "ERROR: build succeeded but $APK is missing"; exit 1; }
cp -f "$APK" "$OUT_DIR/$APP_NAME-v$VER.apk"

rm -rf .work
echo "$NEXT" > "$VER_FILE"
echo ""
echo "==> Done!"
echo "    APK : $OUT_DIR/$APP_NAME-v$VER.apk"
echo "    Next build will be v$NEXT"
