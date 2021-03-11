(ns exfn.app
  (:require [reagent.dom :as r]))

(comment
  (str "To switch to cljs repl")
  (shadow/repl :your-build-id))

(defn app
  []
  [:div
   [:h1 "Lets learn reagent and re-frame!"]
   [:p "This is a paragraph"]
   [:ul
    (for [x (range 1 11)]
      [:li (str "List item: " x)])]])

(defn ^:dev/after-load start
  []
  (r/render [app]
            (.getElementById js/document "app")))

(defn ^:export init []
  (js/console.log "Lets learn re-frame!")
  (start))

