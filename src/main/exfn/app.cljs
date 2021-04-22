(ns exfn.app
  (:require [reagent.dom :as dom]
            [re-frame.core :as rf]
            [goog.string.format]))

;; -- Helpers ------------------------------------------------------------------------------------
(defn det [r p1 p2]
  (- (* (- (:x p1) (:x r))
        (- (:y p2) (:y r)))
     (* (- (:x p2) (:x r))
        (- (:y p1) (:y r)))))

(defn calculate-w [r [v1 v2]]
  (if (<= (:y v1) (:y r))
    (if (and (> (:y v2) (:y r)) (pos? (det r v1 v2)))
      1 0)
    (if (and (<= (:y v2) (:y r)) (neg? (det r v1 v2)))
      -1 0)))

(defn is-point-outside? [point points]
  (let [closed-points (conj points (first points))]
    (->> (map (partial calculate-w point) (partition 2 1 closed-points))
         (reduce +)
         (zero?))))

;;-- Events and Effects --------------------------------------------------------------------------
(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:points []
    :current-action :drawing-boundary
    :point {}
    :should-fill false
    :location "Requires Calculation"
    :undo-stack []
    :redo-stack []}))

(defn clear-canvas [canvas ctx]
  (let [w (.-width canvas)
        h (.-height canvas)]
    (.beginPath ctx)
    (set! (.-fillStyle ctx) "white")
    (.rect ctx 0 0  w h)
    (.fill ctx)))

(defn draw-guides [ctx points x y]
  (.beginPath ctx)
  (.lineTo ctx 0 y)
  (.lineTo ctx 500 y)
  (set! (.-lineWidth ctx) 1.0)
  (set! (.-strokeStyle ctx) "green")
  (.stroke ctx))

(defn draw-selected-point [ctx x y]
  (when (and (not (nil? x)) (not (nil? y)))
    (.beginPath ctx)
    (set! (.-strokeStyle ctx) "black")
    (set! (.-fillStyle ctx) "blue")
    (.arc ctx x y 4 0 (* 2 (.-PI js/Math)) 0)
    (.stroke ctx)
    (.fill ctx)))

(defn draw-boundary [ctx points should-fill]
  (set! (.-lineWidth ctx) 2.0)
  (set! (.-strokeStyle ctx) "black")
  (.beginPath ctx)
  (dorun (map (fn [{:keys [x y]}]
                (.arc ctx x y 1 0 (* 2 (.-PI js/Math)) 1)
                (.lineTo ctx x y)) points))
  (.stroke ctx)
  (when should-fill
    (set! (.-fillStyle ctx) "yellow")
    (.fill ctx)))

