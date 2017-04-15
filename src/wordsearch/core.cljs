(ns wordsearch.core
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

;(println "This text is printed from src/wordsearch/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state 
  (atom {
    :title "Hello World"
    :tiles [
    [
      { :letter "a" :in_words [] :pressed true }
      { :letter "b" :in_words [] }
      { :letter "c" :in_words [] }
    ]
    [
      { :letter "f" :in_words ["foo"] }
      { :letter "o" :in_words ["foo"] }
      { :letter "o" :in_words ["foo"] }
    ]
    [
      { :letter "d" :in_words [] }
      { :letter "e" :in_words [] }
      { :letter "f" :in_words [] :pressed true }
    ]]}))

(defn render-tiles [tiles] [:td]
  (map (fn [tile] 
         [(if (:pressed tile) 
            :td.App-grid-item.pressed-grid-item 
            :td.App-grid-item.unpressed-grid-item) 
          (:letter tile)]) tiles))

(defn render-rows [rows]
  (map (fn [tiles] [:tr (render-tiles tiles)]) rows))

(defn app []
  [:div.App
   [:div.App-header
    [:h2 "Welcome to Jub's Wordsearch!"]]
   [:div.App-body
    [:ul
     [:li "foobar"]
     [:li "squanch"]]
    [:table
     [:tbody (let [rendered-rows (render-rows (:tiles @app-state))] 
               (println rendered-rows)
               rendered-rows)
      ]]]
   [:div.App-footer "Here's my wordsearch"]])

(reagent/render-component [app]
                          (. js/document (getElementById "app")))

(def board-size 500)
(def tile-size 25)
;(def lines [[0 25 25 25] 
;            [0 50 25 50]
;            [0 75 25 75]
;            [25 0 25 25]
;            [50 0 50 25]
;            [75 0 75 25]])
(def lines (concat (for [x (range 0 board-size tile-size)] [x 0 x board-size])
                   (for [y (range 0 board-size tile-size)] [0 y board-size y])))
             

(defn draw-line [context from-x from-y to-x to-y]
  (. context (moveTo from-x from-y))
  (. context (lineTo to-x to-y))
  (set! (.-strokeStyle context) "black")
  (. context stroke))

(defn draw-board []
  (let [canvas (. js/document (getElementById "board"))
        context (. canvas getContext "2d")]
    (loop [line (first lines)
           lines (rest lines)]
      (apply draw-line context line)
      (if-not (empty? lines)
        (recur (first lines) (rest lines))))))

(draw-board)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
