(ns exfn.app
  (:require [reagent.dom :as dom]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [goog.string :as gstring]
            [goog.string.format]))

(comment
  (str "DEV NOTE.
        --------------------------------------------------------------------------------------------------")
  (str "Run with shadow using npx shadow-cljs watch app"
       "If using INtelliJ and Cursive::"
       "Run repl after connecting in the browser."
       "To switch to cljs repl evaluate this, don't use cljs drop down in repl tab.")
  (shadow/repl :app)
  (str "Now test that the repl is connected"
       "you should see the alert go to the repl, return nil and also an alert in the browser.")
  (js/alert "hello!")
  (str "If dependencies aren't being resolved run shadow-cljs pom and reload all from disk in File menu.
        --------------------------------------------------------------------------------------------------"))

(defn dispatch-timer-event []
  (rf/dispatch [:tick]))

;;-- Events --------------------------------------------------------------------------
(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:time-remaining 0
    :countdown-visible false
    :initiator-visible true}))

(rf/reg-event-db
 :start
 (fn [db [_ time]]
   (-> db
       (assoc :time-remaining time)
       (assoc :countdown-visible true)
       (assoc :initiator-visible false))))

(rf/reg-event-db
 :reset
 (fn [db _]
   (-> db
       (assoc :time-remaining 0)
       (assoc :countdown-visible false)
       (assoc :initiator-visible true))))

(rf/reg-event-db
 :tick
 (fn [db _]
   (if (or (zero? (db :time-remaining)) (db :initiator-visible))
     db
     (update db :time-remaining dec))))

;; -- Subscriptions ------------------------------------------------------------------
(rf/reg-sub
 :time-remaining
 (fn [db _]
   (:time-remaining db)))

(rf/reg-sub
 :initiator-visible
 (fn [db _]
   (db :initiator-visible)))

(rf/reg-sub
 :countdown-visible
 (fn [db _]
   (db :countdown-visible)))

;; -- Reagent Forms ------------------------------------------------------------------
(defn countdown-initiator []
  (let [time-entered (reagent.core/atom 0)]
    (fn []
      (when @(rf/subscribe [:initiator-visible])
        [:div.initiator
         (let [time @time-entered]
           [:div.timer (gstring/format "%02d" (if (nil? time) 0 time))])
         [:div
          [:button.btn.up
           {:on-click #(swap! time-entered inc)}
           [:i.fas.fa-chevron-up]]
          [:button.btn.down
           {:on-click #(when (> @time-entered 0)
                         (swap! time-entered dec))}
           [:i.fas.fa-chevron-down]]]
         [:div
          [:button.btn.btn-danger.start
           {:on-click #(rf/dispatch [:start @time-entered])}
           "Start"]]]))))

(defn countdown []
  (let [time @(rf/subscribe [:time-remaining])
        color (if (<= time 10) :red :black)]
    [:div.countdown {:style {:visibility (if @(rf/subscribe [:countdown-visible]) :visible :hidden)}}
     [:div {:style {:color color}}
      (gstring/format "%02d" (if (nil? time) 0 time))]
     [:div
      [:button.btn.btn-secondary.reset
       {:on-click #(rf/dispatch [:reset])}
       "reset"]]]))

;; -- App -------------------------------------------------------------------------
(defn app []
  [:div.container
   [:h1 "Countdown"]
   [countdown-initiator]
   [countdown]])

;; -- After-Load --------------------------------------------------------------------
;; Do this after the page has loaded.
;; Initialize the initial db state.
(defonce initialize (rf/dispatch-sync [:initialize]))       ; dispatch the event which will create the initial state.)

; move this to the event, get the do-timer returned from this and use it to cleaInterval on countdown finishing.
; remove from this defonce.
; is this a co-effect?
(defonce do-timer (js/setInterval dispatch-timer-event 1000)) 

(defn ^:dev/after-load start
  []
  (dom/render [app]
              (.getElementById js/document "app")))

(defn ^:export init []
  (start))
