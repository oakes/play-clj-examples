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
(def ^:const aggro-distance 2)
(def ^:const attack-distance 0.5)
(def ^:const grid-tile-size 256)
(def ^:const directions [:w :nw :n :ne
                         :e :se :s :sw])
(def ^:const velocities [[-1 0] [-1 1] [0 1] [1 1]
                         [1 0] [1 -1] [0 -1] [-1 -1]])

(defn on-layer?
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

(defn near-entity?
  [{:keys [x y x-feet y-feet id] :as e} e2 min-distance]
  (and (not= id (:id e2))
       (< (Math/abs (double (- (+ x x-feet) (+ (:x e2) (:x-feet e2)))))
          min-distance)
       (< (Math/abs (double (- (+ y y-feet) (+ (:y e2) (:y-feet e2)))))
          min-distance)))

(defn near-entities?
  [entities entity min-distance]
  (some #(near-entity? entity % min-distance) entities))

(defn invalid-location?
  [screen entities entity]
  (or (near-entities? entities entity (:min-distance entity))
      (on-layer? screen entity "walls")))

(defn decelerate
  [velocity]
  (let [velocity (* velocity deceleration)]
    (if (< (Math/abs velocity) damping)
      0
      velocity)))

(defn get-player
  [entities]
  (some #(if (:player? %) %) entities))

(defn ^:private get-player-velocity
  [{:keys [x-velocity y-velocity max-velocity]}]
  (if (game :touched?)
    (let [x (float (- (game :x) (/ (game :width) 2)))
          y (float (- (/ (game :height) 2) (game :y)))
          x-adjust (* max-velocity (Math/abs (double (/ x y))))
          y-adjust (* max-velocity (Math/abs (double (/ y x))))]
      [(* (Math/signum x) (min max-velocity x-adjust))
       (* (Math/signum y) (min max-velocity y-adjust))])
    [(cond
       (key-pressed? :dpad-left) (* -1 max-velocity)
       (key-pressed? :dpad-right) max-velocity
       :else x-velocity)
     (cond
       (key-pressed? :dpad-down) (* -1 max-velocity)
       (key-pressed? :dpad-up) max-velocity
       :else y-velocity)]))

(defn ^:private get-npc-axvelocity
  [{:keys [max-velocity]} diff]
  (cond
    (> diff attack-distance) (* -1 max-velocity)
    (< diff (* -1 attack-distance)) max-velocity
    :else 0))

(defn ^:private get-npc-aggro-velocity
  [{:keys [x y x-feet y-feet] :as npc} me]
  (let [x-diff (- (+ x x-feet) (+ (:x me) (:x-feet me)))
        y-diff (- (+ y y-feet) (+ (:y me) (:y-feet me)))]
    [(get-npc-axvelocity npc x-diff)
     (get-npc-axvelocity npc y-diff)]))

(defn ^:private get-npc-velocity
  [entities {:keys [last-attack attack-interval
                    x-velocity y-velocity max-velocity]
             :as entity}]
  (let [me (get-player entities)]
    (if (near-entity? entity me aggro-distance)
      (get-npc-aggro-velocity entity me)
      (if (>= last-attack attack-interval)
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
