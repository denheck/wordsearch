(ns wordsearch.core
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "This text is printed from src/wordsearch/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state 
  (atom {
    :title "Hello World"
    :tiles [
    [
      { :letter "a", :in_words [] },
      { :letter "b", :in_words [] },
      { :letter "c", :in_words [] }
    ],
    [
      { :letter "f", :in_words ["foo"] },
      { :letter "o", :in_words ["foo"] },
      { :letter "o", :in_words ["foo"] },
    ],
    [
      { :letter "d", :in_words [] },
      { :letter "e", :in_words [] },
      { :letter "f", :in_words [] }
    ]]}))

(defn render-columns [columns] [:td]
  (map (fn [column] [:td (:letter column)]) columns))

(defn render-rows [rows]
  (map (fn [columns] [:tr (render-columns columns)]) rows))

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

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
