(ns minicraft.core
  (:require [minicraft.entities :as e]
            [minicraft.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.ui :refer :all]))

(defonce manager (asset-manager))
(set-asset-manager! manager)

(defn update-screen!
  [screen entities]
  (doseq [{:keys [x y player?]} entities]
    (when player?
      (position! screen x y)))
  entities)

(defn render-if-necessary!
  [screen entities]
  ; only render the entities whose :draw-time not= 0 and :health is > 0
  (render! screen (remove #(or (= 0 (:draw-time %))
                               (<= (:health %) 0))
                          entities))
  entities)

(defn play-sounds!
  [entities]
  (doseq [{:keys [play-sound]} entities]
    (when play-sound
      (sound! play-sound :play)))
  (map #(dissoc % :play-sound) entities))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (let [screen (->> (/ 1 u/pixels-per-tile)
                      (orthogonal-tiled-map "level1.tmx")
                      (update! screen :camera (orthographic) :renderer))
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
          attack-down-image (texture sheet :set-region 48 0 16 8)
          attack-right-image (texture sheet :set-region 32 8 8 16)
          attack-images [attack-down-image
                         (texture attack-down-image :flip false true)
                         attack-right-image
                         attack-right-image]
          hit-image (texture sheet :set-region 40 8 16 16)]
      (->> (pvalues
             (apply e/create-player player-images)
             (apply e/create-attack attack-images)
             (e/create-hit hit-image)
             (take 5 (repeat (apply e/create-zombie zombie-images)))
             (take 5 (repeat (apply e/create-slime slime-images)))
             (take 20 (repeat (e/create-tree tree-image)))
             (take 10 (repeat (e/create-cactus cactus-image))))
           flatten
           (reduce #(e/randomize-locations screen %1 %2) []))))
  
  :on-render
  (fn [screen entities]
    (clear!)
    (->> entities
         (map (fn [entity]
                (->> entity
                     (e/move screen entities)
                     (e/animate screen)
                     (e/animate-attack screen entities)
                     (e/animate-hit entities)
                     (e/prevent-move entities)
                     (e/adjust-times screen))))
         e/attack-player
         (sort-by :y #(compare %2 %1))
         play-sounds!
         (render-if-necessary! screen)
         (update-screen! screen)))
  
  :on-resize
  (fn [screen entities]
    (height! screen u/vertical-tiles))
  
  :on-key-down
  (fn [{:keys [key]} entities]
    (when-let [player (find-first :player? entities)]
      (when (= key (key-code :space))
        (e/attack entities player))))
  
  :on-touch-down
  (fn [screen entities]
    (let [player (find-first :player? entities)
          min-x (/ (game :width) 3)
          max-x (* (game :width) (/ 2 3))
          min-y (/ (game :height) 3)
          max-y (* (game :height) (/ 2 3))]
      (when (and (< min-x (game :x) max-x)
                 (< min-y (game :y) max-y))
        (e/attack entities player)))))

(defscreen text-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    (assoc (label "0" (color :white))
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

(defgame minicraft
  :on-create
  (fn [this]
    (set-screen! this main-screen text-screen)))
