(ns super-koalio.core
  (:require [play-clj.core :refer :all]
            [super-koalio.entities :as e]
            [super-koalio.utils :as u]))

(declare super-koalio main-screen)

(defn update-screen!
  [screen entities]
  (doseq [{:keys [x y height is-me? to-destroy]} entities]
    (when is-me?
      (move-x! screen x)
      (when (< y (- height))
        (set-screen! super-koalio main-screen)))
    (when-let [[tile-x tile-y] to-destroy]
      (tiled-map-layer! screen "walls" :set-cell tile-x tile-y nil)))
  entities)

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (->> (orthogonal-tiled-map "level1.tmx" (/ 1 u/pixels-per-tile))
         (update! screen :camera (orthographic-camera) :renderer))
    (let [sheet (texture "koalio.png")
          tiles (texture! sheet :split 18 26)
          player-images (for [col [0 1 2 3 4]]
                          (texture (aget tiles 0 col)))]
      (apply e/create player-images)))
  :on-render
  (fn [screen entities]
    (clear! 0.5 0.5 1 1)
    (render! screen)
    (->> entities
         (map #(->> %
                    (e/move screen)
                    (e/prevent-move screen)
                    (e/animate screen)))
         (draw! screen)
         (update-screen! screen)))
  :on-resize
  (fn [screen entities]
    (height! screen u/vertical-tiles)
    nil))

(defgame super-koalio
  :on-create
  (fn [this]
    (set-screen! this main-screen)))
