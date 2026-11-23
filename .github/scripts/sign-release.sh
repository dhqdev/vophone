#!/usr/bin/env bash
# Signs a vophone release APK in place (called by .github/workflows/release.yml).
# See docs/ATUALIZACOES.md, "Chave de assinatura".
#
# With the GitHub secrets VOPHONE_RELEASE_KEYSTORE (the .p12 file in base64) and VOPHONE_RELEASE_PASSWORD:
#   signed with vophone's own private key, plus the proof that the old public key
#   (app/vophone-debug.keystore) hands over to it (APK Signature Scheme v3 key rotation).
#   Phones on Android 9+ with an older vophone update by themselves, without reinstalling,
#   and from then on only accept versions signed with the private key.
#   Android 8 and older keep seeing the old key (they have no key rotation).
# Without them: the APK keeps Gradle's signature with the old public key, as before. That is refused
#   if the last release was already signed with the private key (the phones would refuse it and stop updating).
#
# Usage: sign-release.sh <apk>    Needs ANDROID_HOME with build-tools 34.0.0, and GH_TOKEN for the check above.
set -euo pipefail

APK="$1"
APKSIGNER="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/build-tools/34.0.0/apksigner"
# apksigner 34 needs a recent Java; the Gradle build of this project runs on Java 11.
if [ -n "${JAVA_HOME_17_X64:-}" ]; then
    export JAVA_HOME="$JAVA_HOME_17_X64"
    export PATH="$JAVA_HOME/bin:$PATH"
fi
OLD_KEY=(--ks app/vophone-debug.keystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android)

if [ -z "${VOPHONE_RELEASE_KEYSTORE:-}" ]; then
    debug_digest=$(keytool -list -v -keystore app/vophone-debug.keystore -storepass android -alias androiddebugkey 2>/dev/null \
        | sed -n 's/.*SHA256: //p' | tr -d ': ' | tr 'A-F' 'a-f' | head -n 1)
    latest_url=$(curl -fsSL -H "Authorization: Bearer ${GH_TOKEN:-}" "https://api.github.com/repos/$GITHUB_REPOSITORY/releases/latest" 2>/dev/null \
        | jq -r '[.assets[]? | select(.name | endswith(".apk")) | .browser_download_url][0] // empty' || true)
    if [ -n "$latest_url" ] && curl -fsSL -o latest-release.apk "$latest_url"; then
        latest_digests=$("$APKSIGNER" verify --print-certs latest-release.apk | sed -n 's/.*SHA-256 digest: //p' || true)
        rm -f latest-release.apk
        if [ -z "$debug_digest" ]; then
            echo "::warning::Could not read the old key's fingerprint; skipping the check of the last release."
        elif [ -n "$latest_digests" ] && grep -vqx "$debug_digest" <<< "$latest_digests"; then
            echo "::error::The last release was signed with vophone's private key, but the secret VOPHONE_RELEASE_KEYSTORE is missing: phones would refuse this version. Add the secrets again (docs/ATUALIZACOES.md)."
            exit 1
        fi
    fi
    echo "No VOPHONE_RELEASE_KEYSTORE secret: signed with the old public key, like the versions before."
    "$APKSIGNER" verify --print-certs "$APK"
    exit 0
fi

if [ -z "${VOPHONE_RELEASE_PASSWORD:-}" ]; then
    echo "::error::The secret VOPHONE_RELEASE_PASSWORD is missing (the password of VOPHONE_RELEASE_KEYSTORE)."
    exit 1
fi

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
(umask 077 && printf '%s' "$VOPHONE_RELEASE_KEYSTORE" | base64 -d > "$work/release.p12")
NEW_KEY=(--ks "$work/release.p12" --ks-key-alias vophone --ks-pass env:VOPHONE_RELEASE_PASSWORD --key-pass env:VOPHONE_RELEASE_PASSWORD)

# The proof "old key -> new key". Made again at every release: same keys, same proof.
"$APKSIGNER" rotate --out "$work/lineage" --old-signer "${OLD_KEY[@]}" --new-signer "${NEW_KEY[@]}"
# v1/v2 with the old key (Android 8 and older), v3 with the new key and the proof (Android 9 and newer).
"$APKSIGNER" sign "${OLD_KEY[@]}" --next-signer "${NEW_KEY[@]}" \
    --lineage "$work/lineage" --rotation-min-sdk-version 28 \
    --out "$work/signed.apk" "$APK"

"$APKSIGNER" verify --verbose --print-certs "$work/signed.apk"
"$APKSIGNER" lineage --in "$work/signed.apk" --print-certs
mv "$work/signed.apk" "$APK"
echo "Signed with vophone's private key (rotated from the old public key)."
