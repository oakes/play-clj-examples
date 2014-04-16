(ns minimal-3d-physics.core.desktop-launcher
  (:require [minimal-3d-physics.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. minimal-3d-physics "minimal-3d-physics" 800 600)
  (Keyboard/enableRepeatEvents true))
