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
    (catch Exception _)))

(defn connect-room!
  [screen r1 r2]
  (let [rand-spot (+ 1 (rand-int (- size 3)))
        x-diff (- (:x r2) (:x r1))
        y-diff (- (:y r2) (:y r1))]
    (doseq [i (range size)
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
        (tiled-map-layer! :set-cell (+ x 1) (+ y 1) nil)))))

(defn connect-rooms!
  [screen rooms room]
  (let [visited-room (assoc room :visited? true)
        rooms (map #(if (= % room) visited-room %) rooms)]
    (if-let [next-room (get-rand-neighbor rooms room)]
      (do
        (connect-room! screen room next-room)
        (loop [rooms rooms]
          (let [new-rooms (connect-rooms! screen rooms next-room)]
            (if (= rooms new-rooms)
              rooms
              (recur new-rooms)))))
      (if (-> (filter :end? rooms) count (> 0))
        rooms
        (map #(if (= % visited-room) (assoc % :end? true) %) rooms)))))
