(ns dungeon-crawler.entities
  (:require [dungeon-crawler.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]))

(defn create
  [path mask-size]
  (let [grid (u/split-texture path u/grid-tile-size mask-size)
        moves (zipmap u/directions
                      (map #(animation u/duration (take 4 %)) grid))
        attacks (zipmap u/directions (map #(texture (nth % 4)) grid))
        specials (zipmap u/directions (map #(texture (nth % 5)) grid))
        hits (zipmap u/directions (map #(texture (nth % 6)) grid))
        deads (zipmap u/directions (map #(texture (nth % 7)) grid))
        texture-size (/ mask-size u/grid-tile-size)
        start-direction :s]
    (assoc (texture (get-in grid [(.indexOf u/directions start-direction) 0]))
           :width texture-size
           :height texture-size
           :moves moves
           :attacks attacks
           :specials specials
           :hits hits
           :deads deads
           :min-distance 0.25
           :x-velocity 0
           :y-velocity 0
           :x-feet 0
           :y-feet 0
           :last-attack 0
           :attack-interval 1
           :direction start-direction)))

(defn create-player
  []
  (assoc (create "characters/male_light.png" 128)
         :player? true
         :max-velocity 2
         :attack-interval 0.25))

(defn create-ogre
  []
  (assoc (create "characters/ogre.png" 256)
         :npc? true
         :max-velocity 1
         :x-feet 0.25
         :y-feet 0.25))

(defn create-elemental
  []
  (assoc (create "characters/elemental.png" 256)
         :npc? true
         :max-velocity 2
         :x-feet 0.25
         :y-feet 0.25))

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

(defn animate
  [screen entity]
  (if-let [direction (u/get-direction entity)]
    (if-let [anim (get-in entity [:moves direction])]
      (merge entity
             (animation->texture screen anim)
             {:direction direction})
      entity)
    entity))

(defn prevent-move
  [screen entities {:keys [x y x-change y-change] :as entity}]
  (let [old-x (- x x-change)
        old-y (- y y-change)
        x-entity (assoc entity :y old-y)
        y-entity (assoc entity :x old-x)]
    (merge entity
           (when (u/is-invalid-location? screen entities x-entity)
             {:x-velocity 0 :x-change 0 :x old-x})
           (when (u/is-invalid-location? screen entities y-entity)
             {:y-velocity 0 :y-change 0 :y old-y}))))

(defn adjust
  [{:keys [delta-time]} {:keys [last-attack attack-interval npc?] :as entity}]
  (assoc entity
         :last-attack (if (and npc? (>= last-attack attack-interval))
                        0
                        (+ last-attack delta-time))))

(defn randomize-location
  [screen entities {:keys [width height] :as entity}]
  (->> (for [tile-x (range 0 (- u/map-width width))
             tile-y (range 0 (- u/map-height height))]
         (isometric->screen screen {:x tile-x :y tile-y}))
       shuffle
       (drop-while
         #(or (u/is-near-entity? (merge entity %) (u/get-player entities) 5)
              (u/is-invalid-location? screen entities (merge entity %))))
       first
       (merge entity)))
