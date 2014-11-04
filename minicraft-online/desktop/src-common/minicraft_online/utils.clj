(ns minicraft-online.utils
  (:require [play-clj.core :refer :all]))

(def ^:const vertical-tiles 20)
(def ^:const pixels-per-tile 8)
(def ^:const duration 0.2)
(def ^:const damping 0.5)
(def ^:const max-velocity 5)
(def ^:const max-velocity-npc 3)
(def ^:const deceleration 0.9)
(def ^:const map-width 50)
(def ^:const map-height 50)
(def ^:const background-layer "grass")
(def ^:const max-draw-time 0.2)
(def ^:const max-attack-time 1)
(def ^:const aggro-distance 6)
(def ^:const attack-distance 1.5)
(def ^:const death-delay 2000)
(def ^:const timeout death-delay)

(defn completely-on-layer?
  [screen {:keys [x y width height]} & layer-names]
  (let [layers (map #(tiled-map-layer screen %) layer-names)]
    (->> (for [tile-x (range (int x) (+ x width))
               tile-y (range (int y) (+ y height))]
           (-> (some #(tiled-map-cell % tile-x tile-y) layers)
               nil?
               not))
         (drop-while identity)
         first
         nil?)))

(defn on-start-layer?
  [screen {:keys [start-layer] :as entity}]
  (->> (for [layer-name (map-layer-names screen)]
         (or (= layer-name background-layer)
             (= (completely-on-layer? screen entity layer-name)
                (= layer-name start-layer))))
       (drop-while identity)
       first
       nil?))

(defn near-entity?
  [{:keys [x y id] :as e} e2 min-distance]
  (and (not= id (:id e2))
       (nil? (:draw-time e2))
       (> (:health e2) 0)
       (< (Math/abs ^double (- x (:x e2))) min-distance)
       (< (Math/abs ^double (- y (:y e2))) min-distance)))

(defn near-entities?
  [entities entity min-distance]
  (some #(near-entity? entity % min-distance) entities))

(defn invalid-location?
  [screen entities entity]
  (or (not (on-start-layer? screen entity))
      (near-entities? entities entity (:min-distance entity))))

(defn decelerate
  [velocity]
  (let [velocity (* velocity deceleration)]
    (if (< (Math/abs velocity) damping)
      0
      velocity)))

(defn ^:private touched?
  [key]
  (and (game :touched?)
       (case key
         :down (< (game :y) (/ (game :height) 3))
         :up (> (game :y) (* (game :height) (/ 2 3)))
         :left (< (game :x) (/ (game :width) 3))
         :right (> (game :x) (* (game :width) (/ 2 3)))
         false)))

(defn ^:private get-player-velocity
  [{:keys [x-velocity y-velocity]}]
  [(cond
     (or (key-pressed? :dpad-left) (touched? :left))
     (* -1 max-velocity)
     (or (key-pressed? :dpad-right) (touched? :right))
     max-velocity
     :else
     x-velocity)
   (cond
     (or (key-pressed? :dpad-down) (touched? :down))
     (* -1 max-velocity)
     (or (key-pressed? :dpad-up) (touched? :up))
     max-velocity
     :else
     y-velocity)])

(defn get-velocity
  [entities {:keys [player? person?] :as entity}]
  (cond
    player? (get-player-velocity entity)
    person? [(:x-velocity entity) (:y-velocity entity)]
    :else [0 0]))

(defn get-direction
  [{:keys [x-velocity y-velocity]}]
  (cond
    (not= y-velocity 0)
    (if (> y-velocity 0) :up :down)
    (not= x-velocity 0)
    (if (> x-velocity 0) :right :left)
    :else nil))

(defn find-id
  [entities id]
  (find-first #(= id (:id %)) entities))
