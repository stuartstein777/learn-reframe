(ns exfn.app
  (:require [reagent.dom :as dom]
            [re-frame.core :as rf]
            [goog.string.format]))

;;-- Events and Effects --------------------------------------------------------------------------
(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:points []}))

(rf/reg-event-db
 :point-click
 (fn [db [_ xy]]
   (update-in db [:points] conj xy)))

;; -- Subscriptions ------------------------------------------------------------------
(rf/reg-sub
 :points
 (fn [db _]
   (:points db)))

;; -- Reagent Forms ------------------------------------------------------------------
(defn point-canvas []
  [:div.content
   [:canvas#point-canvas.canvas
    {:on-click  (fn [^js e] (rf/dispatch [:point-click {:x (.. e -nativeEvent -offsetX) :y (.. e -nativeEvent -offsetY)}]))}]])

(defn buttons []
  [:div.content])

(defn points []
  (fn []
    [:div
     [:ul
      (for [point @(rf/subscribe [:points])]
        [:li (str "(" (:x point) "," (:y point) ")")])]]))

;; -- App -------------------------------------------------------------------------
(defn app []
  [:div.container
   [point-canvas]
   [points]])

(comment (rf/dispatch-sync [:initialize]))

;; -- After-Load --------------------------------------------------------------------
;; Do this after the page has loaded.
;; Initialize the initial db state.
(defn ^:dev/after-load start
  []
  (dom/render [app]
              (.getElementById js/document "app")))

(defn ^:export init []
  (start))

(defonce initialize (rf/dispatch-sync [:initialize]))       ; dispatch the event which will create the initial state.)