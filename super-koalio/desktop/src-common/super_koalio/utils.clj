(ns super-koalio.utils
  (:require [play-clj.core :refer :all]))

(def ^:const vertical-tiles 20)
(def ^:const pixels-per-tile 16)
(def ^:const duration 0.2)
(def ^:const max-velocity 5)
(def ^:const max-jump-velocity (* max-velocity 4))
(def ^:const deceleration 0.9)
(def ^:const gravity -2.5)

(defn decelerate
  [velocity]
  (let [velocity (* velocity deceleration)]
    (if (< (Math/abs velocity) 0.5)
      0
      velocity)))

(defn ^:private is-touched?
  [key]
  (and (game :is-touched?)
       (case key
         :down (> (game :y) (* (game :height) (/ 2 3)))
         :up (< (game :y) (/ (game :height) 3))
         :left (< (game :x) (/ (game :width) 3))
         :right (> (game :x) (* (game :width) (/ 2 3)))
         false)))

(defn get-x-velocity
  [{:keys [is-me? x-velocity]}]
  (if is-me?
    (cond
      (or (is-pressed? :dpad-left) (is-touched? :left))
      (* -1 max-velocity)
      (or (is-pressed? :dpad-right) (is-touched? :right))
      max-velocity
      :else
      x-velocity)
    x-velocity))

(defn get-y-velocity
  [{:keys [is-me? y-velocity can-jump?]}]
  (if is-me?
    (cond
      (and can-jump? (or (is-pressed? :dpad-up) (is-touched? :up)))
      max-jump-velocity
      :else
      y-velocity)
    y-velocity))

(defn get-direction
  [{:keys [x-velocity direction]}]
  (cond
    (> x-velocity 0) :right
    (< x-velocity 0) :left
    :else
    direction))
