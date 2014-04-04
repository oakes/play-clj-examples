(set! *warn-on-reflection* true)

(ns dungeon-crawler.core
  (:require [dungeon-crawler.entities :as e]
            [dungeon-crawler.rooms :as r]
            [dungeon-crawler.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.ui :refer :all]))

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
          hurt-sound-1 (sound "playerhurt.wav")
          hurt-sound-2 (sound "monsterhurt.wav")
          death-sound (sound "death.wav")]
      (r/connect-rooms! screen rooms start-room)
      (->> [(->> (assoc (e/create-player)
                        :x start-player-x
                        :y start-player-y
                        :hurt-sound hurt-sound-1
                        :death-sound death-sound)
                 (isometric->screen screen))
            (e/create-elementals 20)
            (e/create-ogres 20)]
           flatten
           (map #(if-not (:hurt-sound %) (assoc % :hurt-sound hurt-sound-2) %))
           (reduce
             (fn [entities entity]
               (conj entities
                     (-> (if (:npc? entity)
                           (e/randomize-location screen entities entity)
                           entity)
                         (assoc :id (count entities)))))
             []))))
  :on-render
  (fn [screen entities]
    (clear!)
    (let [me (u/get-player entities)]
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
  (fn [{:keys [x y button] :as screen} entities]
    (when (= button (button-code :right))
      (let [me (u/get-player entities)
            victim (u/get-entity-at-cursor screen entities x y)
            victim (when (u/can-attack? me victim) victim)]
        (e/attack screen me victim entities))))
  :on-mouse-moved
  (fn [{:keys [x y] :as screen} entities]
    (if (u/get-entity-at-cursor screen entities x y)
      (input! :set-cursor-image (:attack-cursor screen) 0 0)
      (input! :set-cursor-image nil 0 0))))

(defscreen npc-health-screen
  :on-show
  (fn [screen entities]
    (update! screen
             :renderer (-> main-screen :screen deref :renderer)
             :camera (-> main-screen :screen deref :camera))
    (shape :filled))
  :on-render
  (fn [screen entities]
    (when-let [e (u/get-entity-at-cursor (-> main-screen :screen deref)
                                         (-> main-screen :entities deref)
                                         (game :x)
                                         (game :y))]
      (let [bar-x (:x e)
            bar-y (+ (:y e) (:height e))
            bar-w (:width e)
            pct (/ (:health e) (+ (:health e) (:wounds e)))]
        (->> (shape (first entities)
                    :set-color (color :red)
                    :rect bar-x bar-y bar-w u/npc-bar-h
                    :set-color (color :green)
                    :rect bar-x bar-y (* bar-w pct) u/npc-bar-h)
             vector
             (render! screen))))))

(defscreen overlay-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    [(assoc (label "0" (color :white))
            :id :fps
            :x 5)
     (assoc (shape :filled)
            :id :bar)])
  :on-render
  (fn [screen entities]
    (->> (for [entity entities]
           (case (:id entity)
             :fps (doto entity (label! :set-text (str (game :fps))))
             :bar (let [me (u/get-player (-> main-screen :entities deref))
                        pct (/ (:health me) (+ (:health me) (:wounds me)))]
                    (shape entity
                           :set-color (color :red)
                           :rect u/bar-x u/bar-y u/bar-w u/bar-h
                           :set-color (color :green)
                           :rect u/bar-x u/bar-y u/bar-w (* u/bar-h pct)))
             entity))
         (render! screen)))
  :on-resize
  (fn [screen entities]
    (height! screen 300)))

(defgame dungeon-crawler
  :on-create
  (fn [this]
    (set-screen! this main-screen npc-health-screen overlay-screen)))
