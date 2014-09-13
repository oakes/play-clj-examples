(ns minicraft-online.entities
  (:require [minicraft-online.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]))

(defn ^:private create
  ([start-layer img]
    (assoc img
           :x 0
           :y 0
           :width 2
           :height 2
           :x-velocity 0
           :y-velocity 0
           :start-layer start-layer
           :min-distance 2
           :health 6
           :direction :down
           :hurt-sound (sound "monsterhurt.wav")))
  ([start-layer down up]
    (let [anim (animation u/duration [down up])]
      (assoc (create start-layer down)
             :down anim
             :up anim
             :right anim
             :left anim
             :health 8
             :damage 2
             :attack-time 0)))
  ([start-layer down up stand-right walk-right]
    (let [down-flip (texture down :flip true false)
          up-flip (texture up :flip true false)
          stand-flip (texture stand-right :flip true false)
          walk-flip (texture walk-right :flip true false)]
      (assoc (create start-layer down)
             :down (animation u/duration [down down-flip])
             :up (animation u/duration [up up-flip])
             :right (animation u/duration [stand-right walk-right])
             :left (animation u/duration [stand-flip walk-flip])
             :health 20
             :damage 4
             :attack-time 0))))

(defn create-hit
  []
  (let [sheet (texture "tiles.png")
        tiles (texture! sheet :split 16 16)
        img (texture sheet :set-region 40 8 16 16)]
    (assoc (create nil img)
           :hit? true
           :draw-time 0)))

(defn create-attack
  []
  (let [sheet (texture "tiles.png")
        tiles (texture! sheet :split 16 16)
        down (texture sheet :set-region 48 0 16 8)
        up (texture down :flip false true)
        right (texture sheet :set-region 32 8 8 16)]
    (assoc (create nil down up right right)
           :attack? true
           :draw-time 0)))

(defn create-person
  []
  (let [sheet (texture "tiles.png")
        tiles (texture! sheet :split 16 16)
        player-images (for [col [0 1 2 3]]
                        (texture (aget tiles 6 col)))
        [down up stand-right walk-right] player-images]
    (assoc (create "grass" down up stand-right walk-right)
           :person? true)))

(defn create-player
  []
  (assoc (create-person)
         :player? true
         :id (rand-int Integer/MAX_VALUE)
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
  [{:keys [x y health] :as attacker} victim]
  (or (= attacker victim)
      (= health 0)
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
               (merge e
                      (if (and (= health 0) (:death-sound victim))
                        {:play-sound (:death-sound victim)
                         :death-time (System/currentTimeMillis)}
                        {:play-sound (:hurt-sound victim)})
                      {:health health}))
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
      ; position the hit slightly below the victim so it appears on top
      (assoc entity :x x :y (- y 0.01))
      entity)
    entity))

(defn randomize-location
  [screen {:keys [width height] :as entity}]
  (->> (for [tile-x (range 0 (- u/map-width width))
             tile-y (range 0 (- u/map-height height))]
         {:x tile-x :y tile-y})
       shuffle
       (drop-while #(u/invalid-location? screen [] (merge entity %)))
       first
       (merge entity)))

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

(defn update-person
  [screen entities entity attrs]
  (let [updated-person (merge entity (dissoc attrs :health)) ; we're keeping track of health locally
        entities (remove #{entity} entities)]
    (when-not (u/near-entities? entities updated-person 1)
      (conj entities updated-person))))

(defn add-person
  [screen entities attrs]
  (let [new-person (merge (create-person) attrs)]
    (when-not (u/invalid-location? screen entities new-person)
      (conj entities new-person))))

(defn inactive?
  [entity]
  (and (:last-update entity)
       (> (- (System/currentTimeMillis) (:last-update entity)) u/timeout)))
