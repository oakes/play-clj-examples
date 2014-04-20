(ns minimal-3d.core.desktop-launcher
  (:require [minimal-3d.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. minimal-3d "minimal-3d" 800 600)
  (Keyboard/enableRepeatEvents true))
