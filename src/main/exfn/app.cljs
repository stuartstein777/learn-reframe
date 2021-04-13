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

(det {:x 6 :y 6} {:x 5 :y 2} {:x 5 :y 10})

(defn calculate-w [r v1 v2]
  (if (<= (:y v1) (:y r))
    (if (and (> (:y v2) (:y r)) (> (det r v1 v2) 0))
      1 0)
    (if (and (<= (:y v2) (:y r)) (< (det r v1 v2) 0))
      -1 0)))

(defn is-point-outside? [point points]
  (let [closed-points (conj points (first points))
        w (->> (map (partial calculate-w point) closed-points (rest closed-points))
               (reduce + 0))]
    (zero? w)))

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
     (set! (.-strokeStyle ctx) "black")
     (dorun (map (fn [{:keys [x y]}]
                   (.arc ctx x y 0.0 0 (* 2 (.-PI js/Math)) 0)
                   (.lineTo ctx x y)) points))
     (.stroke ctx)
     (.fill ctx)
     (.beginPath ctx)
     (when (and (not (nil? x)) (not (nil? y)))
       (.arc ctx x y 4 0 (* 2 (.-PI js/Math)) 0)
       (set! (.-strokeStyle ctx) "black")
       (.stroke ctx)
       (set! (.-fillStyle ctx) "blue")
       (.fill ctx)))))

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
   (if (is-point-outside? point points)
     (assoc db :location "Outside")
     (assoc db :location "Inside"))))

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

(rf/reg-sub
 :point-count
 (fn [{:keys [points]}]
   (count points)))

;; -- Reagent Forms ------------------------------------------------------------------
(defn point-canvas []
  [:div.content
   [:canvas#point-canvas.canvas
    {:on-click  (fn [^js e] (rf/dispatch [:point-click {:x (.. e -nativeEvent -offsetX) :y (.. e -nativeEvent -offsetY)}]))
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
     {:style    {:margin 10}
      :on-click #(rf/dispatch [:reset-boundary])}
     "Reset boundary"]]
   [:button.btn.btn-info
    {:on-click #(rf/dispatch [:calculate])}
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
   [location]
   [buttons]
   [points]])


(comment (rf/dispatch-sync [:initialize]))
(comment (rf/dispatch-sync [:reset-boundary]))
(comment (rf/dispatch [:update-canvas]))
(rf/dispatch [:update-canvas]);only here for debugging / dev / testing.

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



(comment
  (defn de
    "Prints the vector part of (let [{:keys []} m]) map destructuring for you.
  Use at the REPL, copy the output, and use in your code.
  Given a map m returns the full recursive destructuring form ready for use in regular Clojure (let ...).
  If a specific key at a specific level of the map cannot be destructured, the destructuring stops there for that key.
  An INFO message will be printed with the specific key that cannot be destructured.
  If it encounters a vector which contains maps, it will destructure the first map in the vector.
  Supported map keys:
   - keywords
   - qualified keywords
   - symbols
   - qualified symbols
   - strings (not all string keys can be destructured, avoid strings)
  Usage:
  ```
  (def m  {:name     :alice
           :favorite {:music   [{:genre :rock}
                                {:genre :trance}]
                      :friends #{:bob :clara}}})
  (de m)
  ;=>
  [{:keys [name favorite]} m
   {:keys [music friends]} favorite
   [{:keys [genre]}] music]
  ```
  "
    ([m]
     (de m {:?symbol nil}))
    ([m {:keys [?symbol locals-index] :or {locals-index (atom {})}}]
     {:pre [(map? m)]}
     (transduce
      (map identity)
      (completing
       (fn [{:keys [locals-index] :as accum} [a-key a-val]]
         (let [[?via {:keys [local-to-index new-local keys-destruct]}] (key->?local a-key locals-index)
               ?destructure-more (val->?destructure-more a-val)
               accum'            (condp = ?via
                              ;choose type of destructuring

                                   :via-dest-vec
                              ;{:keys [] :strs []} style
                                   (update-in accum [:left-side keys-destruct]
                                              (fn [?v] (conjv ?v new-local)))

                                   :via-orig-key
                              ;{xyz :xyz} style
                                   (update accum :left-side
                                           (fn [m] (assoc m new-local (prepare-a-key a-key))))

                              ;else, no change
                                   accum)]


           (when local-to-index
             (swap! locals-index update local-to-index (fnil inc 1)))

      ;print message if a key cannot be destructured
           (when (nil? ?via)
             (println "INFO ::: Map key" (str "'" a-key "'") "does not support destructuring"))


           (if (and ?via ?destructure-more)
       ;'schedule' further destructuring
             (update-in accum' [:destructure-more] conj [new-local ?destructure-more])
       ;else
             (do
               (timbre/info "READY")
               accum'))))

       (fn [{:keys [left-side destructure-more locals-index] :as accum-final}]
         (let [left-side' (if (:map-in-vector? (meta m))
                            [left-side]
                            left-side)
               right-side (or ?symbol (symbol "m"))
               output'    [left-side' right-side]
               ret        (reduce
                           (fn [-output' [a-symbol m]]
                             (apply conj -output'
                                    (trampoline de m {:?symbol a-symbol :locals-index locals-index})))
                           output'
                           destructure-more)]

           ret)))
   ;reduce accum "state"
      {:left-side {} :destructure-more [] :locals-index locals-index}
      m))))