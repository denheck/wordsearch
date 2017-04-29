(ns wordsearch.core
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

;(println "This text is printed from src/wordsearch/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state 
  (atom { :title "Hello World" }))

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
(def words [{:text "fo" :at nil}])

(def tiles 
  [{ :x 0 :y 0 :letter "f" :word-start :w1}
   { :x 1 :y 0 :letter "o" :word-end :w1}
   { :x 0 :y 1 :letter "d"}
   { :x 1 :y 1 :letter "e"}])

; VIEW
(def font-size 60)
(def board-width 500)

(defn tile-width [tiles] (/ board-width (. js/Math (sqrt (count tiles)))))

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
  (doseq [{:keys [letter letter-x letter-y]} (:tiles state)]
    (draw-text context letter (- letter-x (/ font-size 4)) (+ letter-y (/ font-size 4))))
  (if-not (or (empty? (:line-start state)) (empty? (:line-end state)))
    (apply draw-line context (concat (:line-start state) (:line-end state)))))

(defn mouse-position 
  "get mouse position coordinates on canvas"
  [canvas event]
  [(- (.-pageX event) (.-offsetLeft canvas)) (- (.-pageY event) (.-offsetTop canvas))])

(defn find-tile [x y tiles]
  (reduce (fn [found-tile tile]
            (let [position-x (:position-x tile)
                  position-y (:position-y tile)
                  tile-size (tile-width tiles)]
              (if (and (<= position-x x (+ position-x tile-size)) (<= position-y y (+ position-y tile-size))) (reduced tile) nil))) nil tiles))

(defn coordinate-range 
  [start-coordinate end-coordinate]
  (cond
    ; TODO: replace 10 with sqrt total number of tiles
    (= start-coordinate end-coordinate) (repeat 10 start-coordinate)
    (> start-coordinate end-coordinate) (range start-coordinate (- end-coordinate 1) -1)
    :else (range start-coordinate (+ 1 end-coordinate))))

(defn find-word [[board-start-x board-start-y] [board-end-x board-end-y] tiles]
  "Find word by positions of start and end of line on board"
  (let [start-tile (find-tile board-start-x board-start-y tiles)
        end-tile (find-tile board-end-x board-end-y tiles)
        start-x (:x start-tile)
        end-x (:x end-tile)
        start-y (:y start-tile)
        end-y (:y end-tile)
        xs (coordinate-range start-x end-x)
        ys (coordinate-range start-y end-y)]
    (reduce str 
            (map (fn [x y] 
                   (reduce (fn [found-tile tile] 
                             (if (and (= x (:x tile)) (= y (:y tile))) (reduced (:letter tile)) nil)) nil tiles)) xs ys))))

(defn mark-found [found-word words] 
  (map (fn [word] (if (= (:text found-word) (:text word)) found-word word)) words))

; TODO: not sure if this is working (NOT NEEDED MAYBE)
(defn sort-tiles [tiles]
  (sort-by (fn [{ :keys [x y] } tile] (+ x (* y 10))) tiles))

(let [canvas (. js/document (getElementById "board"))
      context (. canvas getContext "2d")
      state (atom {:line-start []
                   :line-end []
                   :words words
                   ; Add board position and letter position of tiles
                   :tiles (map (fn [tile [letter-x letter-y]]
                                 (let [{:keys [x y]} tile
                                       tile-size (tile-width tiles)
                                       position-x (* x tile-size)
                                       position-y (* y tile-size)
                                       tile-center-offset (/ tile-size 2)
                                       letter-x (+ position-x tile-center-offset)
                                       letter-y (+ position-y tile-center-offset)]
                                   (assoc tile :letter-x letter-x :letter-y letter-y :position-x position-x :position-y position-y))) tiles)})]
  (set! (.-onmousedown canvas) 
        (fn [event] 
          (swap! state 
                (fn [state] 
                  (let [coordinates (mouse-position canvas event)]
                    (assoc state :line-start coordinates :line-end coordinates))))))
  (set! (.-onmouseup canvas)
        (fn [event]
          (swap! state
                 (fn [current-state]
                   (let [{:keys [words line-start line-end tiles]} current-state
                         word {:text (find-word line-start line-end tiles) :at [line-start line-end]}]
                     (println (:words current-state))
                     (assoc current-state :line-start [] :line-end [] :words (mark-found word words)))))))
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
