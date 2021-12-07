(ns exfn.app
  (:require [reagent.dom :as dom]
            [re-frame.core :as rf]
            [goog.string.format]
            [clojure.set :as set]))


;; npm install
;; npx shadow-cljs watch app
;; connect to browser
;; connect to repl
;; -- Helpers ------------------------------------------------------------------------------------
(defn check [letters words]
  (let [letters (set letters)]
    (filter (fn [w]
              (js/console.log w)
              (set/subset? (set (:word w)) letters)) words)))

(defn get-next-id [people]
  (if (seq people)
    (-> (apply max-key :id people)
        (:id)
        (inc))
    0))

(comment (check "abcdefghijklmnop" [{:id 0 :word "bishop"}
                                    {:id 1 :word "king"}]))
;;-- Events and Effects --------------------------------------------------------------------------
(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:words []
    :current-word ""
    :winners []
    :letters #{}}))

(rf/reg-event-db 
 :add-word
 (fn [{:keys [words] :as db} _]
   (let [new-words (conj words {:id   (get-next-id (:words db))
                                :word (db :current-word)})]
     (-> db
         (assoc :words new-words)
         (assoc :current-word "")
         (assoc :winners (check (db :letters) new-words))))))

(rf/reg-event-db
 :word-change
 (fn [db [_ word]]
   (assoc db :current-word word)))

(rf/reg-event-db
 :delete
 (fn [{:keys [words] :as db} [_ id-to-remove]]
   (let [new-words (remove (fn [{:keys [id]}] (= id-to-remove id)) words)]
     (-> db
         (assoc :words new-words)
         (assoc :winners (check (db :letters) new-words))))))

(rf/reg-event-db
 :letters-change
 (fn [db [_ letters]]
   (-> db
    (assoc :letters letters)
    (assoc :winners (check letters (db :words))))))

;; -- Subscriptions ------------------------------------------------------------------
(rf/reg-sub
 :words
 (fn [db _]
   (:words db)))

(rf/reg-sub
 :letters
 (fn [db _]
   (:letters db)))

(rf/reg-sub
 :current-word
 (fn [db _]
   (:current-word db)))

(rf/reg-sub
 :winners
 (fn [db _]
   (:winners db)))

;; -- Reagent Forms ------------------------------------------------------------------
(defn add-words-and-letters []
  (let [letters @(rf/subscribe [:letters])]
    [:div
     [:div {:style {:margin 10
                    :padding 10}}
      [:label "Enter letters"]
      [:input {:type "text"
               :on-change #(rf/dispatch-sync [:letters-change (-> % .-target .-value)])
               :value letters}]]
     [:div
      [:label (str (count (set letters)) " distinct letters entered.")]]
     [:div {:style {:margin 10
                    :padding 10}}
      [:label "Add word: "]
      [:input {:type "text"
               :on-change #(rf/dispatch-sync [:word-change (-> % .-target .-value)])
               :value @(rf/subscribe [:current-word])
               :style {:margin 10}}]
      [:button.btn.btn-primary
       {:on-click #(rf/dispatch [:add-word])}
       [:i.fas.fa-plus]]]]))

(defn entered-words []
  (let [words @(rf/subscribe [:words])]
    [:div
     [:h2 (str "Entered words (" (count words) "): ")]
     [:ul
      (for [{:keys [id word]} words]
        [:li {:key id} [:div [:button.delete.btn
                              {:on-click #(rf/dispatch [:delete id])}
                              [:i.fas.fa-trash-alt]] word]])]]))

(defn winners []
  (let [winners @(rf/subscribe [:winners])]
    [:div
     [:h2 (str "Winners below (" (count winners) "):") [:i.fas.fa-grin-stars
                                                        {:style {:visibility (if (>= (count winners) 3)
                                                                               :visible
                                                                               :hidden)
                                                                 :color :orange} }]]
     [:ul
      (for [{:keys [id word]} winners]
        [:li {:key id} [:label word]])]]))

;; -- App -------------------------------------------------------------------------
(defn app []
  [:div.container
   [add-words-and-letters]
   [entered-words]
   [winners]
   ])

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



(comment 
  
  (defn check [letters words]
    (let [letters (set letters)]
      (filter (fn [w]
                (set/subset? (set w) letters)) words)))
)
