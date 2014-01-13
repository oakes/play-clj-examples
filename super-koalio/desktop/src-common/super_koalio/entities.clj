(ns super-koalio.entities
  (:require [play-clj.core :refer :all]
            [super-koalio.utils :as u]))

(defn create
  [walk jump & run]
  (assoc walk
         :walk walk
         :jump jump
         :run (animation u/duration run :loop-pingpong)
         :x 20
         :y 10
         :width 1
         :height (/ 26 18)))
