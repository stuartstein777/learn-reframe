(ns exfn.app
  (:require [reagent.dom :as dom]
            [re-frame.core :as rf]
            [goog.string.format]))

;;-- Events and Effects --------------------------------------------------------------------------
(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:points []
    :current-action :drawing-boundary
    :point {}
    :location "Requires Calculation"}))

(defn clear-canvas [canvas ctx]
  (let [w (.-width canvas)
        h (.-height canvas)]
    (.beginPath ctx)
    (set! (.-fillStyle ctx) "white")
    (.rect ctx 0 0  w h)
    (.fill ctx)))

(rf/reg-fx
 :draw-canvas
 (fn [[points {:keys [x y]}]]
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
     (.stroke ctx)
     (.beginPath ctx)
     (when (and (not (nil? x)) (not (nil? y)))
       (.arc ctx x y 4 0 (* 2 (.-PI js/Math)) 0)
       (.stroke ctx)))))

(rf/reg-event-fx
 :update-canvas
 (fn [{:keys [db]} _]
   {:db db
    :draw-canvas [(:points db) (:point db)]}))

(rf/reg-event-fx
 :point-click
 (fn [{:keys [db]} [_ xy]]
   (cond 
     ; if user is drawing boundary...
     (= :drawing-boundary (db :current-action))
         (let [updated-points (conj (db :points) xy)]
           {:db          (-> db
                             (assoc :points updated-points)
                             (assoc :location "Requires Calculation"))
            :draw-canvas [updated-points (:point db)]})
     
     ; if user is selecting a point.
     (= :selecting-point (db :current-action))
     {:db          (-> db
                       (assoc :point xy)
                       (assoc :location "Requires Calculation"))
      :draw-canvas [(:points db) xy]})))

(rf/reg-event-fx
 :reset-boundary
 (fn [cofx _]
   {:draw-canvas [[] (:point (:db cofx))]
    :db          (-> (:db cofx)
                     (assoc :points [])
                     (assoc :current-action :drawing-boundary))}))

(rf/reg-event-db
 :calculate
 (fn [{:keys [points point] :as db} _]
   (assoc db :location "Requires Calculation")))

(rf/reg-event-db
 :toggle
 (fn [{:keys [current-action] :as db} _]
   (let [toggle {:drawing-boundary :selecting-point :selecting-point :drawing-boundary}]
     (assoc db :current-action (toggle current-action)))))

;; -- Subscriptions ------------------------------------------------------------------
(rf/reg-sub
 :points
 (fn [db _]
   (:points db)))

(rf/reg-sub
 :boundary-button-label
 (fn [db _]
   (:boundary-button-label db)))

(rf/reg-sub
 :current-action
 (fn [db _]
   (:current-action db)))

(rf/reg-sub
 :location
 (fn [db _]
   (:location db)))

(rf/reg-sub
 :toggle-label
 (fn [{:keys [current-action]}]
   (if (= current-action :drawing-boundary)
     "Select Point"
     "Draw Boundary")))

;; -- Reagent Forms ------------------------------------------------------------------
(defn point-canvas []
  [:div.content
   [:canvas#point-canvas.canvas
    {:on-click  (fn [^js e] (rf/dispatch [:point-click {:x (.. e -nativeEvent -offsetX) :y (.. e -nativeEvent -offsetY)}]))
     :width 500
     :height 500}]
   [:div.current-action 
    (let [current-action @(rf/subscribe [:current-action])]
      (if (= current-action :drawing-boundary) "Drawing boundary" "Selecting Point"))]])

(defn buttons []
  [:div.content
   [:div
    [:button.btn.btn-primary
     {:on-click #(rf/dispatch [:toggle (not @(rf/subscribe [:current-action]))])}
     @(rf/subscribe [:toggle-label])]
    [:button.btn.btn-primary
     {:style    {:margin 10}
      :on-click #(rf/dispatch [:reset-boundary])}
     "Reset boundary"]]
   [:button.btn.btn-primary
    "Calculate"]])

(defn points []
  (fn []
    [:div
     [:ul
      (let [points @(rf/subscribe [:points])]
        (for [[{:keys [x y]} key] (zipmap points (range (count points)))]
          [:li {:key key} (str "(" x "," y ")")]))]]))

(defn location []
  [:div.content.result
   @(rf/subscribe [:location])])

;; -- App -------------------------------------------------------------------------
(defn app []
  [:div.container
   [point-canvas]
   [buttons]
   [location]
   [points]])

(comment (rf/dispatch-sync [:initialize]))
(comment (rf/dispatch-sync [:reset-boundary]))
(comment (rf/dispatch-sync [:update-canvas]))

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