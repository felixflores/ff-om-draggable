(ns ff-om-draggable.core
  (:require [cljs.core.async :as async :refer [put! <! pub sub chan sliding-buffer]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [cljs.core.async.macros :as am :refer [go]]))

(def movement-chan (chan))
(def movement-pub (pub movement-chan :topic))

(defn find-topic
  [elem]
  (if (.. elem -dataset -draggable)
    elem
    (when-let [parent (.-parentElement elem)]
      (find-topic parent))))

(defn movement
  [e]
  {:top (.-movementY e)
   :left (.-movementX e)
   :topic (find-topic (.-target e))})

(defn track-movement
  [e]
  (put! movement-chan (movement e)))

(defn track-touchmove
  [e]
  (.preventDefault e)
  (map #(track-movement %) (.-changedTouches e)))

(.addEventListener js/window "mousemove" #(track-movement %))
(.addEventListener js/window "touchmove" #(track-touchmove %))

(defn draggable-item
  [view position-path]
  (fn [item owner]
    (reify
      om/IInitState
      (init-state [_]
        {:position (get-in item position-path)
         :disable true})
      om/IDidMount
      (did-mount [_]
        (let [delta (sub movement-pub (om/get-node owner) (chan))]
          (go (while true
                (let [curr-pos (om/get-state owner :position)
                      pos (if (om/cursor? curr-pos) @curr-pos curr-pos)
                      delta (<! delta)
                      new-pos {:top (+ (pos :top) (delta :top))
                               :left (+ (pos :left) (delta :left))}]
                  (when (not (om/get-state owner :disable))
                    (om/set-state! owner :position new-pos)))))))
      om/IRender
      (render [_]
        (dom/div (clj->js {:data-draggable true
                           :style (conj {:position "absolute"} (om/get-state owner :position))
                           :onMouseOut #(om/set-state! owner :disable true)
                           :onMouseDown #(om/set-state! owner :disable false)
                           :onMouseUp #(om/set-state! owner :disable true)})
                 (om/build view item))))))
