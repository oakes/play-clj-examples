(ns minicraft.entities
  (:require [minicraft.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]))

(defn ^:private create
  ([start-layer img] ; trees and cacti
    (assoc img
           :width 2
           :height 2
           :x-velocity 0
           :y-velocity 0
           :start-layer start-layer
           :min-distance 2
           :health 6
           :direction :down
           :hurt-sound (sound "monsterhurt.wav")))
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
             :attack-time 0)))
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
             :attack-time 0))))

(defn create-tree
  [img]
  (assoc (create "grass" img)
         :tree? true))

(defn create-cactus
  [img]
  (assoc (create "desert" img)
         :cactus? true))

(defn create-hit
  [img]
  (assoc (create nil img)
         :hit? true
         :draw-time 0))

(defn create-slime
  [down up]
  (assoc (create "grass" down up)
         :npc? true))

(defn create-attack
  [down up stand-right walk-right]
  (assoc (create nil down up stand-right walk-right)
         :attack? true
         :draw-time 0))

(defn create-zombie
  [down up stand-right walk-right]
  (assoc (create "grass" down up stand-right walk-right)
         :npc? true))

(defn create-player
  [down up stand-right walk-right]
  (assoc (create "grass" down up stand-right walk-right)
         :player? true
         :hurt-sound (sound "playerhurt.wav")
         :death-sound (sound "death.wav")
         :play-sound (sound "test.wav")))

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
  (if (u/completely-on-layer? screen entity "water")
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

(defn ^:private not-victim?
  [{:keys [x y health npc?] :as attacker} {:keys [player?] :as victim}]
  (or (= health 0)
      (not= npc? player?)
      (not (u/near-entity? attacker victim u/attack-distance))
      (case (:direction attacker)
        :down (< (- y (:y victim)) 0) ; victim is up?
        :up (> (- y (:y victim)) 0) ; victim is down?
        :right (> (- x (:x victim)) 0) ; victim is left?
        :left (< (- x (:x victim)) 0) ; victim is right?
        false)))

(defn attack
  [entities attacker]
  (let [victim (first (drop-while #(not-victim? attacker %) entities))]
    (map (fn [e]
           (cond
             (:attack? e)
             (if (and (:player? attacker) (not= (:health attacker) 0))
               (assoc e
                      :draw-time u/max-draw-time
                      :id-2 (:id attacker))
               e)
             (:hit? e)
             (if-not (:player? victim)
               (assoc e
                      :draw-time (if victim u/max-draw-time 0)
                      :id-2 (:id victim))
               e)
             (= e victim)
             (let [health (max 0 (- (:health e) (:damage attacker)))]
               (assoc e
                      :play-sound (if (and (= health 0) (:death-sound victim))
                                    (:death-sound victim)
                                    (:hurt-sound victim))
                      :health health))
             :else
             e))
         entities)))

(defn ^:private npc-attacker?
  [{:keys [npc? health attack-time] :as npc} player]
  (and npc?
       (> health 0)
       (= attack-time 0)
       (u/near-entity? npc player u/attack-distance)))

(defn attack-player
  [entities]
  (if-let [npc (find-first #(npc-attacker? % (find-first :player? entities))
                           entities)]
    (attack entities npc)
    entities))

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
      ; position the hit slightly below the victim so it appears on top
      (assoc entity :x x :y (- y 0.01))
      entity)
    entity))

(defn randomize-locations
  [screen entities {:keys [width height] :as entity}]
  (->> (for [tile-x (range 0 (- u/map-width width))
             tile-y (range 0 (- u/map-height height))]
         {:x tile-x :y tile-y})
       shuffle
       (drop-while #(u/invalid-location? screen entities (merge entity %)))
       first
       (merge entity {:id (count entities)})
       (conj entities)))

(defn prevent-move
  [entities {:keys [x y x-change y-change health] :as entity}]
  (if (or (= health 0)
          (< x 0)
          (> x (- u/map-width 1))
          (< y 0)
          (> y (- u/map-height 1))
          (and (or (not= 0 x-change) (not= 0 y-change))
               (u/near-entities? entities entity 1)))
    (assoc entity
           :x-velocity 0
           :y-velocity 0
           :x-change 0
           :y-change 0
           :x (- x x-change)
           :y (- y y-change))
    entity))

(defn adjust-times
  [{:keys [delta-time]} {:keys [draw-time attack-time] :as entity}]
  ; if times are > zero, reduce them by the screen's :delta-time
  (conj entity
        (when draw-time
          [:draw-time (max 0 (- draw-time delta-time))])
        (when attack-time
          [:attack-time (if (> attack-time 0)
                          (max 0 (- attack-time delta-time))
                          u/max-attack-time)])))
