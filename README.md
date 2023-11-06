<img src="https://censorship.no/img/logo_ceno.png" width=250px alt="Ceno Browser">


[![pipeline status](https://gitlab.com/censorship-no/ceno-browser/badges/main/pipeline.svg)](https://gitlab.com/censorship-no/ceno-browser/commits/main)
[![Gitlab release (latest by date)](https://img.shields.io/gitlab/v/release/censorship-no/ceno-browser)](https://gitlab.com/censorship-no/ceno-browser/-/releases)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](./LICENSE)
[![Weblate](https://hosted.weblate.org/widgets/censorship-no/-/android-strings/svg-badge.svg)](https://hosted.weblate.org/projects/censorship-no/)

Ceno (Censorship.No!) is a next-generation mobile web browser that uses peer-to-peer technology to deliver websites to your phone and caches popular content with cooperating peers. Ceno can be used to bypass Internet censorship and help others retrieve blocked pages.

Built from [Mozilla Android Components](https://github.com/mozilla-mobile/firefox-android/blob/main/android-components/README.md), it includes the Mozilla Web Platform via GeckoView and a [Ouinet](https://ouinet.work) Client for sharing web content peer-to-peer.

## ▶️ Access

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
      alt="Get it on Play Store"
      height="80">](https://play.google.com/store/apps/details?id=ie.equalit.ceno)
[<img src="https://censorship.no/img/paskoocheh_badge.png"
      alt="Get it on Paskoocheh" 
      height="80">](https://paskoocheh.com/tools/124/android.html?utm_source=UpdatePage)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/ie.equalit.ceno/)
[<img src="https://censorship.no/img/dcomms_badge.png"
     alt="Get it on dComms"
     height="80">](https://dcomm.net.ua/package/ceno/)

## 🚀 Features

🌴 **Browse freely, anytime.**  
Ceno is designed with internet shutdown scenarios in mind. Websites are shared by a global network of peers, and stored in a distributed cache for availability when traditional networks are blocked or go down.

🔓 **Unlock the web.**  
Access any website. Frequently requested content is cached on the network and cannot be forcibly removed.

💲 **Reduce Data Costs.**  
By routing user traffic through peer-to-peer networks, Ceno Browser incurs less data costs while still providing users with circumvention capability.

🌐 **Grow the Network, Fight Censorship.**  
Fight censorship by becoming a bridge! Install and run Ceno Browser to instantly join the network and expand the availability of blocked websites to those in censored countries.

👐 **Free and open source.**  
Ceno Browser is powered by [Ouinet](https://ouinet.work), an open source library enabling third party developers to incorporate the Ceno network into their apps for peer-to-peer connectivity.

## ❗ Important Notice:
Ceno has two modes of operation - **Public** and **Personal**. You can easily toggle between them. Public mode offers the best connectivity but the least privacy - websites that you visit or share are recorded in a publicly accessible registry (BitTorrent). Private mode eliminates this record but may be slower and less efficient at retrieving content. See the [FAQ](https://censorship.no/en/support.html) or [User Manual](https://censorship.no/user-manual/en/) for more details on Ceno usage.

## 🔧 Building
### Developer Build
To build debug versions of Ceno Browser, enter the checkout directory and execute the following commands,
```
cp local.propeties.sample local.properties
ANDROID_HOME=/path/to/Android/Sdk ./gradlew assembleDebug
```
The resulting apks will be copied to the `output/debug/` directory.

The Ouinet client configuration is currently hardcoded at build time and cannot be changed at run time. You may customize the `local.properties` file with your values and rebuild as needed.

By default, the latest versions of the Ouinet library and GeckoView (Ceno fork) are automatically downloaded from the Maven Central repository and used for building both the debug and release variants of Ceno Browser.

## Local Development

You might be interested in building this project against local versions of some of the dependencies. Depending on which dependencies you're building against, there are couple of paths.

### Auto-publication workflow

This is the most streamlined workflow which fully automates dependency publication. It currently supports [android-components](https://github.com/mozilla-mobile/android-components/) and [application-services](https://github.com/mozilla/application-services) dependencies.

In a `local.properties` file in root of the `reference-browser` checkout, specify relative paths to a repository you need (or both):
```
# Local workflow
autoPublish.android-components.dir=../android-components
autoPublish.application-services.dir=../application-services
```

That's it! Next build of `Ceno Browser` will be against your local versions of these repositories. Simply make changes in `android-components` or `application-services`, press Play in `reference-browser` and those changes will be picked-up.

See a [demo of this workflow](https://www.youtube.com/watch?v=qZKlBzVvQGc) in action. Video mentions `Fenix`, but it works in exactly the same way with `Ceno Browser`.

### Dependency substitutions for [GeckoView](https://hg.mozilla.org/mozilla-central)

Ceno Browser requires a fork of GeckoView, which is pulled in automatically from [Maven Central](https://repo1.maven.org/maven2/ie/equalit/ouinet/geckoview-ceno-omni/).

If you are interested in building GeckoView locally, it is recommended that you use our [mozilla-build-scripts](https://gitlab.com/censorship-no/mozilla-build-scripts/) to publish the GeckoView AAR to your local maven repository and then add `mavenLocal()` to the repositories in `app/build.gradle`.

However, GeckoView also be configured via a dependency substitution to test changes to the library locally.

In a `local.properties` file in the root of the `CENO Browser` checkout, specify GeckoView's path via `dependencySubstitutions.geckoviewTopsrcdir=/path/to/mozilla-central` (and, optionally, `dependencySubstitutions.geckoviewTopobjdir=/path/to/topobjdir`). See [Bug 1533465](https://bugzilla.mozilla.org/show_bug.cgi?id=1533465).

This assumes that you have built, packaged, and published your local GeckoView -- but don't worry, the dependency substitution script has the latest instructions for doing that (WARNING: Building GeckoView can take very long, e.g. >40mins depending on your computers resources).

Do not forget to run a Gradle sync in Android Studio after changing `local.properties`. If you specified any substitutions (e.g. GeckoView), they will be reflected in the modules list, and you'll be able to modify them from a single Android Studio window. For auto-publication workflow, use seperate Android Studio windows.

## Ouinet Integration
Prior to building Ceno Browser you will want to set the Ouinet configuration in a `local.properties`, by setting the following values:
```groovy
CACHE_PUB_KEY="YOUR OUINET CACHE PUB KEY"
INJECTOR_CREDENTIALS="ouinet:YOURINJECTORPASSWORD"
INJECTOR_TLS_CERT="-----BEGIN CERTIFICATE-----\\n\
ABCDEFG...\
\\n-----END CERTIFICATE-----"
```
An example configuration can be copied from `local.properties.sample`, but this is essentially empty and will not allow you to connect to the production Ceno network.

Those values will be loaded by Gradle during the build process in **app/build.gradle**:

# Accessibility

If your code has user-facing changes, follow [Android accessibility best practices](https://github.com/mozilla-mobile/shared-docs/blob/main/android/accessibility_guide.md).

# License

This Source Code Form is subject to the terms of [the Mozilla Public License, v. 2.0](LICENSE). 
