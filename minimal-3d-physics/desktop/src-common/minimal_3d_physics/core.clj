(ns minimal-3d-physics.core
  (:require [play-clj.core :refer :all]
            [play-clj.g3d :refer :all]
            [play-clj.g3d-physics :refer :all]
            [play-clj.math :refer :all]
            [play-clj.ui :refer :all]))

(def ^:const mass 10)

(defn get-material
  []
  (let [c (color (+ 0.5 (* 0.5 (rand)))
                 (+ 0.5 (* 0.5 (rand)))
                 (+ 0.5 (* 0.5 (rand)))
                 1)]
    (material :set (attribute! :color :create-specular 1 1 1 1)
              :set (attribute! :float :create-shininess 8)
              :set (attribute! :color :create-diffuse c))))

(defn get-attrs
  []
  (bit-or (usage :position) (usage :normal)))

(defn create-sphere-body!
  [screen]
  (let [shape (sphere-shape 2)
        local-inertia (vector-3 0 0 0)]
    (sphere-shape! shape :calculate-local-inertia mass local-inertia)
    (->> (rigid-body-info mass nil shape local-inertia)
         rigid-body
         (add-body! screen))))

(defn create-sphere!
  [screen mat attrs]
  (-> (model-builder)
      (model-builder! :create-sphere 4 4 4 24 24 mat attrs)
      model
      (assoc :body (create-sphere-body! screen))))

(defn create-box-body!
  [screen]
  (let [shape (box-shape (vector-3 2 2 1))
        local-inertia (vector-3 0 0 0)]
    (box-shape! shape :calculate-local-inertia mass local-inertia)
    (->> (rigid-body-info mass nil shape local-inertia)
         rigid-body
         (add-body! screen))))

(defn create-box!
  [screen mat attrs]
  (-> (model-builder)
      (model-builder! :create-box 4 4 2 mat attrs)
      model
      (assoc :body (create-box-body! screen))))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (let [env (let [attr-type (attribute-type :color :ambient-light)
                    attr (attribute :color attr-type 0.3 0.3 0.3 1)]
                (environment :set attr))
          cam (doto (perspective 67 (game :width) (game :height))
                (position! 10 10 10)
                (direction! 0 0 0))
          screen (update! screen
                          :renderer (model-batch)
                          :world (bullet-3d :discrete-dynamics
                                            :set-gravity (vector-3 0 -10 0))
                          :attributes env
                          :camera cam)]
      [(doto (create-sphere! screen (get-material) (get-attrs))
         (body-position! 0 5 5))
       (doto (create-box! screen (get-material) (get-attrs))
         (body-position! 0 5 0))]))
  :on-render
  (fn [screen entities]
    (clear!)
    (->> entities
         (render! screen)
         (step! screen)))
  :on-resize
  (fn [{:keys [width height] :as screen} entities]
    (size! screen width height))
  :on-touch-down
  (fn [{:keys [x y] :as screen} entities]
    (conj entities (create-box! screen (get-material) (get-attrs)))))

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

(defgame minimal-3d-physics
  :on-create
  (fn [this]
    (set-screen! this main-screen text-screen)))
