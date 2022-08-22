#!/bin/bash

set -e

BUILD_DIR=$(pwd)
SOURCE_DIR=$(dirname -- "$(readlink -f -- "$BASH_SOURCE")")
GECKO_DIR=gecko-dev
ANDROID_HOME=$HOME/.mozbuild/android-sdk-linux
LOCAL_PROPERTIES=local.properties

SUPPORTED_ABIS=(armeabi-v7a arm64-v8a x86 x86_64)
RELEASE_DEFAULT_ABIS=(armeabi-v7a arm64-v8a)
DEFAULT_ABI=armeabi-v7a
RELEASE_KEYSTORE_KEY_ALIAS=upload

CLEAN=false
BUILD_RELEASE=false
BUILD_DEBUG=false
BUILD_OUINET=false
BUILD_LIGHT=false

ABIS=()
OUINET_CONFIG_XML=
VERSION_NUMBER=
RELEASE_KEYSTORE_FILE=
RELEASE_KEYSTORE_PASSWORDS_FILE=

function usage {
    echo "build.sh -- Builds ouinet and ouifennec for android"
    echo "Usage: build-fennec.sh [OPTION]..."
    echo "  -c                            Remove build files (keep downloaded dependencies)"
    echo "  -r                            Build a release build. Requires -v, -k, and -p."
    echo "  -d                            Build a debug build. Will optionally apply -x and -v. This is the default."
    echo "  -o                            Build ouinet from sources and pass the resulting AAR to build-fennec."
    echo "  -a <abi>                      Build for android ABI <abi>. Can be specified multiple times."
    echo "                                Supported ABIs are [${SUPPORTED_ABIS[@]}]."
    echo "                                Default for debug builds is ${DEFAULT_ABI}."
    echo "                                Default for release builds is all supported ABIs."
    echo "  -g <gecko-dir>                The directory where local copy of gecko-dev source code is stored"
    echo "  -l                            Light build, only re-run gradle build for supplied ABIs, do not re-build gecko-dev"
    echo "  -v <version-number>           The version number to use on the APK."
    echo "  -k <keystore-file>            The keystore to use for signing the release APK."
    echo "                                Must contain the signing key aliased as '${RELEASE_KEYSTORE_KEY_ALIAS}'."
    echo "  -p <keystore-password-file>   The password file containing passwords to unlock the keystore file."
    echo "                                Must contain the password for the keystore, followed by the"
    echo "                                password for the signing key, on separate lines."
    exit 1
}

while getopts crdoa:g:lx:v:k:p: option; do
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
        o)
            echo "Option not currently supported" && usage
            #BUILD_OUINET=true
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
        l)
            BUILD_LIGHT=true
            ;;
        x)
            [[ -n $OUINET_CONFIG_XML ]] && usage
            OUINET_CONFIG_XML="${OPTARG}"
            ;;
        v)
            [[ -n $VERSION_NUMBER ]] && usage
            VERSION_NUMBER="${OPTARG}"
            ;;
        k)
            [[ -n $RELEASE_KEYSTORE_FILE ]] && usage
            RELEASE_KEYSTORE_FILE="${OPTARG}"
            ;;
        p)
            [[ -n $RELEASE_KEYSTORE_PASSWORDS_FILE ]] && usage
            RELEASE_KEYSTORE_PASSWORDS_FILE="${OPTARG}"
            ;;
	s) 
	    ANDROID_HOME="${OPTARG}"
	    ;;
        *)
            usage
    esac
done

if $CLEAN; then
    rm *.apk || true
    rm *.aar || true
    rm -rf ouinet-*-{debug,release}/build-android-*-{debug,release} || true
    exit
fi

[[ -z $OUINET_CONFIG_XML ]] && echo "Missing ouinet config xml" && usage

function cp_if_different {
    local from="$1"
    local to="$2"
    cmp -s "$from" "$to" || cp "$from" "$to"
}

$BUILD_RELEASE || $BUILD_DEBUG || BUILD_DEBUG=true

