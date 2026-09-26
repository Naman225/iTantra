#!/bin/bash
# iTantra Android Dependency Setup
# Downloads the official sherpa-onnx Android AAR into android/app/libs/

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIBS_DIR="$SCRIPT_DIR/app/libs"
mkdir -p "$LIBS_DIR"

AAR_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-1.13.8.aar"
TARGET_AAR="$LIBS_DIR/sherpa-onnx-1.13.8.aar"

if [ -f "$TARGET_AAR" ] && [ $(stat -c%s "$TARGET_AAR") -gt 10000000 ]; then
    echo "[OK] sherpa-onnx-1.13.8.aar already present in app/libs"
else
    echo "[DOWNLOADING] sherpa-onnx Android AAR (~50MB) ..."
    curl -L -o "$TARGET_AAR" "$AAR_URL"
    echo "[DONE] Downloaded sherpa-onnx AAR to $TARGET_AAR"
fi
