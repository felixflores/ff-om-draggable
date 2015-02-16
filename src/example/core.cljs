(ns example.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ff-om-draggable.core :refer [draggable-item]]))

(def app-state (atom {:people [{:name "Drag Me" :avatar "images/felixflores.png" :position {:left 500 :top 200}}]}))

(defn person-view
  [person owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/img #js {:src (person :avatar) :draggable false})
               (dom/div nil (person :name))))))

(def draggable-person-view
  (draggable-item person-view [:position]))

(defn people-view
  [app owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js  {:id "people"}
             (om/build-all draggable-person-view (app :people))))))

(om/root
  people-view
  app-state
  {:target (. js/document (getElementById "app"))})
