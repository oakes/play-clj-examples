(ns ui-gallery.core
  (:require [play-clj.core :refer :all]
            [play-clj.ui :as ui]))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen
             :renderer (stage)
             :camera (orthographic-camera))
    (let [ui-skin (ui/skin "uiskin.json")]
      (ui/table [(ui/check-box "I'm a check box" ui-skin)
                 :row
                 (ui/dialog "I'm a dialog" ui-skin :text "This is my content")
                 :row
                 [(ui/image "clojure.png")
                  :width 100
                  :height 100
                  :space-top 20
                  :space-bottom 20]
                 :row
                 (ui/select-box ["I'm a select box"
                                 "I am too"
                                 "So am I"]
                                ui-skin)
                 :row
                 (ui/label "I'm a label" ui-skin)
                 :row
                 (ui/slider {:min 1 :max 10 :step 1} ui-skin)
                 :row
                 (ui/text-button "I'm a button" ui-skin)
                 :row
                 (ui/text-field "I'm a text field" ui-skin)]
                :align (ui/align :center)
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
