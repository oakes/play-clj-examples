(set! *warn-on-reflection* true)

(ns minicraft.core
  (:require [minicraft.entities :as e]
            [minicraft.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.ui :as ui]))

(defn update-screen!
  [screen entities]
  (doseq [{:keys [x y is-me?]} entities]
    (when is-me?
      (move! screen x y)))
  entities)

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (let [renderer (orthogonal-tiled-map "level1.tmx" (/ 1 u/pixels-per-tile))
          camera (orthographic-camera)
          screen (update! screen :renderer renderer :camera camera)
          sheet (texture "tiles.png")
          tiles (texture! sheet :split 16 16)
          player-images (for [col [0 1 2 3]]
                          (texture (aget tiles 6 col)))
          zombie-images (for [col [4 5 6 7]]
                          (texture (aget tiles 6 col)))
          slime-images (for [col [4 5]]
                         (texture (aget tiles 7 col)))
          tree-image (texture sheet :set-region 0 8 16 16)
          cactus-image (texture sheet :set-region 16 8 16 16)
          attack-image (texture sheet :set-region 32 8 16 16)]
      (->> (pvalues
             (apply e/create "grass" attack-image player-images)
             (take 5 (repeatedly #(apply e/create "grass" zombie-images)))
             (take 5 (repeatedly #(apply e/create "grass" slime-images)))
             (take 20 (repeatedly #(e/create "grass" tree-image)))
             (take 10 (repeatedly #(e/create "desert" cactus-image))))
           flatten
           (reduce
             (fn [entities entity]
               (conj entities (e/randomize-location screen entity entities)))
             []))))
  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen)
    (->> entities
         (pmap #(->> % (e/move screen) (e/animate screen)))
         e/prevent-moves
         e/order-by-latitude
         (draw! screen)
         (update-screen! screen)))
  :on-resize
  (fn [screen entities]
    (height! screen u/vertical-tiles)
    nil)
  :on-key-down
  (fn [{:keys [keycode]} entities]
    (let [entity (->> entities (filter :is-me?) first)]
      (cond
        (= keycode (key-code :space))
        (e/attack entity entities))))
  :on-touch-down
  (fn [{:keys [screen-x screen-y]} entities]
    (let [entity (->> entities (filter :is-me?) first)
          min-x (/ (game :width) 3)
          max-x (* (game :width) (/ 2 3))
          min-y (/ (game :height) 3)
          max-y (* (game :height) (/ 2 3))]
      (cond
        (and (< min-x screen-x max-x) (< min-y screen-y max-y))
        (e/attack entity entities)))))

(defscreen text-screen
  :on-show
  (fn [screen entities]
    (update! screen
             :renderer (stage)
             :camera (orthographic-camera))
    (assoc (ui/label "0" (color :white))
           :id :fps
           :x 5))
  :on-render
  (fn [screen entities]
    (render! screen)
    (->> (for [entity entities]
           (case (:id entity)
             :fps (doto entity (ui/label! :set-text (str (game :fps))))
             entity))
         (draw! screen)))
  :on-resize
  (fn [screen entities]
    (height! screen 300)
    nil))

(defgame minicraft
  :on-create
  (fn [this]
    (set-screen! this main-screen text-screen)))
