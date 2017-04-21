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

(defn app []
  [:div.App
   [:div.App-header
    [:h2 "Welcome to Jub's Wordsearch!"]]
   [:div.App-body
    [:ul
     [:li "foobar"]
     [:li "squanch"]]
    [:table]]])

(reagent/render-component [app]
                          (. js/document (getElementById "app")))

; MODEL
(def tiles 
  [{ :x 0 :y 0 :letter "a"}
   { :x 1 :y 0 :letter "b"}
   { :x 2 :y 0 :letter "c"}
   { :x 0 :y 1 :letter "d"}
   { :x 1 :y 1 :letter "e"}
   { :x 2 :y 1 :letter "f"}
   { :x 0 :y 2 :letter "g"}
   { :x 1 :y 2 :letter "h"}
   { :x 2 :y 2 :letter "i"}]) 

; VIEW
(def font-size 60)
(def board-width 500)
(def tile-size (/ board-width (. js/Math (sqrt (count tiles)))))
(def tile-center-offset (/ tile-size 2))
(def tile-centers 
  (let [tile-centers-across 
        ; TODO: fix some tiles are out of range
        (range tile-center-offset board-width tile-size)]
    (for [x tile-centers-across
          y tile-centers-across]
      [x y])))

(defn draw-line [context from-x from-y to-x to-y]
  (. context beginPath)
  (set! (.-strokeStyle context) "black")
  (. context (moveTo from-x from-y))
  (. context (lineTo to-x to-y))
  (. context stroke))

(defn draw-text [context text x y]
  (set! (.-font context) (str font-size "px serif"))
  (. context (fillText text x y)))

(defn draw [canvas context state] 
  (. context (clearRect 0 0 (.-width canvas) (.-height canvas)))
  (doseq [{:keys [letter letter-x letter-y]} 
          (map (fn [tile [letter-x letter-y]] 
                 (assoc tile :letter-x letter-x :letter-y letter-y)) tiles tile-centers)]
    (draw-text context letter (- letter-x (/ font-size 4)) (+ letter-y (/ font-size 4))))
  (if-not (or (empty? (:line-start state)) (empty? (:line-end state)))
    (apply draw-line context (concat (:line-start state) (:line-end state)))))

(defn mouse-position 
  "get mouse position coordinates on canvas"
  [canvas event]
  [(- (.-pageX event) (.-offsetLeft canvas)) (- (.-pageY event) (.-offsetTop canvas))])

(let [canvas (. js/document (getElementById "board"))
      context (. canvas getContext "2d")
      state (atom {:line-start []
                   :line-end []})]
  (set! (.-onmousedown canvas) 
        (fn [event] 
          (swap! state 
                (fn [state] 
                  (let [coordinates (mouse-position canvas event)]
                    (assoc state :line-start coordinates :line-end coordinates))))))
  (set! (.-onmouseup canvas) (fn [event] (swap! state #(assoc % :line-start [] :line-end []))))
  (set! (.-onmousemove canvas) 
        (fn [event] 
          (swap! state
                 (fn [state] 
                   (assoc state :line-end (mouse-position canvas event))))
          (draw canvas context @state)))
  (draw canvas context @state))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counte] inc)
)
