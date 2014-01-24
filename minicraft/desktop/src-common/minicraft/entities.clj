(ns minicraft.entities
  (:require [minicraft.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]))

(defn create
  ([start-layer img] ; trees and cacti
    (assoc img
           :width 2
           :height 2
           :x-velocity 0
           :y-velocity 0
           :start-layer start-layer
           :min-distance 2
           :health 6
           :direction :down))
  ([start-layer down up] ; slimes
    (let [anim (animation u/duration [down up])]
      (assoc (create start-layer down)
             :down anim
             :up anim
             :right anim
             :left anim
             :min-distance 10
             :health 8
             :damage 2
             :npc? true)))
  ([start-layer down up stand-right walk-right] ; player and zombies
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
             :health 10
             :damage 4
             :npc? true))))

(defn move
  [{:keys [delta-time]} entities {:keys [x y] :as entity}]
  (let [[x-velocity y-velocity] (u/get-velocity entities entity)
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
    (if-let [anim (get entity direction)]
      (merge entity
             (animation->texture screen anim)
             {:direction direction})
      entity)
    entity))

(defn ^:private animate-water
  [screen entity]
  (if (u/is-on-layer? screen entity "water")
    (merge entity (texture entity :set-region-height u/pixels-per-tile))
    entity))

(defn ^:private update-texture-size
  [entity]
  (assoc entity
         :width (/ (texture! entity :get-region-width) u/pixels-per-tile)
         :height (/ (texture! entity :get-region-height) u/pixels-per-tile)))

(defn animate
  [screen entity]
  (->> entity
       (animate-direction screen)
       (animate-water screen)
       update-texture-size))

(defn ^:private is-not-victim?
  [{:keys [x y] :as attacker} victim]
  (or (not (u/is-near-entity? attacker victim 1.5))
      (case (:direction attacker)
        :down (< (- y (:y victim)) 0) ; victim is up?
        :up (> (- y (:y victim)) 0) ; victim is down?
        :right (> (- x (:x victim)) 0) ; victim is left?
        :left (< (- x (:x victim)) 0) ; victim is right?
        false)))

(defn attack
  [entities entity]
  (let [victim (first (drop-while #(is-not-victim? entity %) entities))]
    (map (fn [e]
           (cond
             (:attack? e)
             (assoc e :draw-time u/draw-time :id-2 (:id entity))
             (:hit? e)
             (assoc e :draw-time (if victim u/draw-time 0) :id-2 (:id victim))
             (= e victim)
             (assoc e :health (max 0 (- (:health e) (:damage entity))))
             :else
             e))
         entities)))

(defn animate-attack
  [screen entities entity]
  (if (:attack? entity)
    (if-let [{:keys [x y direction]} (u/find-id entities (:id-2 entity))]
      (merge entity
             (animation->texture screen (get entity direction))
             {:x (case direction
                   :right (+ x 2)
                   :left (- x 1)
                   x)
              :y (case direction
                   :down (- y 1)
                   :up (+ y 2)
                   y)})
      entity)
    entity))

(defn animate-hit
  [entities entity]
  (if (:hit? entity)
    (if-let [{:keys [x y]} (u/find-id entities (:id-2 entity))]
      (assoc entity :x x :y y)
      entity)
    entity))

(defn randomize-location
  [screen entities {:keys [width height] :as entity}]
  (->> (for [tile-x (range 0 (- u/map-width width))
             tile-y (range 0 (- u/map-height height))]
         {:x tile-x :y tile-y})
       shuffle
       (drop-while #(u/is-invalid-location? screen entities (merge entity %)))
       first
       (merge entity {:id (count entities)})))

(defn prevent-move
  [entities {:keys [x y x-change y-change] :as entity}]
  (if (and (or (not= 0 x-change) (not= 0 y-change))
           (u/is-near-entities? entities entity 1))
    (assoc entity
           :x-velocity 0
           :y-velocity 0
           :x-change 0
           :y-change 0
           :x (- x x-change)
           :y (- y y-change))
    entity))
