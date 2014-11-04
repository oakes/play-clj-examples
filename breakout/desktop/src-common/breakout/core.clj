(ns breakout.core
  (:require [play-clj.core :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.g2d-physics :refer :all]
            [play-clj.math :refer :all]
            [play-clj.ui :refer :all]))

(declare breakout main-screen text-screen)

(def ^:const pixels-per-tile 32)

(defn create-ball-body!
  [screen radius]
  (let [body (add-body! screen (body-def :dynamic))]
    (->> (circle-shape :set-radius radius
                       :set-position (vector-2 radius radius))
         (fixture-def :density 1 :friction 0 :restitution 1 :shape)
         (body! body :create-fixture))
    body))

(defn create-rect-body!
  [screen width height]
  (let [body (add-body! screen (body-def :static))]
    (->> [0 0
          0 height
          width height
          width 0
          0 0]
         float-array
         (chain-shape :create-chain)
         (fixture-def :density 1 :shape)
         (body! body :create-fixture))
    body))

(defn create-ball-entity!
  [screen]
  (let [ball (texture "ball.png")
        width (/ (texture! ball :get-region-width) pixels-per-tile)
        height (/ (texture! ball :get-region-height) pixels-per-tile)]
    (assoc ball
           :body (create-ball-body! screen (/ width 2))
           :width width :height height)))

(defn create-rect-entity!
  [screen block width height]
  (assoc block
         :body (create-rect-body! screen width height)
         :width width :height height))

(defn move-paddle!
  [entities]
  (when-let [entity (find-first :paddle? entities)]
    (body-x! entity (- (/ (game :x) pixels-per-tile) (/ (:width entity) 2)))))

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
          block (texture "block.png")
          block-w (/ (texture! block :get-region-width) pixels-per-tile)
          block-h (/ (texture! block :get-region-height) pixels-per-tile)
          block-cols (int (/ game-w block-w))
          block-rows (int (/ game-h 2 block-h))
          ball (doto (create-ball-entity! screen)
                 (body-position! (/ 100 pixels-per-tile)
                                 (/ 100 pixels-per-tile)
                                 0)
                 (body! :set-linear-velocity 10 10))
          paddle (doto (create-rect-entity! screen block block-w block-h)
                   (body-position! 0 0 0))
          wall (doto {:body (create-rect-body! screen game-w game-h)}
                 (body-position! 0 0 0))
          floor (doto {:body (create-rect-body! screen game-w floor-h)}
                  (body-position! 0 0 0))]
      ; set the screen width in tiles
      (width! screen game-w)
      ; attach the ball to the paddle so it can't reach the blocks
      ; (this is only meant to test that joints work)
      (comment add-joint! screen
                  (joint-def :rope
                             :body-a (:body ball)
                             :body-b (:body paddle)
                             :max-length 5
                             :collide-connected true))
      ; return the entities
      [(assoc ball :ball? true)
       (assoc paddle :paddle? true)
       (assoc wall :wall? true)
       (assoc floor :floor? true)
       (for [col (range block-cols)
             row (range block-rows)
             :let [x (* col block-w)
                   y (+ (* row block-h) (- game-h (* block-h block-rows)))]]
         (assoc (doto (create-rect-entity! screen block block-w block-h)
                  (body-position! x y 0))
                :block? true))]))
  
  :on-render
  (fn [screen entities]
    (clear!)
    (->> entities
         (step! screen)
         (render! screen)))
  
  :on-mouse-moved
  (fn [screen entities]
    (move-paddle! entities)
    nil)
  
  :on-touch-dragged
  (fn [screen entities]
    (move-paddle! entities)
    nil)
  
  :on-begin-contact
  (fn [screen entities]
    (when-let [entity (first-entity screen entities)]
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
    (height! screen 300)))

(defgame breakout
  :on-create
  (fn [this]
    (set-screen! this main-screen text-screen)))
