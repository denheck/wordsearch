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
    [:table]]])

(reagent/render-component [app]
                          (. js/document (getElementById "app")))

; MODEL
(def words [{:text "fez" :at nil}
            {:text "foo" :at nil}])

(def tiles 
  [{ :x 0 :y 0 :letter "f"}
   { :x 1 :y 0 :letter "o"}
   { :x 2 :y 0 :letter "o"}
   { :x 0 :y 1 :letter "d"}
   { :x 1 :y 1 :letter "e"}
   { :x 2 :y 1 :letter "x"}
   { :x 0 :y 2 :letter "t"}
   { :x 1 :y 2 :letter "f"}
   { :x 2 :y 2 :letter "z"}])

; VIEW
(def board-width 500)

(defn tile-width [tiles] (/ board-width (. js/Math (sqrt (count tiles)))))

(defn unfound-words [words] 
  (filter #(-> %1 :at nil?) words))

(defn draw-line [context from-x from-y to-x to-y]
  (. context beginPath)
  (set! (.-strokeStyle context) "black")
  (. context (moveTo from-x from-y))
  (. context (lineTo to-x to-y))
  (. context stroke))

(defn draw-text [context text x y font-size]
  (set! (.-font context) (str font-size "px serif"))
  (. context (fillText text x y)))

(defn draw [canvas context state] 
  (if (= true (:game-over state))
    (let [board-center-coordinate (/ board-width 2)
          message-box-height 100
          message-box-width 300
          message-box-x (- board-center-coordinate (/ message-box-width 2))
          message-box-y (- board-center-coordinate (/ message-box-height 2))]
      (draw-text context "Play Again?" message-box-x message-box-y 100))
    (do
      (. context (clearRect 0 0 (.-width canvas) (.-height canvas)))
      (let [word-table-padding 5
            word-table-top-offset 50
            word-table-font-size (:word-table-font-size state)
            word-table-ys (range word-table-top-offset (+ word-table-top-offset word-table-font-size (* (+ 1 (count words)) word-table-padding)) word-table-font-size)
            words (unfound-words (:words state))
            tile-font-size (:tile-font-size state)
            draw-text-args (concat 
                             (map (fn [{:keys [letter letter-x letter-y]}] [context letter (- letter-x (/ tile-font-size 4)) (+ letter-y (/ tile-font-size 4)) tile-font-size]) (:tiles state))
                             (map (fn [{:keys [text]} text-y] [context text (+ word-table-padding board-width) text-y word-table-font-size]) words word-table-ys))] 
        (doseq [args draw-text-args] 
          (apply draw-text args)))
      (let [line-start (:line-start state)
            line-end (:line-end state)
            lines (concat (if (or (empty? line-start) (empty? line-end)) [] [[line-start line-end]]) (filter some? (map :at (:words state))))] 
        (doseq [[[from-x from-y] [to-x to-y]] lines] 
          (draw-line context from-x from-y to-x to-y))))))

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

(defn is-game-over [words] 
  (every? #(-> %1 :at nil? not) words))


(defn start []
  (let [canvas (. js/document (getElementById "board"))
        context (. canvas getContext "2d")
        state (atom {:game-over false
                     :line-start []
                     :line-end []
                     :words words
                     :word-table-width 200 ; TODO: should be adjustable based on board dimensions
                     :word-table-font-size 25
                     :tile-font-size 60
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
    (add-watch state :view-renderer (fn [_key _ref _prev-state new-state] (draw canvas context new-state)))
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
                           word {:text (find-word line-start line-end tiles) :at [line-start line-end]}
                           reversed-word {:text (reduce str (reverse (:text word))) :at [line-end line-start]}
                           words (mark-found word (mark-found reversed-word words))]
                       ; TODO: may want to ensure words can only be found if they are horizontal, vertical or diagonal
                       (assoc current-state :line-start [] :line-end [] :game-over (is-game-over words) :words words))))))
    (set! (.-onmousemove canvas) 
          (fn [event] 
            (swap! state
                   (fn [state] 
                     (assoc state :line-end (mouse-position canvas event))))))
    (draw canvas context @state)))

(start)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counte] inc)
)
