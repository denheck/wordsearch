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

(defn fits-in? 
  "check if all tiles are within the boundaries of the board"
  [new-tiles num-tiles-wide] 
  (let [in-range #(some? (some #{%} (range 0 (inc num-tiles-wide))))]
    (every? true? (mapcat (fn [{:keys [x y]}] [(in-range x) (in-range y)]) new-tiles))))

(defn first-arg [& args] (first args))

(defn get-direction-functions 
  "get a vector of functions to apply to coordinates based on direction, i.e. :ne == [+ -]"
  [direction]
  (get {:n [first-arg -] :ne [+ -] :e [+ first-arg] :se [+ +] :s [first-arg +] :sw [- +] :w [- first-arg] :nw [- -]} direction))

(defn coordinate-range 
  [start-coordinate end-coordinate]
  (cond
    ; TODO: replace 10 with sqrt total number of tiles
    (= start-coordinate end-coordinate) (repeat 10 start-coordinate)
    (> start-coordinate end-coordinate) (range start-coordinate (- end-coordinate 1) -1)
    :else (range start-coordinate (+ 1 end-coordinate))))

(defn generate-tiles [word x y direction greatest-coordinate]
  "recalculate x and y based on greatest coordinate and generate tiles in direction"
  (let [direction-functions (get-direction-functions direction)
        word-length (count word)
        last-x (apply (first direction-functions) [x word-length])
        last-y (apply (second direction-functions) [y word-length])
        xs (coordinate-range x last-x)
        ys (coordinate-range y last-y)]
    (map (fn [x y letter] {:x x :y y :letter letter}) xs ys (seq word))))

(defn generate-missing-tiles
  "fill in the missing tiles for the provided tiles"
  [tiles num-tiles-wide]
  (let [coordinates (map (fn [{:keys [x y]}] [x y]) tiles)]
    (concat 
      (for [x (range 0 (inc num-tiles-wide))
            y (range 0 (inc num-tiles-wide))
            :let [letter (rand-nth (vec "ABCDEFGHIJKLMNOPQRSTUVWXYZ"))]
            :when (not (some #{[x y]} coordinates))]
        {:x x :y y :letter letter}) tiles)))

(defn overlap?
  "check if tiles overlap"
  [new-tiles tiles]
  (reduce (fn [_ new-tile]
            (let [tile-overlaps? (some true? (map (fn [tile] (and (= (:x new-tile) (:x tile)) (= (:y new-tile) (:y tile)))) tiles))]
              (if tile-overlaps? (reduced true) false))) false new-tiles))

; MODEL
(defn generate-board [words directions num-tiles-wide] ; TODO: should probably be renamed to something to do with tiles
  "generate tiles for board based on words"
  (let [words (->> words (sort count) set)
        new-tiles (reduce (fn [tiles word] 
                            (loop [] 
                              (let [x (rand-int num-tiles-wide) 
                                    y (rand-int num-tiles-wide) 
                                    direction (rand-nth directions)
                                    new-tiles (generate-tiles word x y direction num-tiles-wide)
                                    overlapping? (overlap? new-tiles tiles)]
                                (if (and (fits-in? new-tiles num-tiles-wide) (not (overlap? new-tiles tiles)))
                                  (concat tiles new-tiles)
                                  (recur))))) [] words)]
    (generate-missing-tiles new-tiles num-tiles-wide)))

; VIEW
(def board-width 500)

(defn tile-width [tiles] (/ board-width (. js/Math (sqrt (count tiles)))))

(defn unfound-word 
  [word]
  (nil? (:at word)))

(defn draw-line [context from-x from-y to-x to-y]
  (. context beginPath)
  (set! (.-strokeStyle context) "black")
  (set! (.-lineWidth context) 5)
  (. context (moveTo from-x from-y))
  (. context (lineTo to-x to-y))
  (. context stroke))

(defn draw-text [context text x y font-size & {:keys [color] :or {color "#000000"}}]
  (set! (.-font context) (str font-size "px Monaco"))
  (set! (.-fillStyle context) color)
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
            word-count (count (:words state))
            word-table-bottom (* (+ word-table-top-offset word-table-font-size word-table-padding) (inc word-count))
            word-table-ys (range word-table-top-offset word-table-bottom word-table-font-size)
            words (:words state)
            tile-font-size (:tile-font-size state)
            draw-text-args (concat 
                             (map (fn [{:keys [letter letter-x letter-y]}] 
                                    [context letter (- letter-x (/ tile-font-size 4)) (+ letter-y (/ tile-font-size 4)) tile-font-size]) (:tiles state))
                             (map (fn [{:keys [text] :as word} text-y] 
                                    (concat 
                                      [context text (+ word-table-padding board-width) text-y word-table-font-size] 
                                      (if (unfound-word word) [] [:color "#EEEEEE"]))) words word-table-ys))] 
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
        word-list [{:id 476561, :word "Albacete"} {:id 76770, :word "Bethesda"} {:id 557047, :word "Osteichthyes"} {:id 192944, :word "Sabbatine"} {:id 635226, :word "Sabra"} {:id 487861, :word "Thisbe"} {:id 1708970, :word "Wadayama"} {:id 238365, :word "accusatives"} {:id 1239, :word "acetin"} {:id 238735, :word "addends"} {:id 2614, :word "advantageable"} {:id 7245, :word "animalize"} {:id 7407, :word "annihilative"} {:id 242532, :word "appertained"} {:id 12904, :word "autor"} {:id 500934, :word "baby-eyes"} {:id 2633879, :word "backtalkers"} {:id 3834848, :word "bake-offs"} {:id 245052, :word "balancers"} {:id 14598, :word "baptise"} {:id 95567, :word "barroom"} {:id 16449, :word "behest"} {:id 18140, :word "bindweed"} {:id 27315, :word "casemated"} {:id 253299, :word "catheterized"} {:id 253583, :word "cellulars"} {:id 1975983, :word "changeset"} {:id 30995, :word "chieve"} {:id 84261, :word "circumstances"} {:id 34288, :word "cloud-born"} {:id 41246, :word "crambe"} {:id 260659, :word "cremini"} {:id 2096877, :word "cruise-control"} {:id 31256992, :word "cybercultural"} {:id 261852, :word "cyberspaces"} {:id 2059763, :word "destigmatization"} {:id 51237, :word "dissertation"} {:id 52710, :word "doubter"} {:id 361332, :word "drenche"} {:id 762567, :word "dretful"} {:id 55296, :word "edition"} {:id 271069, :word "enwombed"} {:id 27449088, :word "ethnoarchaeological"} {:id 1330943, :word "gamahuche"} {:id 284947, :word "hoses"} {:id 70473, :word "humiliation"} {:id 114662, :word "ichthyological"} {:id 286376, :word "illogic"} {:id 720195, :word "inanimates"} {:id 9650616, :word "inlarging"} {:id 520818, :word "intubator"} {:id 120350, :word "jewellery"} {:id 93533, :word "jocundity"} {:id 121151, :word "kalan"} {:id 293267, :word "leechlike"} {:id 124910, :word "leming"} {:id 9618263, :word "lengthenings"} {:id 294401, :word "literarily"} {:id 297288, :word "mediatized"} {:id 3380010, :word "meeted"} {:id 138773, :word "natatory"} {:id 81636, :word "nauseating"} {:id 302837, :word "necromancers"} {:id 14771288, :word "noncorrupt"} {:id 92076, :word "oarsman"} {:id 88936, :word "overhauling"} {:id 1863130, :word "phonecards"} {:id 533800, :word "photo-engraver"} {:id 314978, :word "plebes"} {:id 83566, :word "plethora"} {:id 316134, :word "positing"} {:id 1418817, :word "resourcing"} {:id 191730, :word "root-house"} {:id 563342, :word "roten"} {:id 196329, :word "scourage"} {:id 1833083, :word "scriptability"} {:id 472832, :word "self-tolerance"} {:id 198123, :word "self-torture"} {:id 558974, :word "sheitel"} {:id 205964, :word "spikenard"} {:id 373905, :word "split-tail"} {:id 337621, :word "stickier"} {:id 339785, :word "sunbathed"} {:id 701612, :word "textfile"} {:id 544187, :word "thumb-print"} {:id 9292431, :word "timeslice"} {:id 723557, :word "transcriptome"} {:id 219981, :word "transitionally"} {:id 1854361, :word "tropical-storm"} {:id 2023583, :word "tuna-fish"} {:id 2640980, :word "unbloated"} {:id 1605484, :word "undiscounted"} {:id 349229, :word "unnervingly"} {:id 225987, :word "unrecovered"} {:id 349582, :word "unslings"} {:id 351810, :word "visitors"} {:id 352721, :word "washermen"} {:id 232045, :word "wharves"} {:id 1710758, :word "will-o"} {:id 377693, :word "woolhead"}]
        num-tiles-wide 10
        num-words 5
        words (take num-words (filter #(and (> num-tiles-wide (count %)) (nil? (some #{"-"} %))) (map #(-> % :word .toUpperCase) (shuffle word-list))))
        board-tiles (generate-board words [:n :ne :e :se :s :sw :w :nw] num-tiles-wide)
        tile-size (tile-width board-tiles)
        state (atom {:game-over false
                     :line-start []
                     :line-end []
                     :words (map #(assoc {:at nil} :text %) words)
                     :word-table-width 200 
                     :word-table-font-size 25 ; TODO: can this be the same as tile-font-size
                     :tile-font-size (.floor js/Math (* tile-size 0.55))
                     ; Add board position and letter position of tiles
                     :tiles (map (fn [tile [letter-x letter-y]]
                                   (let [{:keys [x y]} tile
                                         position-x (* x tile-size)
                                         position-y (* y tile-size)
                                         tile-center-offset (/ tile-size 2)
                                         letter-x (+ position-x tile-center-offset)
                                         letter-y (+ position-y tile-center-offset)]
                                     (assoc tile :letter-x letter-x :letter-y letter-y :position-x position-x :position-y position-y))) board-tiles)})]
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
