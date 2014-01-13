(ns minicraft.entities
  (:require [minicraft.utils :as u]
            [play-clj.core :refer :all]))

(defn create
  ([start-layer img] ; trees and cacti
    (assoc img
           :width 2
           :height 2
           :x-velocity 0
           :y-velocity 0
           :start-layer start-layer
           :min-distance 2
           :health 6))
  ([start-layer down up] ; slimes
    (let [anim (animation u/duration [down up])]
      (assoc (create start-layer down)
             :down anim
             :up anim
             :right anim
             :left anim
             :min-distance 10
             :health 8)))
  ([start-layer down up stand-right walk-right] ; zombies
    (let [down-flip (texture down :flip true false)
          up-flip (texture up :flip true false)
          stand-flip (texture stand-right :flip true false)
          walk-flip (texture walk-right :flip true false)]
      (assoc (create start-layer down)
             :down (animation u/duration [down down-flip])
             :up (animation u/duration [up up-flip])
             :right (animation u/duration [stand-right walk-right])
             :left (animation u/duration [stand-flip walk-flip])
             :min-distance 10
             :health 10)))
  ([start-layer attack down up stand-right walk-right] ; player
    (assoc (create start-layer down up stand-right walk-right)
           :is-me? true
           :attack-right attack
           :attack-left (texture attack :flip true false))))

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

(defn ^:private animate-direction
  [screen entity]
  (if-let [direction (u/get-direction entity)]
    (merge entity
           (animation->texture screen (get entity direction))
           {:direction direction})
    entity))

(defn ^:private animate-water
  [screen entity]
  (if (u/is-on-layer? screen entity "water")
    (merge entity (texture entity :set-region-height u/pixels-per-tile))
    entity))

(defn animate
  [screen entity]
  (->> entity
       (animate-direction screen)
       (animate-water screen)
       u/update-texture-size))

(defn attack
  [entity entities]
  (let [close-entities (filter #(u/is-near-entity? entity % 1.5) entities)]
    (println (count close-entities))))

(defn randomize-location
  [screen {:keys [width height] :as entity} entities]
  (->> (for [tile-x (range 0 (- u/map-width width))
             tile-y (range 0 (- u/map-height height))]
         {:x tile-x :y tile-y})
       shuffle
       (drop-while #(u/is-invalid-location? screen (merge entity %) entities))
       first
       (merge entity)))

(defn ^:private prevent-move
  [{:keys [x y x-change y-change] :as entity} entities]
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

(defn prevent-moves
  [entities]
  (pmap #(prevent-move % entities) entities))

(defn order-by-latitude
  [entities]
  (sort #(if (> (:y %1) (:y %2)) -1 1) entities))