(rf/reg-fx
 :draw-canvas
 (fn [[points {:keys [x y]} should-fill]]
   (let [canvas (.getElementById js/document "point-canvas")
         ctx (.getContext canvas "2d")]
     (.scale ctx 1 1)
     (clear-canvas canvas ctx)
     (draw-boundary ctx points should-fill)
     (draw-selected-point ctx x y)
     #_(draw-guides ctx points x y))))

(rf/reg-event-fx
 :update-canvas
 (fn [{:keys [db]} _]
   {:db db
    :draw-canvas [(:points db) (:point db) (:should-fill db)]}))

(rf/reg-event-fx
 :point-click
 (fn [{:keys [db]} [_ xy]]
   (cond
     ; if user is drawing boundary...
     (= :drawing-boundary (db :current-action))
     (let [updated-points (conj (db :points) xy)]
       {:db          (-> db
                         (assoc :points updated-points)
                         (assoc :location (if (is-point-outside? (db :point) updated-points) "Outside" "Inside"))
                         (assoc :redo-stack [])
                         (update :undo-stack conj (db :points)))
        :draw-canvas [updated-points (:point db) (db :should-fill)]})

     ; if user is selecting a point.
     (= :selecting-point (db :current-action))
     {:db          (-> db
                       (assoc :point xy)
                       (assoc :location (if (is-point-outside? xy (db :points)) "Outside" "Inside")))
      :draw-canvas [(:points db) xy (db :should-fill)]})))

(rf/reg-event-fx
 :reset-boundary
 (fn [{:keys [db]} _]
   {:draw-canvas [[] (:point db) (:should-fill db)]
    :db          (-> db
                     (assoc :points [])
                     (assoc :location "Outside")
                     (update :undo-stack conj (db :points))
                     (assoc :current-action :drawing-boundary))}))

(rf/reg-event-db
 :calculate
 (fn [{:keys [points point] :as db} _]
   (if (is-point-outside? point points)
     (assoc db :location "Outside")
     (assoc db :location "Inside"))))

(rf/reg-event-db
 :toggle
 (fn [{:keys [current-action] :as db} _]
   (let [toggle {:drawing-boundary :selecting-point :selecting-point :drawing-boundary}]
     (assoc db :current-action (toggle current-action)))))

(rf/reg-event-fx
 :toggle-fill
 (fn [{:keys [db]} _]
   (let [should-fill? (not (:should-fill db))]
     {:db (assoc db :should-fill should-fill?)
      :draw-canvas [(:points db) (:point db) should-fill?]})))

;; on undo, we need to make points equal to result of popping undo-stack
;; make undo-stack equal to popping undo-stack
;; push points tp redo-stack
(rf/reg-event-fx
 :undo
 (fn [{:keys [db]} _]
   (if (empty? (:undo-stack db))
     {:db db}
     (let [last-dropped (vec (butlast (db :points)))]
       {:db          (-> db
                         (assoc :points last-dropped)
                         (assoc :location (if (is-point-outside? (db :point) last-dropped) "Outside" "Inside"))
                         (update :undo-stack pop)
                         (update :redo-stack conj (db :points)))
        :draw-canvas [last-dropped (:point db) (:should-fill db)]}))))

(rf/reg-event-fx
 :redo
 (fn [{:keys [db]} _]
   (if (empty? (:redo-stack db))
     {:db db}
     (let [new-points (peek (:redo-stack db))
           new-undo (:points db)
           new-redo (pop (:redo-stack db))]
       {:db          (-> db
                         (assoc :points new-points)
                         (assoc :location (if (is-point-outside? (db :point) new-points) "Outside" "Inside"))
                         (assoc :redo-stack new-redo)
                         (assoc :undo-stack new-undo))
        :draw-canvas [new-points (:point db) (:should-fill db)]}))))

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

(rf/reg-sub
 :point-count
 (fn [{:keys [points]}]
   (count points)))

;; -- Reagent Forms ------------------------------------------------------------------
(defn point-canvas []
  [:div.content
   [:canvas#point-canvas.canvas
    {:on-click  (fn [^js e] (rf/dispatch-sync [:point-click {:x (.. e -nativeEvent -offsetX) :y (.. e -nativeEvent -offsetY)}]))
     :width 500
     :height 500}]
   [:div.current-action
    (let [current-action @(rf/subscribe [:current-action])]
      (if (= current-action :drawing-boundary)
        (str "Drawing boundary. Points: " @(rf/subscribe [:point-count]))
        "Selecting Point"))]])

(defn buttons []
  [:div.content
   [:div
    [:button.btn.btn-primary
     {:on-click #(rf/dispatch [:toggle (not @(rf/subscribe [:current-action]))])}
     @(rf/subscribe [:toggle-label])]
    [:button.btn.btn-danger
     {:on-click #(rf/dispatch [:reset-boundary])}
     "Reset boundary"]]
   [:button.btn.btn-primary
    {:on-click #(rf/dispatch [:toggle-fill])}
    "Toggle fill"]
   [:button.btn.btn-primary
    {:on-click #(rf/dispatch [:undo])}
    "Undo"]
   [:button.btn.btn-primary
    {:on-click #(rf/dispatch [:redo])}
    "Redo"]])

(defn location []
  [:div.content.result
   @(rf/subscribe [:location])])

;; -- App -------------------------------------------------------------------------
(defn app []
  [:div.container
   [point-canvas]
   [location]
   [buttons]
   [:p "When calculating the point the algorithm will automatically close the polygon (i.e. make the last point = the first point), which is why it may
        make it look like its inside while you are drawing."]])

(comment (rf/dispatch-sync [:initialize]))
(comment (rf/dispatch-sync [:reset-boundary]))
(rf/dispatch [:update-canvas])
;only here for debugging / dev / testing.

;; -- After-Load --------------------------------------------------------------------
;; Do this after the page has loaded.
;; Initialize the initial db state.
(defn ^:dev/after-load start
  []
  (dom/render [app]
              (.getElementById js/document "app")))

(defn ^:export init []
  (start))

(defonce initialize (rf/dispatch-sync [:initialize]))       ; dispatch the event which will create the initial state. 

