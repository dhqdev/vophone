#!/usr/bin/env bash
#
# Builds the vophone ROM: LineageOS for one phone model, with vophone built in as the
# home screen and phone app (see rom/vendor/vophone).
#
# Usage:
#   rom/build-rom.sh <lineage-source-dir> <device-codename>
# Example:
#   rom/build-rom.sh ~/android/lineage a52q
#
# Before running it, the LineageOS source must already be synced and the device's
# proprietary files extracted, exactly as in the "Build for <codename>" page of
# https://wiki.lineageos.org/devices (see rom/README.md).
#
# Set VOPHONE_APK=/path/to/vophone.apk to use an APK you already have instead of building it.

set -euo pipefail

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <lineage-source-dir> <device-codename>" >&2
    exit 1
fi

SRC="$(cd "$1" && pwd)"
DEVICE="$2"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VENDOR_SRC="$ROOT/rom/vendor/vophone"
VENDOR_DST="$SRC/vendor/vophone"

if [ ! -f "$SRC/build/envsetup.sh" ]; then
    echo "$SRC does not look like a LineageOS source tree (build/envsetup.sh not found)." >&2
    exit 1
fi

# 1. vophone APK
if [ -n "${VOPHONE_APK:-}" ]; then
    APK="$VOPHONE_APK"
else
    echo ">> Building vophone"
    (cd "$ROOT" && ./gradlew assembleFDroidDebug)
    APK="$ROOT/app/build/outputs/apk/fDroid/debug/app-fDroid-debug.apk"
fi
cp "$APK" "$VENDOR_SRC/prebuilt/vophone.apk"

# 2. Copy vendor/vophone into the source tree (the Android build does not follow symlinks)
echo ">> Copying vendor/vophone into $SRC"
mkdir -p "$SRC/vendor"
rm -rf "$VENDOR_DST"
cp -a "$VENDOR_SRC" "$VENDOR_DST"

cd "$SRC"
set +u
# shellcheck disable=SC1091
source build/envsetup.sh
echo ">> Preparing $DEVICE"
breakfast "$DEVICE"
set -u

# 3. Make the device product inherit vophone
MK="$(find device -path "*/$DEVICE/lineage_$DEVICE.mk" -print -quit)"
if [ -z "$MK" ]; then
    echo "device/*/$DEVICE/lineage_$DEVICE.mk not found. Is '$DEVICE' the right codename?" >&2
    exit 1
fi
if ! grep -q "vendor/vophone/vophone.mk" "$MK"; then
    printf '\n# vophone\n$(call inherit-product, vendor/vophone/vophone.mk)\n' >> "$MK"
    echo ">> Added vophone to $MK"
fi

# 4. Build (takes hours the first time)
echo ">> Building the ROM"
set +u
mka bacon
set -u

echo
echo ">> Done. Files to flash are in: $SRC/out/target/product/$DEVICE/"
ls -1 "$SRC/out/target/product/$DEVICE/"lineage-*.zip 2>/dev/null || true
