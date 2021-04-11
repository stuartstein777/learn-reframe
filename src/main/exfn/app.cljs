(ns exfn.app
  (:require [reagent.dom :as dom]
            [re-frame.core :as rf]
            [goog.string.format]))

;;-- Events and Effects --------------------------------------------------------------------------
(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:points []
    :drawing-boundary? false
    :point {}
    :selecting-point? false
    :boundary-button-label "Draw Boundary"}))

(defn clear-canvas [canvas ctx]
  (let [w (.-width canvas)
        h (.-height canvas)]
    (.beginPath ctx)
    (set! (.-fillStyle ctx) "white")
    (.rect ctx 0 0  w h)
    (.fill ctx)))

(rf/reg-fx
 :update-canvas
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
     (.stroke ctx))))

(rf/reg-event-fx
 :update-canvas
 (fn [{:keys [db]} _]
   {:db db
    :update-canvas (:points db)}))


(rf/reg-event-fx
 :point-click
 (fn [{:keys [db]} [_ xy]]
   (if (:drawing-boundary? db)
     (let [updated-points (conj (db :points) xy)]
       {:db          (assoc db :points updated-points)
        :update-canvas updated-points}))))

(rf/reg-event-db
 :toggle-drawing-boundary
 (fn [db [_ drawing?]]
   (-> db
       (assoc :drawing-boundary? drawing?)
       (assoc :boundary-button-label (if drawing? "Finish boundary" "Draw boundary")))))

(rf/reg-event-fx
 :reset-boundary
 (fn [cofx _]
   {:db
    (-> (:db cofx)
        (assoc :points [])
        (assoc :drawing-boundary? true)
        (assoc :boundary-button-label "Finish boundary"))
    :draw-points []}))

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
 :drawing-boundary?
 (fn [db _]
   (:drawing-boundary? db)))

;; -- Reagent Forms ------------------------------------------------------------------
(defn point-canvas []
  [:div.content
   [:canvas#point-canvas.canvas
    {:on-click  (fn [^js e] (rf/dispatch [:point-click {:x (.. e -nativeEvent -offsetX) :y (.. e -nativeEvent -offsetY)}]))
     :width 500
     :height 500}]])

(defn buttons []
  [:div.content
   [:button.btn.btn-primary
    {:on-click #(rf/dispatch [:toggle-drawing-boundary (not @(rf/subscribe [:drawing-boundary?]))])}
    @(rf/subscribe [:boundary-button-label])]
   [:button.btn.btn-primary
    {:style {:margin 10}
     :on-click #(rf/dispatch [:reset-boundary])}
    "Reset boundary"]
   [:button.btn.btn-primary
    {:on-click #(rf/dispatch-sync [:selecting-point?])}
    "Select Point"]])

(defn points []
  (fn []
    [:div
     [:ul
      (let [points @(rf/subscribe [:points])]
        (for [[{:keys [x y]} key] (zipmap points (range (count points)))]
          [:li {:key key} (str "(" x "," y ")")]))]]))

;; -- App -------------------------------------------------------------------------
(defn app []
  [:div.container
   [point-canvas]
   [buttons]
   [points] ])

(comment (rf/dispatch-sync [:initialize]))
(comment (rf/dispatch-sync [:reset-boundary]))
(rf/dispatch-sync [:update-canvas])

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

(let [points [{:x 227 :y 443}
              {:x 115 :y 239}
              {:x 105 :y 192}
              {:x 254 :y 142}]]
  (for [[{:keys [x y]} key] (zipmap points (range (count points)))]
    [:li {:key key}(str "(" x "," y ")")]))