(ns main.exfn.scratch
  (:require [clojure.set :as set]))

(defn check [letters words]
  (filter (fn [w] (set/subset? (set w) (set letters))) words))

(check "abcdefghijklmnop" ["king" "bishop" ])

