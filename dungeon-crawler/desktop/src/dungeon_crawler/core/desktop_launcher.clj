(ns dungeon-crawler.core.desktop-launcher
  (:require [dungeon-crawler.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. dungeon-crawler "dungeon-crawler" 800 600)
  (Keyboard/enableRepeatEvents true))
