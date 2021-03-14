(ns exfn.app
  (:require [reagent.dom :as dom]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]))

(def data
  [["Name" "Weapon" "Side" "Height"]
   ["Luke Skywalker" "Blaster" "Good" 1.72 0]
   ["Leia Organa" "Blaster" "Good" 1.5 1]
   ["Han Solo" "Blaster" "Good" 1.8 2]
   ["Obi-Wan Kenobi" "Light Saber" "Good" 1.82 3]
   ["Chewbacca" "Bowcaster" "Good" 2.28 4]
   ["Darth Vader" "Light Saber" "Bad" 2.03 5]])

(comment
  (str "DEV NOTES.
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

(rf/reg-event-db
  :initialize
  (fn [_ _]
    {:tables {:new-hope {:header (first data)
                         :rows   (vec (rest data))}}}))

;; Draggable list using component local state.
(defn put-before [items pos item]
  (let [items (remove #{item} items)
        head (take pos items)
        tail (drop pos items)]
    (concat head [item] tail)))

(defn draggable-list [{:keys [on-reorder]
                       :or   {on-reorder (fn [])}} & items]
  (let [items (vec items)
        s (reagent/atom {:order (range (count items))})]
    (fn []
      [:ul
       (doall
         (for [[i pos] (map vector (:order @s) (range))]
           [:li
            {:key           i
             :style         {:border (when (= i (:drag-index @s))
                                       "1px solid blue")}
             :draggable     true
             :on-drag-start #(swap! s assoc :drag-index i)
             :on-drag-over  (fn [e]
                              (.preventDefault e)
                              (swap! s assoc :drag-over pos)
                              (swap! s update :order put-before pos (:drag-index @s)))
             :on-drag-leave #(swap! s assoc :drag-over :nothing)
             :on-drag-end   (fn []
                              (swap! s dissoc :drag-over :drag-index)
                              (on-reorder (map items (:order @s))))}
            (get items i)]))])))

;; Sortable Table
(rf/reg-sub
  :table
  (fn [db [_ key]]
    (get-in db [:tables key])))

(comment
  (let [header (map keyword (first data))
        rows (rest data)]
    (->> (map #(zipmap header %) rows)
         (map #(assoc % :Side (keyword (:Side %))))
         (filter #(= :Good (:Side %)))
         (cljs.pprint/print-table))))

(defn sortable-table [table-key]
  (let [s (reagent/atom {})]                                ;component local state
    (fn [table-key]
      (let [table @(rf/subscribe [:table table-key])
            key (:sort-key @s)
            direction (:sort-direction @s)
            rows (cond->> (:rows table)
                          key (sort-by #(nth % key))
                          (= :ascending direction) reverse)
            sorts [key direction]]
        [:div
         [:table
          [:thead
           [:tr
            (for [[i h] (map vector (range) (:header table))]
              [:th
               {:style    {:line-height "1em"
                           :cursor      :pointer}
                :on-click #(cond
                             (= [i :descending] sorts)
                             (swap! s assoc
                                    :sort-direction :ascending)
                             (= [i :descending] sorts)
                             (swap! s dissoc
                                    :sort-direction :sort-key)
                             :else
                             (swap! s assoc
                                    :sort-key i
                                    :sort-direction :descending))}
               h
               [:div {:style {:display        :inline-block
                              :vertical-align :middle
                              :line-height    "1em"}}
                [:div {:style {:color (if (= [i :descending] sorts)
                                        :black
                                        "#aaa")}}

                 [:i.fas.fa-sort-up]]
                [:div {:style {:color (if (= [i :ascending] sorts)
                                        :black
                                        "#aaa")}}
                 [:i.fas.fa-sort-down]]]])]]
          [:tbody
           (for [row rows]
             [:tr
              (for [v row]
                [:td v])
              [:td [:i.fas.fa-trash-alt]]])]]]))))

(comment
  (let [e {:id 0 :name "Leia" :weapon "Blaster" :height 1.5 :side "Good"}]
    (conj (mapv (fn [x] [:td x]) (vals (dissoc e :id)))
          (vector [:td [:i.fas.fa-trash-alt {:id (:id e)}]]))))

(defn app []
  (let [s (reagent/atom {})]
    (fn []
      [:div.container
       [:h1 "Draggable List"]
       [draggable-list
        {:on-reorder (fn [item-order]
                       (swap! s assoc :order item-order))}
        "a" "b" "c" "d" "z"]
       [:h1 "Characters"]
       [:button.btn.btn-primary
        {:style {:margin 5}}
        [:i.fas.fa-plus]]
       [:div
        [sortable-table :new-hope]]])))

(rf/dispatch-sync [:initialize])

(defn ^:dev/after-load start
  []
  (rf/dispatch-sync [:initialize])
  (dom/render [app]
              (.getElementById js/document "app")))

(defn ^:export init []
  (js/console.log "Lets learn re-frame!")
  (start))