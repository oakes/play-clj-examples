(ns dungeon-crawler.entities
  (:require [dungeon-crawler.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]))

(defn create
  [grid mask-size]
  (let [moves (zipmap u/directions
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
           :x-velocity 0
           :y-velocity 0
           :x-feet 0
           :y-feet 0
           :last-attack 0
           :attack-interval 1
           :direction start-direction
           :health 10
           :wounds 0
           :damage 2)))

(defn create-player
  []
  (let [path "characters/male_light.png"
        mask-size 128
        grid (u/split-texture path u/grid-tile-size mask-size)]
    (assoc (create grid mask-size)
           :player? true
           :max-velocity 2
           :attack-interval 0.25
           :health 40
           :hurt-sound (sound "playerhurt.wav")
           :death-sound (sound "death.wav"))))

(defn create-ogres
  [n]
  (let [path "characters/ogre.png"
        mask-size 256
        grid (u/split-texture path u/grid-tile-size mask-size)]
    (->> (assoc (create grid mask-size)
                :npc? true
                :max-velocity 1
                :x-feet 0.35
                :y-feet 0.35
                :hurt-sound (sound "monsterhurt.wav"))
         repeat
         (take n))))

(defn create-elementals
  [n]
  (let [path "characters/elemental.png"
        mask-size 256
        grid (u/split-texture path u/grid-tile-size mask-size)]
    (->> (assoc (create grid mask-size)
                :npc? true
                :max-velocity 2
                :x-feet 0.35
                :y-feet 0.35
                :hurt-sound (sound "monsterhurt.wav"))
         repeat
         (take n))))

(defn update-health-bar
  [bar entity]
  (when entity
    (let [bar-x (:x entity)
          bar-y (+ (:y entity) (:height entity))
          bar-w (:width entity)
          pct (/ (:health entity) (+ (:health entity) (:wounds entity)))]
      (shape bar
             :set-color (color :red)
             :rect bar-x bar-y bar-w u/npc-bar-h
             :set-color (color :green)
             :rect bar-x bar-y (* bar-w pct) u/npc-bar-h))))

(defn move
  [{:keys [delta-time]} entities {:keys [x y health] :as entity}]
  (let [[x-velocity y-velocity] (u/get-velocity entities entity)
        x-change (* x-velocity delta-time)
        y-change (* y-velocity delta-time)]
    (cond
      (= health 0)
      (assoc entity :x-velocity 0 :y-velocity 0)
      (or (not= 0 x-change) (not= 0 y-change))
      (assoc entity
             :x-velocity (u/decelerate x-velocity)
             :y-velocity (u/decelerate y-velocity)
             :x-change x-change
             :y-change y-change
             :x (+ x x-change)
             :y (+ y y-change))
      :else
      entity)))

(defn ^:private recover
  [{:keys [last-attack health direction] :as entity}]
  (if (and (>= last-attack 0.5) (> health 0))
    (merge entity
           (-> (get-in entity [:moves direction])
               (animation! :get-key-frame 0)
               texture))
    entity))

(defn animate
  [screen {:keys [x-velocity y-velocity] :as entity}]
  (if-let [direction (u/get-direction x-velocity y-velocity)]
    (let [anim (get-in entity [:moves direction])]
      (merge entity
             (animation->texture screen anim)
             {:direction direction}))
    (recover entity)))

(defn prevent-move
  [screen entities {:keys [x y x-change y-change] :as entity}]
  (let [old-x (- x x-change)
        old-y (- y y-change)
        x-entity (assoc entity :y old-y)
        y-entity (assoc entity :x old-x)]
    (merge entity
           (when (u/invalid-location? screen entities x-entity)
             {:x-velocity 0 :x-change 0 :x old-x})
           (when (u/invalid-location? screen entities y-entity)
             {:y-velocity 0 :y-change 0 :y old-y}))))

(defn adjust
  [{:keys [delta-time]} {:keys [last-attack attack-interval npc?] :as entity}]
  (assoc entity
         :last-attack (if (and npc? (>= last-attack attack-interval))
                        0
                        (+ last-attack delta-time))))

(defn attack
  [screen {:keys [x y x-feet y-feet damage] :as attacker} victim entities]
  (map (fn [{:keys [id direction] :as e}]
         (cond
           (= id (:id attacker))
           (let [direction (or (when victim
                                 (u/get-direction-to-entity attacker victim))
                               direction)]
             (merge e
                    {:last-attack 0
                     :direction direction}
                    (when (> (:health e) 0)
                      (get-in e [:attacks direction]))))
           (= id (:id victim))
           (if attacker
             (let [health (max 0 (- (:health victim) damage))]
               (merge e
                      {:last-attack 0
                       :health health
                       :wounds (+ (:wounds victim) damage)
                       :play-sound (if (and (= health 0) (:death-sound victim))
                                     (:death-sound victim)
                                     (:hurt-sound victim))}
                      (if (> health 0)
                        (get-in e [:hits direction])
                        (get-in e [:deads direction]))))
             e)
           :else
           e))
         entities))

(defn randomize-location
  [screen entities {:keys [width height] :as entity}]
  (->> (for [tile-x (range 0 (- u/map-width width))
             tile-y (range 0 (- u/map-height height))]
         (isometric->screen screen {:x tile-x :y tile-y}))
       shuffle
       (drop-while
         #(or (u/near-entity? (merge entity %) (find-first :player? entities) 5)
              (u/invalid-location? screen entities (merge entity %))))
       first
       (merge entity)))

(defn randomize-locations
  [screen entities entity]
  (conj entities
        (-> (if (:npc? entity)
              (randomize-location screen entities entity)
              entity)
            (assoc :id (count entities)))))
