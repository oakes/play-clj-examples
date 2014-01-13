(ns super-koalio.entities
  (:require [play-clj.core :refer :all]
            [super-koalio.utils :as u]))

(defn create
  [walk jump & run]
  (assoc walk
         :walk walk
         :jump jump
         :run (animation u/duration run :loop-pingpong)
         :x 20
         :y 10
         :width 1
         :height (/ 26 18)))

(defn move
  [{:keys [delta-time]} {:keys [x y] :as entity}]
  (let [x-velocity (u/get-x-velocity entity)
        y-velocity (u/get-y-velocity entity)
        x-change (* x-velocity delta-time)
        y-change (* y-velocity delta-time)]
    (if (or (not= 0 x-change) (not= 0 y-change))
      (assoc entity
             :x-velocity (u/decelerate x-velocity)
             :y-velocity (u/decelerate y-velocity)
             :x-change x-change
             :y-change y-change
             :x (+ x x-change)
             :y (+ y y-change))
      entity)))
