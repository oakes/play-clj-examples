(ns super-koalio.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.ui :refer :all]
            [super-koalio.entities :as e]
            [super-koalio.utils :as u]))

(declare super-koalio main-screen text-screen)

(defn update-screen!
  [screen entities]
  (doseq [{:keys [x y height me? to-destroy]} entities]
    (when me?
      (position! screen x (/ u/vertical-tiles 2))
      (when (< y (- height))
        (set-screen! super-koalio main-screen text-screen)))
    (when-let [[tile-x tile-y] to-destroy]
      (tiled-map-layer! (tiled-map-layer screen "walls")
                        :set-cell tile-x tile-y nil)))
  (map #(dissoc % :to-destroy) entities))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (->> (orthogonal-tiled-map "level1.tmx" (/ 1 u/pixels-per-tile))
         (update! screen :timeline [] :camera (orthographic) :renderer))
    (let [sheet (texture "koalio.png")
          tiles (texture! sheet :split 18 26)
          player-images (for [col [0 1 2 3 4]]
                          (texture (aget tiles 0 col)))]
      (apply e/create player-images)))
  
  :on-render
  (fn [screen entities]
    (clear! 0.5 0.5 1 1)
    (some->> (if (or (key-pressed? :space) (u/touched? :center))
               (rewind! screen 2)
               (map (fn [entity]
                      (->> entity
                           (e/move screen)
                           (e/prevent-move screen)
                           (e/animate screen)))
                    entities))
             (render! screen)
             (update-screen! screen)))
  
  :on-resize
  (fn [{:keys [width height] :as screen} entities]
    (height! screen u/vertical-tiles)))

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

(defgame super-koalio
  :on-create
  (fn [this]
    (set-screen! this main-screen text-screen)))
