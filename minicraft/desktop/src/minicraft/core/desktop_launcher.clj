(ns minicraft.core.desktop-launcher
  (:require [minicraft.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. minicraft "minicraft" 800 600)
  (Keyboard/enableRepeatEvents true))
