#!/usr/bin/env bash
# Run locally where the owner-controlled TEST keystore is available.
# Usage: tools/sign-test-apk.sh /path/to/ci-debug.apk /path/to/keystore.jks keyAlias /path/to/TESSEL-alpha16.apk
set -euo pipefail
[[ $# == 4 ]] || { echo "Usage: $0 input.apk test-keystore alias output.apk" >&2; exit 2; }
input="$1"; keystore="$2"; alias="$3"; output="$4"
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
[[ -n "$sdk" ]] || { echo "Set ANDROID_SDK_ROOT" >&2; exit 2; }
build_tools="$sdk/build-tools/37.0.0"
apksigner="$build_tools/apksigner"
aapt="$build_tools/aapt"
[[ -x "$apksigner" && -x "$aapt" && -f "$input" && -f "$keystore" && ! -e "$output" ]] || { echo "Missing tool/input or output already exists" >&2; exit 2; }
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
# apksigner prompts for passwords; never put passwords on the command line or in Git.
"$apksigner" sign --ks "$keystore" --ks-key-alias "$alias" --out "$tmp/test.apk" "$input"
report="$("$apksigner" verify --verbose --print-certs "$tmp/test.apk")"
printf '%s\n' "$report"
fingerprint="$(printf '%s\n' "$report" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p' | tr '[:upper:]' '[:lower:]')"
[[ "$fingerprint" == "9fd3c6ade8dfb2b7d9912f0c6f028d810aed8b9fa5914eee594ee992b91fdc19" ]] || { echo "ERROR: wrong test signing certificate" >&2; exit 1; }
badging="$("$aapt" dump badging "$tmp/test.apk")"
printf '%s\n' "$badging" | grep -q "^package: name='com.kreatonik.tessel.debug' versionCode='37' versionName='1.1.0-alpha.16'"
cp "$tmp/test.apk" "$output"
if command -v sha256sum >/dev/null; then sha256sum "$output"; else shasum -a 256 "$output"; fi
