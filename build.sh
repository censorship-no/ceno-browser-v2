#!/bin/bash

set -e

BUILD_DIR=$(pwd)
SOURCE_DIR=$(dirname -- "$(readlink -f -- "$BASH_SOURCE")")
GECKO_DIR=gecko-dev
AC_DIR=./android-components
ANDROID_HOME=$HOME/.mozbuild/android-sdk-linux
LOCAL_PROPERTIES=local.properties

SUPPORTED_ABIS=(armeabi-v7a arm64-v8a x86 x86_64)
RELEASE_DEFAULT_ABIS=(armeabi-v7a arm64-v8a)
DEFAULT_ABI=arm64-v8a

CLEAN=false
BUILD_RELEASE=false
BUILD_DEBUG=false
BUILD_OUINET=false
USE_LOCAL_GECKOVIEW=false
VARIANT=
BUILD_DATE=

ABIS=()
OUINET_CONFIG_XML=
VERSION_NUMBER=
RELEASE_KEYSTORE_KEY_ALIAS=upload
RELEASE_KEYSTORE_FILE=
RELEASE_KEYSTORE_PASSWORDS_FILE=

BRAND=ceno
BRAND_DIR=
RES_DIR="app/src/main/res"

function usage {
    echo "build.sh -- Builds CENO v2 APKs and (optionally) builds ouinet and geckoview dependencies"
    echo "Usage: build.sh [OPTION]..."
    echo "  -c                            Remove build files (keep downloaded dependencies)"
    echo "  -r                            Build a release build. Requires -v, -k, and -p."
    echo "  -d                            Build a debug build. Will optionally apply -x and -v. This is the default."
    echo "  -o                            Build ouinet from sources and pass the resulting AAR to build-fennec."
    echo "  -a <abi>                      Build for android ABI <abi>. Can be specified multiple times."
    echo "                                Supported ABIs are [${SUPPORTED_ABIS[@]}]."
    echo "                                Default for debug builds is ${DEFAULT_ABI}."
    echo "                                Default for release builds is all supported ABIs."
    echo "  -g                            Build local copy of geckoview AAR and use in build of CENO"
    echo "  -l                            Use local build of geckoview AAR in CENO build, but do not rebuild geckoview"
    echo "  -v <version-number>           The version number to use on the APK."
    echo "  -k <keystore-file>            The keystore to use for signing the release APK."
    echo "                                Must contain the signing key aliased as '${RELEASE_KEYSTORE_KEY_ALIAS}'."
    echo "  -p <keystore-password-file>   The password file containing passwords to unlock the keystore file."
    echo "                                Must contain the password for the keystore, followed by the"
    echo "                                password for the signing key, on separate lines."
    exit 1
}

while getopts crdoa:glx:v:k:p:b: option; do
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
            BUILD_GECKOVIEW=true
            ;;
        l)
            USE_LOCAL_GECKOVIEW=true
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
        b)
            BRAND="${OPTARG}"
            ;;
        *)
            usage
    esac
done

if $CLEAN; then
    rm .build_date || true
    rm *.apk || true
    rm *.aar || true
    rm -rf ouinet-*-{debug,release}/build-android-*-{debug,release} || true
    exit
fi

[[ -z $OUINET_CONFIG_XML ]] && echo "WARNING: no ouinet.xml included, BT Bootstrap configuration may be missing from this build!"

function check_variant {
    $BUILD_RELEASE || $BUILD_DEBUG || BUILD_DEBUG=true

    if $BUILD_RELEASE; then
        [[ -z $VERSION_NUMBER ]] && echo "Missing version number" && usage
        [[ -z $RELEASE_KEYSTORE_FILE ]] && echo "Missing keystore file" && usage
        [[ -z $RELEASE_KEYSTORE_PASSWORDS_FILE ]] && echo "Missing keystore password file" && usage
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

    for variant in debug release; do
        if [[ $variant = debug ]]; then
            $BUILD_DEBUG || continue
            KEYSTORE_FILE="$(realpath ${DEBUG_KEYSTORE_FILE})"
            KEYSTORE_KEY_ALIAS="${DEBUG_KEYSTORE_KEY_ALIAS}"
            KEYSTORE_PASSWORDS_FILE="$(realpath ${DEBUG_KEYSTORE_PASSWORDS_FILE})"
            OUINET_VARIANT_FLAGS=
            GECKO_VARIANT_FLAGS=
            VARIANT=debug
        else
            $BUILD_RELEASE || continue
            KEYSTORE_FILE="$(realpath ${RELEASE_KEYSTORE_FILE})"
            KEYSTORE_KEY_ALIAS="${RELEASE_KEYSTORE_KEY_ALIAS}"
            KEYSTORE_PASSWORDS_FILE="$(realpath ${RELEASE_KEYSTORE_PASSWORDS_FILE})"
            OUINET_VARIANT_FLAGS=-r
            GECKO_VARIANT_FLAGS=-r
            VARIANT=release
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
    fi
}

