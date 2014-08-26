(ns minimal-3d-model.core
  (:require [play-clj.core :refer :all]
            [play-clj.g3d :refer :all]
            [play-clj.math :refer :all]
            [play-clj.ui :refer :all]))

(def manager (asset-manager))
(set-asset-manager! manager)

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen
             :renderer (model-batch)
             :camera (doto (perspective 75 (game :width) (game :height))
                       (position! 0 0 20)
                       (direction! 0 0 0)
                       (near! 0.1)
                       (far! 300)))
    (model "knight.g3dj"))
  
  :on-render
  (fn [screen entities]
    (clear! 1 1 1 1)
    (doto screen
      (perspective! :rotate-around (vector-3 0 0 0) (vector-3 0 1 0) 1)
      (perspective! :update))
    (render! screen entities)))

(defscreen text-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    (assoc (label "0" (color :black))
           :id :fps
           :x 5))
  
  :on-render
  (fn [screen entities]
    (->> (for [entity entities]
           (case (:id entity)
             :fps (doto entity (label! :set-text (str (game :fps))))
             entity))
         (render! screen)))
  
  :on-resize
  (fn [screen entities]
    (height! screen 300)))

(defgame minimal-3d-model
  :on-create
  (fn [this]
    (set-screen! this main-screen text-screen)))
