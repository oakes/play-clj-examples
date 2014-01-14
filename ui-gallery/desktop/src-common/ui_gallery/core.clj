(ns ui-gallery.core
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen
             :renderer (stage)
             :camera (orthographic-camera))
    (let [ui-skin (skin "uiskin.json")]
      (table [(check-box "I'm a check box" ui-skin)
              :row
              (dialog "I'm a dialog" ui-skin :text "This is my content")
              :row
              [(image "clojure.png")
               :width 100
               :height 100
               :space-top 20
               :space-bottom 20]
              :row
              (select-box ["I'm a select box"
                           "I am too"
                           "So am I"]
                          ui-skin)
              :row
              (label "I'm a label" ui-skin)
              :row
              (slider {:min 1 :max 10 :step 1} ui-skin)
              :row
              (text-button "I'm a button" ui-skin)
              :row
              (text-field "I'm a text field" ui-skin)]
             :align (align :center)
             :set-fill-parent true)))
  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen)
    (draw! screen entities))
  :on-resize
  (fn [screen entities]
    (height! screen 400)
    nil)
  :on-ui-changed
  (fn [screen entities]
    (println (:actor screen))))

(defgame ui-gallery
  :on-create
  (fn [this]
    (set-screen! this main-screen)))
