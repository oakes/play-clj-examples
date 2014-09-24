(defproject minimal-3d-model "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[com.badlogicgames.gdx/gdx "1.3.1"]
                 [com.badlogicgames.gdx/gdx-backend-robovm "1.3.1"]
                 [com.badlogicgames.gdx/gdx-box2d "1.3.1"]
                 [com.badlogicgames.gdx/gdx-bullet "1.3.1"]
                 [org.clojure/clojure "1.6.0"]
                 [play-clj "0.4.0-SNAPSHOT"]]
  :source-paths ["src/clojure" "../desktop/src-common"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.7" "-source" "1.7" "-Xlint:-options"]
  :ios {:robovm-opts ["-forcelinkclasses" "minimal_3d_model.**:clojure.**:com.badlogic.**:play_clj.**"
                      "-libs" "libs/libObjectAL.a:libs/libgdx.a:libs/libgdx-box2d.a:libs/libgdx-bullet.a"
                      "-frameworks" "UIKit:OpenGLES:QuartzCore:CoreGraphics:OpenAL:AudioToolbox:AVFoundation"
                      "-resources" "../desktop/resources/**"]}
  :aot :all
  :main minimal_3d_model.core.IOSLauncher)
