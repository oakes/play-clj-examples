## Introduction

An online version of minicraft. To try it out, clone [play-clj.net](https://github.com/oakes/play-clj.net) and run `lein run` inside it. This will start a server on localhost. Then, start a few different instances of this game (by running `lein run` in the desktop directory from several terminals). Each instance should now show the other instances' players, and positions should live-update when they move around.

## Contents

* `android/src` Android-specific code
* `desktop/resources` Images, audio, and other files
* `desktop/src` Desktop-specific code
* `desktop/src-common` Cross-platform game code