(ns minicraft-online.core.desktop-launcher
  (:require [minicraft-online.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. minicraft-online "minicraft-online" 800 600)
  (Keyboard/enableRepeatEvents true))
