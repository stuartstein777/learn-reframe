(ns exfn.app
  (:require [reagent.dom :as r]))

(comment
  (str "To switch to cljs repl evaluate this, don't use cljs drop down in repl tab.")
  (shadow/repl :app)
  (js/alert "hello!"))

(defn random-string []
  (->> "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
       (shuffle)
       (take 5)
       (apply str)))

(defn app
  []
  [:div
   [:h1 "Lets learn reagent and re-frame!!!"]
   [:p {:class "bigp"} "This is a paragraph"]
   [:ol
    (for [x (range 1 11)]
      [:li {:key (str "li-item-" x)} (str (random-string))])]])

(defn ^:dev/after-load start
  []
  (r/render [app]
            (.getElementById js/document "app")))

(defn ^:export init []
  (js/console.log "Lets learn re-frame!")
  (start))