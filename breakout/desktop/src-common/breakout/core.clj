(ns breakout.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.g2d-physics :refer :all]
            [play-clj.ui :refer :all]))

(declare breakout main-screen text-screen)

(def ^:const pixels-per-tile 32)

(defn create-ball-body!
  [screen x y radius]
  (->> (circle radius)
       (fixture :density 1 :friction 0 :restitution 1 :shape)
       (create-body! screen :dynamic :set-transform x y 0 :create-fixture)))

(defn create-rect-body!
  [screen x y width height]
  (->> [0 0
        0 height
        width height
        width 0
        0 0]
       float-array
       (chain :create-chain)
       (fixture :density 1 :shape)
       (create-body! screen :static :set-transform x y 0 :create-fixture)))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (let [screen (update! screen
                          :camera (orthographic)
                          :renderer (stage)
                          :world (box-2d 0 0))
          game-w (/ (game :width) pixels-per-tile)
          game-h (/ (game :height) pixels-per-tile)
          floor-h (/ 1 pixels-per-tile)
          ball (texture "ball.png")
          ball-x (/ 100 pixels-per-tile)
          ball-y (/ 100 pixels-per-tile)
          ball-w (/ (texture! ball :get-region-width) pixels-per-tile)
          ball-h (/ (texture! ball :get-region-height) pixels-per-tile)
          block (texture "block.png")
          block-w (/ (texture! block :get-region-width) pixels-per-tile)
          block-h (/ (texture! block :get-region-height) pixels-per-tile)
          block-cols (int (/ game-w block-w))
          block-rows (int (/ game-h 2 block-h))]
      ; set the screen width in tiles
      (width! screen game-w)
      ; return the entities
      [(assoc ball
              :ball? true
              :body (doto (create-ball-body! screen ball-x ball-y (/ ball-w 2))
                      (body! :set-linear-velocity 10 10))
              :x ball-x :y ball-y
              :width ball-w :height ball-h)
       (assoc block
              :paddle? true
              :body (create-rect-body! screen 0 0 block-w block-h)
              :x 0 :y 0
              :width block-w
              :height block-h)
       {:wall? true :body (create-rect-body! screen 0 0 game-w game-h)}
       {:floor? true :body (create-rect-body! screen 0 0 game-w floor-h)}
       (for [col (range block-cols)
             row (range block-rows)
             :let [x (* col block-w)
                   y (+ (* row block-h) (/ game-h 2))]]
         (assoc block
                :block? true
                :body (create-rect-body! screen x y block-w block-h)
                :x x :y y
                :width block-w :height block-h))]))
  :on-render
  (fn [screen entities]
    (clear!)
    (->> entities
         (render! screen)
         (step! screen)))
  :on-mouse-moved
  (fn [screen entities]
    (for [entity entities]
      (cond
        (:paddle? entity)
        (body-x! entity (- (/ (game :x) pixels-per-tile) (/ (:width entity) 2)))
        :else
        entity)))
  :on-begin-contact
  (fn [screen entities]
    (when-let [entity (first-contact screen entities)]
      (cond
        (:floor? entity)
        (set-screen! breakout main-screen text-screen)
        (:block? entity)
        (remove #(= entity %) entities)))))

(defscreen text-screen
  :on-show
  (fn [screen entities]
    (update! screen :camera (orthographic) :renderer (stage))
    (assoc (label "0" (color :white))
           :id :fps
           :x 5))
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
    nil))

(defgame breakout
  :on-create
  (fn [this]
    (set-screen! this main-screen text-screen)))
