(ns minicraft-online.core
  (:require [minicraft-online.entities :as e]
            [minicraft-online.utils :as u]
            [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.net :refer :all]
            [play-clj.ui :refer :all]
            [schema.core :as s]))

(declare minicraft-online main-screen text-screen)

(defonce manager (asset-manager))
(set-asset-manager! manager)

(def topics {:minicraft-update {:id s/Num
                                :health s/Num
                                :x s/Num
                                :y s/Num
                                :x-velocity s/Num
                                :y-velocity s/Num}
             :minicraft-attack {:id s/Num}})

(defn broadcast-player!
  [screen player topic]
  (let [send-keys (-> topics (get topic) keys)]
    (broadcast! screen topic (select-keys player send-keys))))

(defn update-screen!
  [screen entities]
  (doseq [{:keys [x y player?]} entities]
    (when player?
      (position! screen x y)))
  entities)

(defn render-if-necessary!
  [screen entities]
  ; only render the entities whose :draw-time not= 0 and :health is > 0
  (render! screen (remove #(or (= 0 (:draw-time %))
                               (<= (:health %) 0))
                          entities))
  entities)

(defn play-sounds!
  [entities]
  (doseq [{:keys [play-sound]} entities]
    (when play-sound
      (sound! play-sound :play)))
  (map #(dissoc % :play-sound) entities))

(defn restart-if-dead!
  [player]
  (when (some->> (:death-time player)
                 (- (System/currentTimeMillis))
                 (< u/death-delay))
    (set-screen! minicraft-online main-screen text-screen)))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (let [renderer (orthogonal-tiled-map "level1.tmx" (/ 1 u/pixels-per-tile))
          network (client screen (keys topics))
          screen (update! screen
                          :camera (orthographic)
                          :renderer renderer
                          :network network)]
      [(e/randomize-location screen (e/create-player))
       (e/create-attack)
       (e/create-hit)]))
  
  :on-render
  (fn [screen entities]
    (let [player (find-first :player? entities)]
      (broadcast-player! screen player :minicraft-update)
      (restart-if-dead! player))
    (screen! text-screen :on-update-player-count
             :count (count (filter :person? entities)))
    (clear!)
    (->> entities
         (map (fn [entity]
                (->> entity
                     (e/move screen entities)
                     (e/animate screen)
                     (e/animate-attack screen entities)
                     (e/animate-hit entities)
                     (e/prevent-move entities)
                     (e/adjust-times screen))))
         (sort-by :y #(compare %2 %1))
         play-sounds!
         (remove e/inactive?)
         (render-if-necessary! screen)
         (update-screen! screen)))
  
  :on-resize
  (fn [screen entities]
    (height! screen u/vertical-tiles))
  
  :on-key-down
  (fn [screen entities]
    (when-let [player (find-first :player? entities)]
      (when (= (:key screen) (key-code :space))
        (broadcast-player! screen player :minicraft-attack)
        (e/attack entities player))))
  
  :on-touch-down
  (fn [screen entities]
    (let [player (find-first :player? entities)
          min-x (/ (game :width) 3)
          max-x (* (game :width) (/ 2 3))
          min-y (/ (game :height) 3)
          max-y (* (game :height) (/ 2 3))]
      (when (and (< min-x (game :x) max-x)
                 (< min-y (game :y) max-y))
        (broadcast-player! screen player :minicraft-attack)
        (e/attack entities player))))
  
  :on-network-receive
  (fn [{:keys [topic message] :as screen} entities]
    (when (try (s/validate (get topics topic) message)
            (catch Exception _))
      (case topic
        :minicraft-update
        (let [entity (find-first #(= (:id message) (:id %)) entities)
              attrs (assoc message :last-update (System/currentTimeMillis))]
          (if entity
            (when-not (:player? entity)
              (e/update-person screen entities entity attrs))
            (e/add-person screen entities attrs)))
        
        :minicraft-attack
        (when-let [entity (find-first #(= (:id message) (:id %)) entities)]
          (when-not (:player? entity)
            (e/attack entities entity))))))
  
  :on-hide
  (fn [screen entities]
    (disconnect! screen)))

(defscreen text-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    (assoc (label "Players: 1" (color :white))
           :id :count
           :x 5))
  
  :on-render
  (fn [screen entities]
    (render! screen entities))
  
  :on-resize
  (fn [screen entities]
    (height! screen 300))
  
  :on-update-player-count
  (fn [screen entities]
    (for [entity entities]
      (case (:id entity)
        :count (doto entity
                 (label! :set-text (format "Players: %d" (:count screen))))
        entity))))

(defgame minicraft-online
  :on-create
  (fn [this]
    (set-screen! this main-screen text-screen)))

(defscreen blank-screen
  :on-render
  (fn [screen entities]
    (clear!)))

(set-screen-wrapper! (fn [screen screen-fn]
                       (try (screen-fn)
                         (catch Exception e
                           (.printStackTrace e)
                           (set-screen! minicraft-online blank-screen)))))
