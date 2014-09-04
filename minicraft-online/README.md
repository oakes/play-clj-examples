## Introduction

An online version of minicraft. If you see no other players, try starting a few different instances of this game (by running `lein run` in the desktop directory from several terminals). Each instance should now show the other instances' players, and positions should live-update when they move around.

## Contents

* `android/src` Android-specific code
* `desktop/resources` Images, audio, and other files
* `desktop/src` Desktop-specific code
* `desktop/src-common` Cross-platform game code
* `ios/src` iOS-specific code

## Building

All projects can be built using [Nightcode](https://nightcode.info/), or on the command line using [Leiningen](https://github.com/technomancy/leiningen) with the [lein-droid](https://github.com/clojure-android/lein-droid) and [lein-fruit](https://github.com/oakes/lein-fruit) plugins.
