(ns exfn.helpers)

(defn update-if [pred f]
  (fn [x] (if (pred x) (f x) x)))
