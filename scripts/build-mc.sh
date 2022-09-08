#!/bin/bash

# (m-c = mozilla-central repo)

set -e

BUILD_DIR=$(realpath $(pwd))
#SOURCE_DIR=$(dirname -- $(dirname -- "$(readlink -f -- "$BASH_SOURCE")"))
#SOURCE_DIR_RW=${BUILD_DIR}/source-rw

IS_RELEASE_BUILD=0

export MOZBUILD_STATE_PATH="${HOME}/.mozbuild"
export PATH="${MOZBUILD_STATE_PATH}/android-sdk-linux/build-tools/31.0.0/:$HOME/.cargo/bin:$PATH"
MOZ_FETCHES_DIR=${BUILD_DIR}/moz_fetches

function usage {
    echo "build-mc.sh -- Builds mozilla-central binaries for android"
    echo "Usage: build-mc.sh [OPTION]..."
    echo "  -r                            Make a release build."
    exit 1
}

# CENO v2: Most options are no longer needed for building m-c
# release option may be the only relevant option anymore
while getopts r option; do
    case "$option" in
        r)
            IS_RELEASE_BUILD=1
            ;;
        *)
            usage
    esac
done

ABI=${ABI:-armeabi-v7a}
case "$ABI" in
    omni)
        TARGET=aarch64
        ;;
    armeabi-v7a)
        TARGET=arm-linux-androideabi
        ;;
    arm64-v8a)
        TARGET=aarch64
        ;;
    x86_64)
        TARGET=x86_64
        ;;
    x86)
        TARGET=i686
        ;;
    *)
        echo "Unknown ABI: '$ABI', valid values are armeabi-v7a, arm64-v8a, x86 and x86_64."
        exit 1
esac
if [ $IS_RELEASE_BUILD -eq 1 ]; then
    VARIANT=release
else
    VARIANT=debug
fi

ABI_BUILD_DIR="${BUILD_DIR}"/build-${ABI}-${VARIANT}
AAR_OUTPUT_DIR="${ABI_BUILD_DIR}"/gradle/maven/org/mozilla/geckoview/geckoview-default-omni-${ABI}/${MOZ_MAJOR_VER}.0.${MOZ_BUILD_DATE}

# CENO v2: TODO mount_cow was triggering "Too many open files" error, is this still needed?
#function mount_cow {
#    local WORK_DIR="${BUILD_DIR}"/source-cow-work
#
#    local DO_INITIALIZE
#    [[ -e ${SOURCE_DIR_RW} ]] && DO_INITIALIZE=false || DO_INITIALIZE=true
#    local IS_MOUNTED
#    mount | awk '{ print $3 }' | grep -x -F "$(realpath "${SOURCE_DIR_RW}")" >/dev/null && IS_MOUNTED=true || IS_MOUNTED=false
#
#    mkdir -p "${WORK_DIR}"
#    mkdir -p "${SOURCE_DIR_RW}"
#
#    if ! $IS_MOUNTED; then
#        unionfs -o cow -o hide_meta_files "${WORK_DIR}"=RW:"${SOURCE_DIR}"=RO "${SOURCE_DIR_RW}"
#    fi
#    trap "sleep 1 && fusermount -u -z '${SOURCE_DIR_RW}'" EXIT
#
#    if $DO_INITIALIZE; then
#        pushd "${SOURCE_DIR_RW}"/${MOZ_DIR} >/dev/null
#	./mach clobber
#        popd >/dev/null
#    fi
#}

function patch_mc {
    local COOKIE_FILE="${BUILD_DIR}"/.finished-patch
    if [[ -e "${COOKIE_FILE}" ]]; then
        return
    fi
    pushd "${MOZ_DIR}" >/dev/null
    # Apply any patches needed for CENO/Ouinet functionality
    #moz-phab patch --apply-to here --nocommit --skip-dependencies D153882
    popd >/dev/null
    touch "${COOKIE_FILE}"
}

function bootstrap_mc {
    local COOKIE_FILE="${BUILD_DIR}"/.finished-bootstrap
    if [[ -e "${COOKIE_FILE}" ]]; then
        return
    fi

    if ! command -v rustc >/dev/null; then
        # Install rust https://www.rust-lang.org/en-US/install.html
        curl https://sh.rustup.rs -sSf | sh -s -- -y
        rustup update
        rustup toolchain install 1.37.0
        rustup default 1.37.0
    fi

    pushd ${MOZ_DIR} >/dev/null
    ./mach --no-interactive bootstrap --application-choice="GeckoView/Firefox for Android"
    popd >/dev/null
    touch "${COOKIE_FILE}"
}

