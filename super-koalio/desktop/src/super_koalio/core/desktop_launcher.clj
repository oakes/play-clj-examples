(ns super-koalio.core.desktop-launcher
  (:require [super-koalio.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. super-koalio "super-koalio" 800 600)
  (Keyboard/enableRepeatEvents true))
