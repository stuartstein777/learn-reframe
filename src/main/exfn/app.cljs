(ns exfn.app
  (:require [reagent.dom :as dom]
            [re-frame.core :as rf]
            [goog.string.format]))

;;-- Events and Effects --------------------------------------------------------------------------
(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:points []}))

(defn clear-canvas [canvas ctx]
  (let [w (.-width canvas)
        h (.-height canvas)]
    (.beginPath ctx)
    (set! (.-fillStyle ctx) "white")
    (.rect ctx 0 0  w h)
    (.fill ctx)))

(rf/reg-fx
 :draw-points
 (fn [points]
   (let [canvas (.getElementById js/document "point-canvas")
         ctx (.getContext canvas "2d")]
     (.scale ctx 1 1)
     (clear-canvas canvas ctx)
     (.beginPath ctx)
     (set! (.-lineWidth ctx) 2.0)
     (set! (.-strokeStyle ctx) "red")
     (dorun (map (fn [{:keys [x y]}]
                   (.arc ctx x y 1 0 (* 2 (.-PI js/Math)) 0)
                   (.lineTo ctx x y)) points))
     ;(.closePath ctx)
     (.stroke ctx)
     (.fill ctx))))

(rf/reg-event-fx
 :point-click
 (fn [{:keys [db]} [_ xy]]
   (let [updated-points (conj (db :points) xy)]
     {:db          (assoc db :points updated-points)
      :draw-points updated-points})))

;; -- Subscriptions ------------------------------------------------------------------
(rf/reg-sub
 :points
 (fn [db _]
   (:points db)))

;; -- Reagent Forms ------------------------------------------------------------------
(defn point-canvas []
  [:div.content
   [:canvas#point-canvas.canvas
    {:on-click  (fn [^js e] (rf/dispatch [:point-click {:x (.. e -nativeEvent -offsetX) :y (.. e -nativeEvent -offsetY)}]))
     :width 500
     :height 500}]])

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