function write_build_config {
    function cp_if_different {
        local from="$1"
        local to="$2"
        cmp -s "$from" "$to" || cp "$from" "$to"
    }

    mkdir -p "${ABI_BUILD_DIR}"

    pushd "${ABI_BUILD_DIR}" >/dev/null

    cat > mozconfig-new <<MOZCONFIG_BASE
# Build GeckoView/Firefox for Android:
ac_add_options --enable-application=mobile/android
ac_add_options --target=${TARGET}

# With the following Android SDK and NDK
ac_add_options --with-android-sdk="${MOZBUILD_STATE_PATH}/android-sdk-linux"
# Only the NDK version installed by ./mach bootstrap is supported.
ac_add_options --with-android-ndk="${MOZBUILD_STATE_PATH}/android-ndk-r21d"

# Only the versions of clang and ld installed by ./mach bootstrap are supported.
CC="${MOZBUILD_STATE_PATH}/clang/bin/clang"
CXX="${MOZBUILD_STATE_PATH}/clang/bin/clang++"
# Use the linker installed by mach instead of the system linker.
ac_add_options --enable-linker=lld

mk_add_options 'export CCACHE_COMPRESS=""'
mk_add_options 'export CCACHE_CPP2=yes'
ac_add_options --with-ccache=sccache

mk_add_options MOZ_OBJDIR="${ABI_BUILD_DIR}"

ac_add_options --disable-crashreporter
ac_add_options --disable-updater
MOZCONFIG_BASE

    if [[ $IS_RELEASE_BUILD -eq 1 ]]; then
        echo "export MOZILLA_OFFICIAL=1" >> mozconfig-new
        # This disables Android Strict Mode for release builds
        # (i.e. "StrictMode policy violation" messages in the Android log),
        # according to `org.mozilla.gecko.GeckoApp.onCreate()`.
        # It also disables site issue reporting,
        # according to `gecko-dev/mobile/android/extensions/moz.build`
        # and `gecko-dev/mobile/android/locales/jar.mn`.
        #echo "ac_add_options --enable-update-channel=release" >> mozconfig-new
        echo "ac_add_options --enable-release" >> mozconfig-new
        echo "ac_add_options --disable-debug" >> mozconfig-new
        echo "ac_add_options --enable-optimize" >> mozconfig-new
    fi

    if [ "$ABI" == armeabi-v7a -o "$ABI" == x86 -o "$ABI" == x86_64 ]; then
        # See https://mozilla.logbot.info/mobile/20190706#c16442172
        # This can be removed when the bug causing it is fixed.
        echo "ac_add_options --disable-elf-hack" >> mozconfig-new
    fi

    if [ "$ABI" == omni ]; then
        export MOZ_FETCHES_DIR=${MOZ_FETCHES_DIR}
        export MOZ_ANDROID_FAT_AAR_ARCHITECTURES="armeabi-v7a,arm64-v8a"
        export MOZ_ANDROID_FAT_AAR_ARM64_V8A=geckoview-default-omni-arm64-v8a-${MOZ_MAJOR_VER}.0.${MOZ_BUILD_DATE}.aar
        export MOZ_ANDROID_FAT_AAR_ARMEABI_V7A=geckoview-default-omni-armeabi-v7a-${MOZ_MAJOR_VER}.0.${MOZ_BUILD_DATE}.aar
    fi

    export MOZCONFIG="${ABI_BUILD_DIR}/mozconfig"
    export MOZ_BUILD_DATE=${MOZ_BUILD_DATE}

    cp_if_different mozconfig-new mozconfig

    popd >/dev/null
}

function build_mc {
    pushd "${MOZ_DIR}" >/dev/null
    ./mach build
    popd >/dev/null
}

function package_mc {
    pushd "${MOZ_DIR}" >/dev/null
    if [ "$ABI" != omni ]; then
        ./mach build binaries && ./mach gradle geckoview:publishWithGeckoBinariesDebugPublicationToMavenRepository
        mkdir -p "${MOZ_FETCHES_DIR}" && cp "${AAR_OUTPUT_DIR}"/*.aar ${MOZ_FETCHES_DIR}/.
    fi
    popd >/dev/null
}

#mount_cow
#patch_mc
bootstrap_mc
write_build_config
build_mc
package_mc
