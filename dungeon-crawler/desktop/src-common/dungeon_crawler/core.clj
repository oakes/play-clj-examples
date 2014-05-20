(set! *warn-on-reflection* true)

(ns dungeon-crawler.core
  (:require [dungeon-crawler.entities :as e]
            [dungeon-crawler.rooms :as r]
            [dungeon-crawler.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.ui :refer :all]))

(declare dungeon-crawler main-screen npc-health-screen overlay-screen)

(defonce manager (asset-manager))
(set-asset-manager! manager)

(defn update-screen!
  [screen entities]
  (doseq [{:keys [x y player?]} entities]
    (when player?
      (position! screen x y)))
  entities)

(defn play-sounds!
  [entities]
  (doseq [{:keys [play-sound]} entities]
    (when play-sound
      (sound! play-sound :play)))
  (map #(dissoc % :play-sound) entities))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (let [screen (->> (/ 1 u/pixels-per-tile)
                      (isometric-tiled-map "level1.tmx")
                      (update! screen
                               :attack-cursor (pixmap "dwarven_gauntlet.png")
                               :camera (orthographic)
                               :renderer))
          start-room {:x (rand-int r/rows)
                      :y (rand-int r/cols)}
          start-player-x (+ (* (:x start-room) r/size)
                            (/ r/size 2))
          start-player-y (+ (* (:y start-room) r/size)
                            (/ r/size 2))
          rooms (for [row (range r/rows)
                      col (range r/cols)]
                  {:x row :y col})
          me (assoc (e/create-player)
                    :x start-player-x
                    :y start-player-y)]
      (r/connect-rooms! screen rooms start-room)
      (->> [(isometric->screen screen me)
            (e/create-elementals 20)
            (e/create-ogres 20)]
           flatten
           (reduce #(e/randomize-locations screen %1 %2) []))))
  
  :on-render
  (fn [screen entities]
    (clear!)
    (let [me (u/get-player entities)]
      ; update health bars
      (->> (some #(if (= (:id %) (:mouse-npc-id screen)) %) entities)
           (run! npc-health-screen :on-update-health-bar :entity))
      (run! overlay-screen :on-update-health-bar :entity me)
      ; run game logic
      (->> entities
           (map (fn [entity]
                  (->> entity
                       (e/move screen entities)
                       (e/animate screen)
                       (e/prevent-move screen entities)
                       (e/adjust screen))))
           (e/attack screen (some #(if (u/can-attack? % me) %) entities) me)
           play-sounds!
           (render-sorted! screen u/sort-entities ["walls"])
           (update-screen! screen))))
  
  :on-resize
  (fn [screen entities]
    (height! screen u/vertical-tiles))
  
  :on-touch-down
  (fn [{:keys [input-x input-y button] :as screen} entities]
    (when (= button (button-code :right))
      (let [me (u/get-player entities)
            victim (u/get-entity-at-cursor screen entities input-x input-y)
            victim (when (u/can-attack? me victim) victim)]
        (e/attack screen me victim entities))))
  
  :on-mouse-moved
  (fn [{:keys [input-x input-y] :as screen} entities]
    (let [e (u/get-entity-at-cursor screen entities input-x input-y)]
      (input! :set-cursor-image (if e (:attack-cursor screen) nil) 0 0)
      (update! screen :mouse-npc-id (:id e))
      nil)))

(defscreen npc-health-screen
  :on-show
  (fn [screen entities]
    (shape :filled))
  
  :on-render
  (fn [screen entities]
    ; draw on main-screen so we can use its coordinate system
    (draw! (-> main-screen :screen deref) entities))
  
  ; custom function that is invoked in main-screen
  :on-update-health-bar
  (fn [screen entities]
    (if-let [e (:entity screen)]
      (let [bar-x (:x e)
            bar-y (+ (:y e) (:height e))
            bar-w (:width e)
            pct (/ (:health e) (+ (:health e) (:wounds e)))]
        (shape (first entities)
               :set-color (color :red)
               :rect bar-x bar-y bar-w u/npc-bar-h
               :set-color (color :green)
               :rect bar-x bar-y (* bar-w pct) u/npc-bar-h))
      (shape (first entities)))))

(defscreen overlay-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    [(assoc (label "0" (color :white))
            :id :fps
            :x 5)
     (assoc (shape :filled)
            :id :bar
            :x 5
            :y 40)
     ; this is meant for testing particle effects
     (comment assoc (particle-effect "particles/fire.p")
            :id :particle)])
  
  :on-render
  (fn [screen entities]
    (->> (for [entity entities]
           (case (:id entity)
             :fps (doto entity (label! :set-text (str (game :fps))))
             entity))
         (render! screen)))
  
  :on-resize
  (fn [screen entities]
    (height! screen 300)
    (for [e entities]
      (case (:id e)
        :particle (assoc e :x (width screen) :y 0)
        e)))
  
  ; custom function that is invoked in main-screen
  :on-update-health-bar
  (fn [screen entities]
    (for [entity entities]
      (case (:id entity)
        :bar (let [me (:entity screen)
                   pct (/ (:health me) (+ (:health me) (:wounds me)))]
               (shape entity
                      :set-color (color :red)
                      :rect 0 0 u/bar-w u/bar-h
                      :set-color (color :green)
                      :rect 0 0 u/bar-w (* u/bar-h pct)))
        entity))))

(defgame dungeon-crawler
  :on-create
  (fn [this]
    (set-screen! this main-screen npc-health-screen overlay-screen)))
