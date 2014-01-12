(ns minicraft.entities
  (:require [minicraft.utils :as u]
            [play-clj.core :refer :all]))

(defn create
  ([start-layer img]
    (assoc img
           :width 2
           :height 2
           :x-velocity 0
           :y-velocity 0
           :start-layer start-layer
           :min-distance 2))
  ([start-layer down up]
    (let [anim (animation u/duration [down up])]
      (assoc down
             :width 2
             :height 2
             :x-velocity 0
             :y-velocity 0
             :down anim
             :up anim
             :right anim
             :left anim
             :start-layer start-layer
             :min-distance 2)))
  ([start-layer down up stand-right walk-right]
    (let [down-flip (texture down :flip true false)
          up-flip (texture up :flip true false)
          stand-flip (texture stand-right :flip true false)
          walk-flip (texture walk-right :flip true false)]
      (assoc down
             :width 2
             :height 2
             :x-velocity 0
             :y-velocity 0
             :down (animation u/duration [down down-flip])
             :up (animation u/duration [up up-flip])
             :right (animation u/duration [stand-right walk-right])
             :left (animation u/duration [stand-flip walk-flip])
             :start-layer start-layer
             :min-distance 10))))

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

(defn animate
  [screen {:keys [up down left right x-velocity y-velocity] :as entity}]
  (let [anim (cond
               (not= y-velocity 0)
               (if (> y-velocity 0) up down)
               (not= x-velocity 0)
               (if (> x-velocity 0) right left)
               :else nil)
        img (if anim
              (animation->texture screen anim)
              entity)
        img (if (u/is-on-layer? screen entity "water")
              (texture img :set-region-height u/pixels-per-tile)
              img)]
    (assoc (merge entity img)
           :width (/ (texture! img :get-region-width) u/pixels-per-tile)
           :height (/ (texture! img :get-region-height) u/pixels-per-tile))))

(defn attack
  [entity entities])

(defn randomize-location
  [screen {:keys [width height] :as entity} entities]
  (->> (for [tile-x (range 0 (- u/map-width width))
             tile-y (range 0 (- u/map-height height))]
         (assoc entity :x tile-x :y tile-y))
       shuffle
       (drop-while #(u/is-invalid-location? screen % entities))
       first))

(defn prevent-moves
  [screen entities]
  (pmap (fn [{:keys [x y x-change y-change] :as entity}]
          (if (and (or (not= 0 x-change) (not= 0 y-change))
                   (u/is-near-entities? entity entities 1))
            (assoc entity
                   :x-velocity 0
                   :y-velocity 0
                   :x-change 0
                   :y-change 0
                   :x (- x x-change)
                   :y (- y y-change))
            entity))
        entities))

(defn order-by-latitude
  [entities]
  (sort #(if (> (:y %1) (:y %2)) -1 1) entities))