if $BUILD_RELEASE; then
    [[ -z $VERSION_NUMBER ]] && echo "Missing version number" && usage
    [[ -z $RELEASE_KEYSTORE_FILE ]] && echo "Missing keystore file" && usage
    [[ -z $RELEASE_KEYSTORE_PASSWORDS_FILE ]] && echo "Missing keystore password file" && usage
fi

if [[ ${#ABIS[@]} -eq 0 ]]; then
    if $BUILD_RELEASE; then
        ABIS=${RELEASE_DEFAULT_ABIS[@]}
    else
        ABIS=($DEFAULT_ABI)
    fi
fi

if $BUILD_DEBUG; then
    DEBUG_KEYSTORE_FILE="${BUILD_DIR}/debug.keystore"
    DEBUG_KEYSTORE_KEY_ALIAS=androiddebugkey
    DEBUG_KEYSTORE_PASSWORDS_FILE="${BUILD_DIR}/debug.keystore-passwords"
    if [[ -e ${DEBUG_KEYSTORE_FILE} && -e ${DEBUG_KEYSTORE_PASSWORDS_FILE} ]]; then
        :
    elif [[ -e ~/.android/debug.keystore ]]; then
        cp ~/.android/debug.keystore "${DEBUG_KEYSTORE_FILE}"
        rm -f "${DEBUG_KEYSTORE_PASSWORDS_FILE}"
        echo android >> ${DEBUG_KEYSTORE_PASSWORDS_FILE}
        echo android >> ${DEBUG_KEYSTORE_PASSWORDS_FILE}
    else
        keytool -genkeypair -keystore "${DEBUG_KEYSTORE_FILE}" -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -deststoretype pkcs12 -dname "cn=Unknown, ou=Unknown, o=Unknown, c=Unknown"
        rm -f "${DEBUG_KEYSTORE_PASSWORDS_FILE}"
        echo android >> ${DEBUG_KEYSTORE_PASSWORDS_FILE}
        echo android >> ${DEBUG_KEYSTORE_PASSWORDS_FILE}
    fi
fi

GECKO_SRC_DIR=${SOURCE_DIR}/${GECKO_DIR}
DATE="$(date  +'%Y-%m-%d_%H%m')"
for variant in debug release; do
    if [[ $variant = debug ]]; then
        $BUILD_DEBUG || continue
        KEYSTORE_FILE="$(realpath ${DEBUG_KEYSTORE_FILE})"
        KEYSTORE_KEY_ALIAS="${DEBUG_KEYSTORE_KEY_ALIAS}"
        KEYSTORE_PASSWORDS_FILE="$(realpath ${DEBUG_KEYSTORE_PASSWORDS_FILE})"
        OUINET_VARIANT_FLAGS=
        GECKO_VARIANT_FLAGS=
    else
        $BUILD_RELEASE || continue
        KEYSTORE_FILE="$(realpath ${RELEASE_KEYSTORE_FILE})"
        KEYSTORE_KEY_ALIAS="${RELEASE_KEYSTORE_KEY_ALIAS}"
        KEYSTORE_PASSWORDS_FILE="$(realpath ${RELEASE_KEYSTORE_PASSWORDS_FILE})"
        OUINET_VARIANT_FLAGS=-r
        GECKO_VARIANT_FLAGS=-r
    fi

    for ABI in ${ABIS[@]}; do
        if $BUILD_OUINET; then
            OUINET_BUILD_DIR="${BUILD_DIR}/ouinet-${ABI}-${variant}"
            mkdir -p "${OUINET_BUILD_DIR}"
            pushd "${OUINET_BUILD_DIR}" >/dev/null
            ABI=${ABI} "${SOURCE_DIR}"/ouinet/scripts/build-android.sh ${OUINET_VARIANT_FLAGS}
            popd >/dev/null

            OUINET_AAR_BUILT="${OUINET_BUILD_DIR}"/build-android-${ABI}-${variant}/ouinet/outputs/aar/ouinet-${variant}.aar
            OUINET_AAR="$(realpath ${BUILD_DIR}/ouinet-${ABI}-${variant}-${DATE}.aar)"
            cp "${OUINET_AAR_BUILT}" "${OUINET_AAR}"
            OUINET_AAR_BUILT_PARAMS="-o ${OUINET_AAR}"
        fi

        if [ $BUILD_LIGHT = false ]; then
            ABI=${ABI} MOZ_DIR=${GECKO_DIR} "${SOURCE_DIR}"/scripts/build-mc.sh ${GECKO_VARIANT_FLAGS}
        fi

        GECKO_OBJ_DIR=${SOURCE_DIR}/build-${ABI}-${variant}

        cp -n ${LOCAL_PROPERTIES}.sample ${LOCAL_PROPERTIES}

        if grep -q '^sdk.dir=.*' ${LOCAL_PROPERTIES}; then
            sed -i "s|^sdk.dir=.*|sdk.dir=${ANDROID_HOME}|" ${LOCAL_PROPERTIES}
        else 
            echo "sdk.dir=${ANDROID_HOME}" >> ${LOCAL_PROPERTIES}
        fi

        if grep -q '^dependencySubstitutions.geckoviewTopsrcdir=.*' ${LOCAL_PROPERTIES}; then
            sed -i "s|^dependencySubstitutions.geckoviewTopsrcdir=.*|dependencySubstitutions.geckoviewTopsrcdir=${GECKO_SRC_DIR}|" ${LOCAL_PROPERTIES}
        else 
            echo "dependencySubstitutions.geckoviewTopsrcdir=${GECKO_SRC_DIR}" ${LOCAL_PROPERTIES}
        fi

        if grep -q '^dependencySubstitutions.geckoviewTopobjdir=.*' ${LOCAL_PROPERTIES}; then
            sed -i "s|^dependencySubstitutions.geckoviewTopobjdir=.*|dependencySubstitutions.geckoviewTopobjdir=${GECKO_OBJ_DIR}|" ${LOCAL_PROPERTIES}
        else 
            echo "dependencySubstitutions.geckoviewTopsrcdir=${GECKO_OBJ_DIR}" ${LOCAL_PROPERTIES}
        fi

        if grep -q '^ABI=.*' ${LOCAL_PROPERTIES}; then
            sed -i "s|^ABI=.*|ABI=${ABI}|" ${LOCAL_PROPERTIES}
        else 
            echo "ABI=${ABI}" ${LOCAL_PROPERTIES}
        fi

        if grep -q '^CACHE_PUB_KEY=.*' ${LOCAL_PROPERTIES}; then 
            if grep -q '^INJECTOR_CREDENTIALS=.*' ${LOCAL_PROPERTIES}; then 
                if grep -q '^INJECTOR_TLS_CERT=.*' ${LOCAL_PROPERTIES}; then
                    echo "Ouinet configuration found"
                else
                    echo "INJECTOR_TLS_CERT not found, please add to local.properties"
                    exit 1
                fi
            else
               echo "INJECTOR_CREDENTIAL not found, please add to local.properties"
               exit 1
            fi
        else
            echo "CACHE_PUB_KEY not found, please add to local.properties"
            exit 1
        fi

        if [[ -n $OUINET_CONFIG_XML ]]; then
            cp_if_different "${OUINET_CONFIG_XML}" "${SOURCE_DIR}"/app/src/main/res/values/ouinet.xml
        fi

        CENOBROWSER_BUILD_DIR="${SOURCE_DIR}/app/build/outputs/apk/${variant}"

        if [[ $variant = debug ]]; then
            "${SOURCE_DIR}"/gradlew assembleDebug
        else
            echo "Release build not yet supported"
            #"${SOURCE_DIR}"/gradlew assembleRelease
        fi

        CENOBROWSER_APK_BUILT="${CENOBROWSER_BUILD_DIR}"/app-${ABI}-${variant}.apk
        CENOBROWSER_APK="${SOURCE_DIR}"/cenoV2-${ABI}-${variant}-${VERSION_NUMBER}-${DATE}.apk
        cp "${CENOBROWSER_APK_BUILT}" "${CENOBROWSER_APK}"

    done
done
