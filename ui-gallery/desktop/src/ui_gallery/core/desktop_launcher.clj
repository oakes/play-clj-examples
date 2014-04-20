(ns ui-gallery.core.desktop-launcher
  (:require [ui-gallery.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. ui-gallery "ui-gallery" 800 600)
  (Keyboard/enableRepeatEvents true))
