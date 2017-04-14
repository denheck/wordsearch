(ns wordsearch.core
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "This text is printed from src/wordsearch/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))

(defn app []
  ;;[:h1 (:text @app-state)])
  [:div.App
   [:div.App-header
    [:h2 "Welcome to Jub's Wordsearch!"]]
   [:div.App-body
    [:ul
     [:li "foobar"]
     [:li "squanch"]]
    [:table
     [:tbody]]]
   [:div.App-footer "Here's my wordsearch"]])

(reagent/render-component [app]
                          (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
