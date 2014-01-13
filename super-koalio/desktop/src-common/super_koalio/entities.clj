(ns super-koalio.entities
  (:require [play-clj.core :refer :all]
            [super-koalio.utils :as u]))

(defn create
  [stand jump & walk]
  (assoc stand
         :stand-right stand
         :stand-left (texture stand :flip true false)
         :jump-right jump
         :jump-left (texture jump :flip true false)
         :walk-right (animation u/duration walk :loop-pingpong)
         :walk-left (animation u/duration
                               (map #(texture % :flip true false) walk)
                               :loop-pingpong)
         :width 1
         :height (/ 26 18)
         :x-velocity 0
         :y-velocity 0
         :x 20
         :y 10
         :is-me? true
         :can-jump? true
         :direction :right))

(defn move
  [{:keys [delta-time]} {:keys [x y can-jump?] :as entity}]
  (let [x-velocity (u/get-x-velocity entity)
        y-velocity (+ (u/get-y-velocity entity) u/gravity)
        x-change (* x-velocity delta-time)
        y-change (* y-velocity delta-time)]
    (if (or (not= 0 x-change) (not= 0 y-change))
      (assoc entity
             :x-velocity (u/decelerate x-velocity)
             :y-velocity (u/decelerate y-velocity)
             :x-change x-change
             :y-change y-change
             :x (+ x x-change)
             :y (+ y y-change)
             :can-jump? (if (> y-velocity 0) false can-jump?))
      entity)))

(defn animate
  [screen {:keys [x-velocity y-velocity
                  stand-right stand-left
                  jump-right jump-left
                  walk-right walk-left] :as entity}]
  (let [direction (u/get-direction entity)]
    (merge entity
           (cond
             (not= y-velocity 0)
             (if (= direction :right) jump-right jump-left)
             (not= x-velocity 0)
             (if (= direction :right)
               (animation->texture screen walk-right)
               (animation->texture screen walk-left))
             :else
             (if (= direction :right) stand-right stand-left)))))
