(ns dungeon-crawler.rooms
  (:require [clojure.set]
            [play-clj.core :refer :all]))

(def ^:const cols 4)
(def ^:const rows 4)
(def ^:const size 10)

(defn get-rand-neighbor
  [rooms {:keys [x y] :as room}]
  (try
    (->> #{(assoc room :x (- x 1))
           (assoc room :y (- y 1))
           (assoc room :x (+ x 1))
           (assoc room :y (+ y 1))}
         (clojure.set/intersection (set rooms))
         vec
         rand-nth)
    (catch Exception _ nil)))

(defn connect-rooms!
  [screen r1 r2]
  (let [rand-spot (+ 1 (rand-int (- size 3)))
        x-diff (- (:x r2) (:x r1))
        y-diff (- (:y r2) (:y r1))]
    (doall
      (for [i (range size)
            :let [x (+ (* (:x r1) size)
                       rand-spot
                       (* x-diff i))
                  y (+ (* (:y r1) size)
                       rand-spot
                       (* y-diff i))]]
        (doto (tiled-map-layer screen "walls")
          (tiled-map-layer! :set-cell x y nil)
          (tiled-map-layer! :set-cell (+ x 1) y nil)
          (tiled-map-layer! :set-cell x (+ y 1) nil)
          (tiled-map-layer! :set-cell (+ x 1) (+ y 1) nil))))))

(defn change-room!
  [screen unvisited-rooms room]
  (if-let [next-room (get-rand-neighbor unvisited-rooms room)]
    (do
      (connect-rooms! screen room next-room)
      (loop [unvisited-rooms (remove #(= % next-room) unvisited-rooms)]
        (let [remaining-rooms (change-room! screen unvisited-rooms next-room)]
          (if (= unvisited-rooms remaining-rooms)
            unvisited-rooms
            (recur remaining-rooms)))))
    unvisited-rooms))
