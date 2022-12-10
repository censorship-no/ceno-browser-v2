#  CENO Browser v2

WARNING: This repository does not represent the current stable version of CENO Browser, please see https://github.com/censorship-no/ceno-browser for the current stable release of CENO Browser.  

A next-generation CENO Browser built from [Mozilla Android Components](https://github.com/mozilla-mobile/firefox-android/blob/main/android-components/README.md).

It includes the Mozilla Web Platform via GeckoView and a Ouinet Client for sharing web content peer-to-peer.

## 🚀 Features

🌴 **Browse freely, anytime.**  
CENO is designed with internet shutdown scenarios in mind. Websites are shared by a global network of peers, and stored in a distributed cache for availability when traditional networks are blocked or go down.

🔓 **Unlock the web.**  
Access any website. Frequently requested content is cached on the network and cannot be forcibly removed.

💲 **Reduce Data Costs.**  
By routing user traffic through peer-to-peer networks, CENO Browser incurs less data costs while still providing users with circumvention capability.

🌐 **Grow the Network, Fight Censorship.**  
Fight censorship by becoming a bridge! Install and run CENO Browser to instantly join the network and expand the availability of blocked websites to those in censored countries.

👐 **Free and open source.**  
CENO Browser is powered by Ouinet, an open source library enabling third party developers to incorporate the CENO network into their apps for peer-to-peer connectivity.

## 👪 Contributing!
Interested in contributing to the project? Great! For starters, make sure to review and agree to the terms of our [Code of Conduct](CODE_OF_CONDUCT.md)

Here are some ways to help CENO Browser improve:
* Test the app with different devices
* Report issues in the [issue tracker](https://gitlab.com/censorship-no/ceno-browser-v2/issues)
* Create a [Pull Request](https://opensource.guide/how-to-contribute/#opening-a-pull-request)
* Help increasing the test coverage by contributing unit tests
* Translate the app on [Weblate](https://hosted.weblate.org/projects/censorship-no/)

### ➿ Translations
Translation support is needed for:
* [Android strings](https://hosted.weblate.org/projects/censorship-no/android-strings/)
* the [CENO web extension](https://gitlab.com/censorship-no/ceno-ext-settings/)
* The [user manual](https://github.com/censorship-no/ceno-docs/)
* The [website](https://censorship.no)

We use Weblate for continuously-updated translations. To get started, create an account at https://weblate.org and visit https://hosted.weblate.org/projects/censorship-no/ to join the project.

## 🔧 Building
### Developer Build

The Ouinet client configuration is currently hardcoded at build time and cannot be changed at run time.  You may customize a copy of the provided `local.properties.sample` with your values and put it in the root of the `CENO Browser` checkout as `local.properties`.

After setting the configuration values correctly, you can build debug versions of the APKs with the following command:

```
./build.sh -a arm64-v8a -a armeabi-v7a
```

By default, the latest versions of the Ouinet library and GeckoView (CENO fork) are automatically downloaded from Maven Central repository and used for building both the debug and release variants CENO Browser.

# Test Channel on Google Play Store

COMING SOON!

# Local Development

You might be interested in building this project against local versions of some of the dependencies. Depending on which dependencies you're building against, there are couple of paths.

## Auto-publication workflow

This is the most streamlined workflow which fully automates dependency publication. It currently supports [android-components](https://github.com/mozilla-mobile/android-components/) and [application-services](https://github.com/mozilla/application-services) dependencies.

In a `local.properties` file in root of the `reference-browser` checkout, specify relative paths to a repository you need (or both):
```
# Local workflow
autoPublish.android-components.dir=../android-components
autoPublish.application-services.dir=../application-services
```

That's it! Next build of `CENO Browser` will be against your local versions of these repositories. Simply make changes in `android-components` or `application-services`, press Play in `reference-browser` and those changes will be picked-up.

See a [demo of this workflow](https://www.youtube.com/watch?v=qZKlBzVvQGc) in action. Video mentions `Fenix`, but it works in exactly the same way with `CENO Browser`.

## Dependency substitutions for [GeckoView](https://hg.mozilla.org/mozilla-central)

CENO Browser requires a fork of GeckoView, which is pulled in automatically from [Maven Central](https://repo1.maven.org/maven2/ie/equalit/ouinet/geckoview-ceno-omni/).

However, GeckoView currently can also be configured via a dependency substitution to test changes to the library locally.

In a `local.properties` file in the root of the `CENO Browser` checkout, specify GeckoView's path via `dependencySubstitutions.geckoviewTopsrcdir=/path/to/mozilla-central` (and, optionally, `dependencySubstitutions.geckoviewTopobjdir=/path/to/topobjdir`). See [Bug 1533465](https://bugzilla.mozilla.org/show_bug.cgi?id=1533465).

This assumes that you have built, packaged, and published your local GeckoView -- but don't worry, the dependency substitution script has the latest instructions for doing that (WARNING: Building GeckoView can take very long, e.g. >40mins depending on your computers resources).

Do not forget to run a Gradle sync in Android Studio after changing `local.properties`. If you specified any substitutions (e.g. GeckoView), they will be reflected in the modules list, and you'll be able to modify them from a single Android Studio window. For auto-publication workflow, use seperate Android Studio windows.

# Ouinet Integration
Prior to building the CENO Browser you will want to set the Ouinet configuration in a `local.properties`, by setting the following values:
```groovy
CACHE_PUB_KEY="YOUR OUINET CACHE PUB KEY"
INJECTOR_CREDENTIALS="ouinet:YOURINJECTORPASSWORD"
INJECTOR_TLS_CERT="-----BEGIN CERTIFICATE-----\\n\
ABCDEFG...\
\\n-----END CERTIFICATE-----"
```
An example configuration can be copied from `local.properties.sample`, but this is essentially empty and will not allow you to connect to the production CENO network.

Those values will be loaded by Gradle during the build process in **app/build.gradle**:

# License

    This Source Code Form is subject to the terms of the Mozilla Public
    License, v. 2.0. If a copy of the MPL was not distributed with this
    file, You can obtain one at http://mozilla.org/MPL/2.0/
