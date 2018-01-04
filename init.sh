#! /bin/sh

set -eu

SDK_PLATFORM="platforms;android-26"

echo yes | "${ANDROID_HOME}/tools/bin/sdkmanager" "${SDK_PLATFORM}"