function cp_if_different {
    local from="$1"
    local to="$2"
    cmp -s "$from" "$to" || cp "$from" "$to"
}

function set_property {
    local property=$1
    local value=$2
    local regex="^${property}=.*"
    if grep -q ${regex} ${LOCAL_PROPERTIES}; then
        sed -i "s|${regex}|${property}=${value}|" ${LOCAL_PROPERTIES}
    else
        echo "${property}=${value}" >> ${LOCAL_PROPERTIES}
    fi
}

function get_set_branding {
    BRAND_DIR="${BUILD_DIR}/branding/${BRAND}"
    if [[ -d "${BRAND_DIR}" ]]; then
        cp -rf ${BRAND_DIR}/res/* ${RES_DIR}/.
    fi
}

function maybe_build_ouinet {
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
    done
}

function maybe_build_geckoview {
    if [ $USE_LOCAL_GECKOVIEW ] && [ $BUILD_GECKOVIEW ]; then
        GROUP_ID=${GROUP_ID} \
        OSSRH_USERNAME=${OSSRH_USERNAME} \
        OSSRH_PASSWORD=${OSSRH_PASSWORD} \
        SIGNING_PASSWORD=${SIGNING_PASSWORD} \
        SIGNING_KEY_ID=${SIGNING_KEY_ID} \
        SIGNING_KEY=${SIGNING_KEY} \
        ./scripts/build-dependencies.sh -r -a armeabi-v7a -a arm64-v8a
    fi
}

function write_local_properties {
    cp -n ${LOCAL_PROPERTIES}.sample ${LOCAL_PROPERTIES}

    STORE_PASSWORD=$(sed -n '1p' ${KEYSTORE_PASSWORDS_FILE})
    KEY_PASSWORD=$(sed -n '2p' ${KEYSTORE_PASSWORDS_FILE})

    set_property sdk.dir ${ANDROID_HOME}
    set_property versionName ${VERSION_NUMBER}
    if $BUILD_DEBUG; then
        # Use cached build date for buildId
        set_property buildId ${BUILD_DATE}
    else
        # Generate a new buildId during gradle build
        set_property buildId ""
    fi
    set_property autoPublish.android-components.dir ${AC_DIR}
    set_property RELEASE_STORE_FILE ${KEYSTORE_FILE}
    set_property RELEASE_STORE_PASSWORD ${STORE_PASSWORD}
    set_property RELEASE_KEY_ALIAS ${KEYSTORE_KEY_ALIAS}
    set_property RELEASE_KEY_PASSWORD ${KEY_PASSWORD}

    if $USE_LOCAL_GECKOVIEW; then
        GECKO_SRC_DIR=${SOURCE_DIR}/${GECKO_DIR}
        GECKO_OBJ_DIR=${SOURCE_DIR}/build-omni-${VARIANT}
        set_property dependencySubstitutions.geckoviewTopsrcdir ${GECKO_SRC_DIR}
        set_property dependencySubstitutions.geckoviewTopobjdir ${GECKO_OBJ_DIR}
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

    # TODO include bootstrap nodes in local properties also
    if [[ -n $OUINET_CONFIG_XML ]]; then
        cp_if_different "${OUINET_CONFIG_XML}" "${SOURCE_DIR}"/app/src/main/res/values/ouinet.xml
    fi
}

function build_apk_for {
    local abis=$1[@]
    local var="$2"
    local list=("${!abis}")

    local DATE="$(date  +'%Y-%m-%d_%H%m')"

    CENOBROWSER_BUILD_DIR="${SOURCE_DIR}/app/build/outputs/apk/${var}"
    if [[ $var = debug ]]; then
        "${SOURCE_DIR}"/gradlew assembleDebug
    elif [[ $var = release ]]; then
        "${SOURCE_DIR}"/gradlew assembleRelease
    fi

    for abi in ${list[@]}; do
        CENOBROWSER_APK_BUILT="${CENOBROWSER_BUILD_DIR}"/app-${abi}-${var}.apk
        CENOBROWSER_APK="${SOURCE_DIR}"/ceno-${abi}-${var}-${VERSION_NUMBER}-${DATE}.apk
        cp "${CENOBROWSER_APK_BUILT}" "${CENOBROWSER_APK}"
    done
}

check_variant
get_set_build_date
get_set_abis
get_set_branding
maybe_build_ouinet
maybe_build_geckoview
write_local_properties
build_apk_for ABIS $VARIANT