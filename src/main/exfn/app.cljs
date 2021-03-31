(ns exfn.app
  (:require [reagent.dom :as dom]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]
            [exfn.helpers :as helpers]
            [goog.string :as gstring]
            [goog.string.format]))

(comment
  (str "DEV NOTE.
        --------------------------------------------------------------------------------------------------")
  (str "Run with shadow using npx shadow-cljs watch app"
       "Run repl after connecting in the browser."
       "To switch to cljs repl evaluate this, don't use cljs drop down in repl tab.")
  (shadow/repl :app)
  (str "Now test that the repl is connected"
       "you should see the alert go to the repl, return nil and also an alert in the browser.")
  (js/alert "hello!")
  (str "If dependencies aren't being resolved run shadow-cljs pom and reload all from disk in File menu.
        --------------------------------------------------------------------------------------------------"))

;;-- Events --------------------------------------------------------------------------
(rf/reg-event-db
  :initialize
  (fn [_ _]
    {:time-remaining 0}))

;(rf/reg-event-db
;  :time-up
;  (fn [db [_ _]]
;    (update db :time-entered inc)))

(rf/reg-event-db
  :start
  (fn [db [_ time]]
    (assoc db :time-remaining time)))

;; -- Subscriptions ------------------------------------------------------------------
(rf/reg-sub
  :time-remaining
  (fn [db _]
    (let [time (:time-remaining db)]
      (gstring/format "%02d" (if (nil? time) 0 time)))))

;; -- Reagent Forms ------------------------------------------------------------------
(defn countdown-initiator []
  (let [time-entered (reagent.core/atom 0)]
    (fn []
      [:div.initiator
       [:div.timer @time-entered]
       [:div
        [:button.btn.up
         {:on-click #(swap! time-entered inc)}
         [:i.fas.fa-chevron-up]]
        [:button.btn.down
         {:on-click #(if (> @time-entered 0)
                       (swap! time-entered dec))}
         [:i.fas.fa-chevron-down]]]
       [:div
        [:button.btn.btn-danger {:on-click #(rf/dispatch [:start @time-entered])
                                 :style    {:margin-top 10}} "Start Countdown"]]])))

(defn selected-time []
  [:div.countdown @(rf/subscribe [:time-remaining])])

;; -- App -------------------------------------------------------------------------
(defn app []
  [:div.container
   [:h1 "Countdown"]
   [countdown-initiator]
   [selected-time]])

;; -- After-Load --------------------------------------------------------------------
;; Do this after the page has loaded.
;; Initialize the initial db state.
(defonce initialize (rf/dispatch-sync [:initialize]))       ; dispatch the event which will create the initial state.)
;
(defn ^:dev/after-load start
  []
  (dom/render [app]
              (.getElementById js/document "app")))

(defn ^:export init []
  (js/console.log "Lets learn re-frame!")
  (start))