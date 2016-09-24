#! /bin/sh

set -eu

BUILD_TOOL=$("${ANDROID_HOME}/tools/android" list sdk --all | grep 'Android SDK Build-tools, revision 22.0.1' | cut -d '-' -f 1 | sed 's/ //g')
SDK_PLATFORM=$("${ANDROID_HOME}/tools/android" list sdk --all | grep 'SDK Platform Android 5.1.1, API 22, revision 2' | cut -d '-' -f 1 | sed 's/ //g')

"${ANDROID_HOME}/tools/android" update sdk -u -a -t "${BUILD_TOOL}"
"${ANDROID_HOME}/tools/android" update sdk -u -a -t "${SDK_PLATFORM}"

"${ANDROID_HOME}/tools/android" update sdk --no-ui --all --filter extra-android-support
"${ANDROID_HOME}/tools/android" update sdk --no-ui --all --filter extra-android-m2repository
