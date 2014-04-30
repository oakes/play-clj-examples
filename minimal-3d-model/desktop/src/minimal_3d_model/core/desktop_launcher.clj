(ns minimal-3d-model.core.desktop-launcher
  (:require [minimal-3d-model.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. minimal-3d-model "minimal-3d-model" 800 600)
  (Keyboard/enableRepeatEvents true))
