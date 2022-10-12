#!/bin/bash

set -e

BUILD_DIR=$(pwd)/geckoview
mkdir -p "${BUILD_DIR}"
SOURCE_DIR=$(dirname -- "$(readlink -f -- "$BASH_SOURCE")")
GECKO_DIR=gecko-dev
MOZ_FETCHES_DIR=${BUILD_DIR}/fetches

SUPPORTED_ABIS=(armeabi-v7a arm64-v8a x86 x86_64)
RELEASE_DEFAULT_ABIS=(armeabi-v7a arm64-v8a)
DEFAULT_ABI=arm64-v8a
RELEASE_KEYSTORE_KEY_ALIAS=upload

CLEAN=false
BUILD_RELEASE=false
BUILD_DEBUG=false
VARIANT=
GECKO_VARIANT_FLAGS=
BUILD_DATE=

ABIS=()
GECKOVIEW_VERSION=107
EXOPLAYER_VERSION=107.0.20220919105141

function usage {
    echo "build-dependencies.sh -- Builds Android Archive dependencies required for CENO Browser"
    echo "Usage: build-dependencies.sh [OPTION]..."
    echo "  -c                            Remove build files (keep downloaded dependencies)"
    echo "  -r                            Build a release build"
    echo "  -d                            Build a debug build. Will optionally apply -x and -v. This is the default."
    echo "  -p                            Publish release AARs to Sonatype, credentials and keys must be"
    echo "                                provided as enviroment variables"
    echo "  -a <abi>                      Build for android ABI <abi>. Can be specified multiple times."
    echo "                                Supported ABIs are [${SUPPORTED_ABIS[@]}]."
    echo "                                Default for debug builds is ${DEFAULT_ABI}."
    echo "                                Default for release builds is ${RELEASE_DEFAULT_ABIS}."
    echo "  -g <gecko-dir>                The directory where local copy of gecko-dev source code is stored"
    echo "  -v <mozilla-version-number>   The major version number of mozilla-central to be used."
    exit 1
}

while getopts crda:g:v:p option; do
    case "$option" in
        c)
            CLEAN=true
            ;;
        r)
            BUILD_RELEASE=true
            ;;
        d)
            BUILD_DEBUG=true
            ;;
        a)
            supported=false
            for i in ${SUPPORTED_ABIS[@]}; do [[ $i = $OPTARG ]] && supported=true && break; done
            listed=false
            for i in ${ABIS[@]}; do [[ $i = $OPTARG ]] && listed=true && break; done

            if ! $supported; then
                echo "Unknown ABI. Supported ABIs are [${SUPPORTED_ABIS[@]}]."
                exit 1
            fi
            if ! $listed; then
                ABIS+=($OPTARG)
            fi
            ;;
        g)
            GECKO_DIR="${OPTARG}"
            ;;
        v)
            GECKOVIEW_VERSION="${OPTARG}"
            ;;
        p)
            BUILD_RELEASE=true
            PUBLISH_RELEASE=true
            ;;
        *)
            usage
    esac
done

function get_set_abis {
    if [[ ${#ABIS[@]} -eq 0 ]]; then
        if $BUILD_RELEASE; then
            ABIS=${RELEASE_DEFAULT_ABIS[@]}
        else
            ABIS=($DEFAULT_ABI)
        fi
    fi
    if [[ ${#ABIS[@]} -eq 1 ]]; then
        IS_OMNI_BUILD=false
    else
        IS_OMNI_BUILD=true
        ABIS+=(omni)
    fi
}

function check_variant {
    for variant in debug release; do
        if [[ $variant = debug ]]; then
            $BUILD_DEBUG || continue
            VARIANT=debug
            GECKO_VARIANT_FLAGS="bootstrap build"
            SUFFIX=-default
        else
            $BUILD_RELEASE || continue
            VARIANT=release
            if $PUBLISH_RELEASE; then
                GECKO_VARIANT_FLAGS="-r bootstrap build publish"
            else
                GECKO_VARIANT_FLAGS="-r bootstrap build"
            fi
            SUFFIX=-ceno
        fi
    done
}

function get_set_build_date {
    BUILD_DATE_COOKIE=${BUILD_DIR}/".build_date"
    if [ -e "${BUILD_DATE_COOKIE}" ]; then
        BUILD_DATE=$(cat ${BUILD_DATE_COOKIE})
    else
        BUILD_DATE=$(date +%Y%m%d%H%M%S)
        echo $BUILD_DATE > $BUILD_DATE_COOKIE
    fi
}

function build_geckoview {
    for ABI in ${ABIS[@]}; do
        ABI_BUILD_DIR="${BUILD_DIR}"/build-${ABI}-${VARIANT}
        AAR_OUTPUT_DIR="${ABI_BUILD_DIR}"/gradle/maven/${GROUP_ID//.//}/geckoview${SUFFIX}-omni-${ABI}/${GECKOVIEW_VERSION}.0.${BUILD_DATE}

        ABI=${ABI} \
        MOZ_DIR=${GECKO_DIR} \
        MOZ_MAJOR_VER=${GECKOVIEW_VERSION} \
        MOZ_FETCHES_DIR=${MOZ_FETCHES_DIR} \
        GROUP_ID=${GROUP_ID} \
        OSSRH_USERNAME=${OSSRH_USERNAME} \
        OSSRH_PASSWORD=${OSSRH_PASSWORD} \
        SIGNING_PASSWORD=${SIGNING_PASSWORD} \
        SIGNING_KEY_ID=${SIGNING_KEY_ID} \
        SIGNING_KEY=${SIGNING_KEY} \
        EXOPLAYER_VERSION=${EXOPLAYER_VERSION} \
        ./scripts/build-geckoview.sh ${GECKO_VARIANT_FLAGS}

        if [ $IS_OMNI_BUILD ] && [ "${ABI}" != omni ]; then
            mkdir -p "${MOZ_FETCHES_DIR}" && cp "${AAR_OUTPUT_DIR}"/*.aar ${MOZ_FETCHES_DIR}/.
        fi
    done
}

get_set_abis
check_variant
get_set_build_date
build_geckoview