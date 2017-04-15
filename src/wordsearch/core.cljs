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
     ;[:tbody (let [rendered-rows (render-rows (:tiles @app-state))] 
     ;          (println rendered-rows)
     ;          rendered-rows)
      ]]])

(reagent/render-component [app]
                          (. js/document (getElementById "app")))

; MODEL
(def num-tiles 16) ; needs to have an integer square root

; VIEW
(def board-width 500)
(def tile-size (/ board-width (. js/Math (sqrt num-tiles))))
(def tile-center-offset (/ tile-size 2))
(def lines (let [next-coordinate (range 0 (+ board-width tile-size) tile-size)] 
             (concat (for [x next-coordinate] [x 0 x board-width])
                     (for [y next-coordinate] [0 y board-width y]))))
(def tiles
  (let [tile-centers-across 
        (range tile-center-offset (+ board-width tile-center-offset) tile-size)]
    (for [x tile-centers-across
          y tile-centers-across]
      {:x x :y y :letter "a"})))

(defn draw-line [context from-x from-y to-x to-y]
  (. context (moveTo from-x from-y))
  (. context (lineTo to-x to-y))
  (set! (.-strokeStyle context) "black")
  (. context stroke))

(defn draw-text [context text x y]
  (set! (.-font context) "20px serif")
  (. context (fillText text x y)))

(defn draw-board []
  (let [canvas (. js/document (getElementById "board"))
        context (. canvas getContext "2d")]
    (loop [line (first lines)
           lines (rest lines)]
      (apply draw-line context line)
      (if-not (empty? lines)
        (recur (first lines) (rest lines))))
    (loop [{:keys [letter x y]} (first tiles)
           tiles (rest tiles)]
      (draw-text context letter x y)
      (if-not (empty? tiles)
        (recur (first tiles) (rest tiles))))))

(draw-board)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counte] inc)
)
