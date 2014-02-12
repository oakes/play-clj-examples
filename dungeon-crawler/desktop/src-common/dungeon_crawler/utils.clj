(ns dungeon-crawler.utils
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]))

(def ^:const vertical-tiles 4)
(def ^:const pixels-per-tile 64)
(def ^:const duration 0.2)
(def ^:const damping 0.5)
(def ^:const deceleration 0.9)
(def ^:const map-width 40)
(def ^:const map-height 40)
(def ^:const max-draw-time 0.2)
(def ^:const max-attack-time 1)
(def ^:const aggro-distance 6)
(def ^:const attack-distance 1.5)
(def ^:const grid-tile-size 256)
(def ^:const directions [:w :nw :n :ne
                         :e :se :s :sw])
(def ^:const velocities [[-1 0] [-1 1] [0 1] [1 1]
                         [1 0] [1 -1] [0 -1] [-1 -1]])

(defn is-on-layer?
  [screen {:keys [width height] :as entity} & layer-names]
  (let [{:keys [x y]} (screen->isometric screen entity)
        layers (map #(tiled-map-layer screen %) layer-names)]
    (->> (for [tile-x (range (int x) (+ x width))
               tile-y (range (int y) (+ y height))]
           (-> (some #(tiled-map-cell screen % tile-x tile-y) layers)
               nil?
               not))
         (filter identity)
         first
         nil?
         not)))

(defn is-near-entity?
  [{:keys [x y id] :as e} e2 min-distance]
  (and (not= id (:id e2))
       (nil? (:draw-time e2))
       (> (:health e2) 0)
       (< (Math/abs ^double (- x (:x e2))) min-distance)
       (< (Math/abs ^double (- y (:y e2))) min-distance)))

(defn is-near-entities?
  [entities entity min-distance]
  (some #(is-near-entity? entity % min-distance) entities))

(defn is-invalid-location?
  [screen entities entity]
  (or (is-near-entities? entities entity (:min-distance entity))
      (is-on-layer? screen entity "walls")))

(defn decelerate
  [velocity]
  (let [velocity (* velocity deceleration)]
    (if (< (Math/abs velocity) damping)
      0
      velocity)))

(defn get-player
  [entities]
  (some #(if (:player? %) %) entities))

(defn ^:private is-touched?
  [key]
  (and (game :is-touched?)
       (case key
         :down (> (game :y) (* (game :height) (/ 2 3)))
         :up (< (game :y) (/ (game :height) 3))
         :left (< (game :x) (/ (game :width) 3))
         :right (> (game :x) (* (game :width) (/ 2 3)))
         false)))

(defn ^:private get-player-velocity
  [{:keys [x-velocity y-velocity max-velocity]}]
  [(cond
     (or (is-pressed? :dpad-left) (is-touched? :left))
     (* -1 max-velocity)
     (or (is-pressed? :dpad-right) (is-touched? :right))
     max-velocity
     :else
     x-velocity)
   (cond
     (or (is-pressed? :dpad-down) (is-touched? :down))
     (* -1 max-velocity)
     (or (is-pressed? :dpad-up) (is-touched? :up))
     max-velocity
     :else
     y-velocity)])

(defn ^:private get-npc-aggro-velocity
  [{:keys [max-velocity] :as npc} me axis]
  (let [diff (- (get npc axis) (get me axis))]
    (cond
      (> diff attack-distance) (* -1 max-velocity)
      (< diff (* -1 attack-distance)) max-velocity
      :else 0)))

(defn ^:private get-npc-velocity
  [entities
   {:keys [attack-time x y x-velocity y-velocity max-velocity] :as entity}]
  (let [me (get-player entities)]
    (if (is-near-entity? entity me aggro-distance)
      [(get-npc-aggro-velocity entity me :x)
       (get-npc-aggro-velocity entity me :y)]
      (if (= attack-time 0)
        [(* max-velocity (- (rand-int 3) 1))
         (* max-velocity (- (rand-int 3) 1))]
        [x-velocity y-velocity]))))

(defn get-velocity
  [entities {:keys [player? npc?] :as entity}]
  (cond
    player? (get-player-velocity entity)
    npc? (get-npc-velocity entities entity)
    :else [0 0]))

(defn get-direction
  [{:keys [^float x-velocity ^float y-velocity]}]
  (some->> velocities
           (filter (fn [[x y]]
                     (and (= x (int (Math/signum x-velocity)))
                          (= y (int (Math/signum y-velocity))))))
           first
           (.indexOf velocities)
           (nth directions)))

(defn find-id
  [entities id]
  (some #(if (= id (:id %)) %) entities))

(defn split-texture
  [path size mask-size]
  (let [start (/ (- size mask-size) 2)
        grid (texture! (texture path) :split size size)]
    (doseq [row grid
            item row]
      (texture! item :set-region item start start mask-size mask-size))
    grid))
