(ns dungeon-crawler.utils
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.math :refer :all]))

(def ^:const vertical-tiles 4)
(def ^:const pixels-per-tile 64)
(def ^:const duration 0.2)
(def ^:const damping 0.5)
(def ^:const deceleration 0.9)
(def ^:const map-width 40)
(def ^:const map-height 40)
(def ^:const aggro-distance 2)
(def ^:const attack-distance 0.25)
(def ^:const grid-tile-size 256)
(def ^:const directions [:w :nw :n :ne
                         :e :se :s :sw])
(def ^:const velocities [[-1 0] [-1 1] [0 1] [1 1]
                         [1 0] [1 -1] [0 -1] [-1 -1]])

(def ^:const bar-w 20)
(def ^:const bar-h 80)
(def ^:const npc-bar-h 0.1)

(defn on-layer?
  [screen {:keys [width height] :as entity} & layer-names]
  (let [{:keys [x y]} (screen->isometric screen entity)
        layers (map #(tiled-map-layer screen %) layer-names)]
    (->> (for [tile-x (range (int x) (+ x width))
               tile-y (range (int y) (+ y height))]
           (-> (some #(tiled-map-cell % tile-x tile-y) layers)
               nil?
               not))
         (filter identity)
         first
         nil?
         not)))

(defn ^com.badlogic.gdx.math.Rectangle entity-rect
  [{:keys [x y x-feet y-feet width height]} min-distance]
  (rectangle (- (+ x x-feet)
                (/ min-distance 4))
             (- (+ y y-feet)
                (/ min-distance 4))
             (- (+ width (/ min-distance 2))
                (* 2 x-feet))
             (- (+ height (/ min-distance 2))
                (* 2 y-feet))))

(defn near-entity?
  [e e2 min]
  (and (not= (:id e) (:id e2))
       (> (:health e2) 0)
       (rectangle! (entity-rect e min) :overlaps (entity-rect e2 min))))

(defn near-entities?
  [entities entity min-distance]
  (some #(near-entity? entity % min-distance) entities))

(defn invalid-location?
  [screen entities entity]
  (or (near-entities? entities entity 0)
      (on-layer? screen entity "walls")))

(defn decelerate
  [velocity]
  (let [velocity (* velocity deceleration)]
    (if (< (Math/abs velocity) damping)
      0
      velocity)))

(defn ^:private get-player-velocity
  [{:keys [x-velocity y-velocity max-velocity]}]
  (if (and (game :touched?) (button-pressed? :left))
    (let [x (float (- (game :x) (/ (game :width) 2)))
          y (float (- (game :y) (/ (game :height) 2)))
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

(defn ^:private get-npc-axis-velocity
  [{:keys [max-velocity]} diff]
  (cond
    (> diff attack-distance) (* -1 max-velocity)
    (< diff (* -1 attack-distance)) max-velocity
    :else 0))

(defn ^:private get-npc-aggro-velocity
  [npc me]
  (let [r1 (entity-rect npc attack-distance)
        r2 (entity-rect me attack-distance)
        x-diff (- (rectangle! r1 :get-x) (rectangle! r2 :get-x))
        y-diff (- (rectangle! r1 :get-y) (rectangle! r2 :get-y))]
    (if-not (rectangle! r1 :overlaps r2)
      [(get-npc-axis-velocity npc x-diff)
       (get-npc-axis-velocity npc y-diff)]
      [0 0])))

(defn ^:private get-npc-velocity
  [entities {:keys [last-attack attack-interval
                    x-velocity y-velocity max-velocity]
             :as entity}]
  (let [me (find-first :player? entities)]
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
  [x-velocity y-velocity]
  (some->> velocities
           (filter (fn [[x y]]
                     (and (= x (int (Math/signum (float x-velocity))))
                          (= y (int (Math/signum (float y-velocity)))))))
           first
           (.indexOf velocities)
           (nth directions)))

(defn get-direction-to-entity
  [{:keys [x y x-feet y-feet last-direction] :as e} e2]
  (or (get-direction (- (+ (:x e2) (:x-feet e2)) (+ x x-feet))
                     (- (+ (:y e2) (:y-feet e2)) (+ y y-feet)))
      last-direction))

(defn find-id
  [entities id]
  (find-first #(= id (:id %)) entities))

(defn split-texture
  [path size mask-size]
  (let [start (/ (- size mask-size) 2)
        grid (texture! (texture path) :split size size)]
    (doseq [row grid
            item row]
      (texture! item :set-region item start start mask-size mask-size))
    grid))

(defn can-attack?
  [e e2]
  (and e2
       (not= (:npc? e) (:npc? e2))
       (> (:health e) 0)
       (>= (:last-attack e) (:attack-interval e))
       (near-entity? e e2 attack-distance)))

(defn get-entity-at-cursor
  [screen entities]
  (let [coords (input->screen screen (input! :get-x) (input! :get-y))]
    (find-first (fn [{:keys [x y width height npc? health] :as entity}]
                  (-> (rectangle x y width height)
                      (rectangle! :contains (:x coords) (:y coords))
                      (and npc? (> health 0))))
                entities)))
