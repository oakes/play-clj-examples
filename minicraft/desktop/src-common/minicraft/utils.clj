(ns minicraft.utils
  (:require [play-clj.core :refer :all]))

(def ^:const vertical-tiles 20)
(def ^:const pixels-per-tile 8)
(def ^:const duration 0.2)
(def ^:const damping 0.5)
(def ^:const max-velocity 5)
(def ^:const deceleration 0.9)
(def ^:const map-width 50)
(def ^:const map-height 50)
(def ^:const background-layer "grass")
(def ^:const draw-count 30)

(defn decelerate
  [velocity]
  (let [velocity (* velocity deceleration)]
    (if (< (Math/abs velocity) damping)
      0
      velocity)))

(defn ^:private is-touched?
  [key]
  (and (game :is-touched?)
       (case key
         :down (> (game :y) (* (game :height) (/ 2 3)))
         :up (< (game :y) (/ (game :height) 3))
         :left (< (game :x) (/ (game :width) 3))
         :right (> (game :x) (* (game :width) (/ 2 3)))
         false)))

(defn get-x-velocity
  [{:keys [is-me? x-velocity]}]
  (if is-me?
    (cond
      (or (is-pressed? :dpad-left) (is-touched? :left))
      (* -1 max-velocity)
      (or (is-pressed? :dpad-right) (is-touched? :right))
      max-velocity
      :else
      x-velocity)
    x-velocity))

(defn get-y-velocity
  [{:keys [is-me? y-velocity]}]
  (if is-me?
    (cond
      (or (is-pressed? :dpad-down) (is-touched? :down))
      (* -1 max-velocity)
      (or (is-pressed? :dpad-up) (is-touched? :up))
      max-velocity
      :else
      y-velocity)
    y-velocity))

(defn get-direction
  [{:keys [x-velocity y-velocity]}]
  (cond
    (not= y-velocity 0)
    (if (> y-velocity 0) :up :down)
    (not= x-velocity 0)
    (if (> x-velocity 0) :right :left)
    :else nil))

(defn is-on-layer?
  [screen {:keys [x y width height]} & layer-names]
  (let [layers (map #(tiled-map-layer screen %) layer-names)]
    (->> (for [tile-x (range (int x) (+ x width))
               tile-y (range (int y) (+ y height))]
           (-> (some #(tiled-map-cell screen % tile-x tile-y) layers)
               nil?
               not))
         (drop-while identity)
         first
         nil?)))

(defn ^:private tiled-map-layer-names
  [screen]
  (for [layer (tiled-map-layers screen)]
    (tiled-map-layer! layer :get-name)))

(defn is-on-start-layer?
  [screen {:keys [start-layer] :as entity}]
  (->> (for [layer-name (tiled-map-layer-names screen)]
         (or (= layer-name background-layer)
             (= (is-on-layer? screen entity layer-name)
                (= layer-name start-layer))))
       (drop-while identity)
       first
       nil?))

(defn is-near-entity?
  [{:keys [x y id] :as e} e2 min-distance]
  (and (not= id (:id e2))
       (nil? (:draw-count e2))
       (< (Math/abs ^double (- x (:x e2))) min-distance)
       (< (Math/abs ^double (- y (:y e2))) min-distance)))

(defn is-near-entities?
  [entities entity min-distance]
  (some #(is-near-entity? entity % min-distance) entities))

(defn is-invalid-location?
  [screen entities entity]
  (or (not (is-on-start-layer? screen entity))
      (is-near-entities? entities entity (:min-distance entity))))

(defn find-id
  [entities id]
  (some #(if (= id (:id %)) %) entities))
