(ns clj-gapi.params
  (:require [medley.core :refer [filter-vals map-keys]])
  (:import [java.net URLEncoder]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helper Fns

(defn encode
  "Combine the key and value with an = and URL encode each part"
  [k v]
  (str (URLEncoder/encode (str k) "UTF-8") "=" (URLEncoder/encode (str v) "UTF-8")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public Fns

(defn match-params
  "Filter the parameters by a given function"
  [params f]
  (->> (filter-vals f params)
    (map-keys name)
    keys))

(defn get-path-params
  "Return a vector of parameter names which appear in the URL path"
  [params]
  (match-params params #(= "path" (:location %))))

(defn get-required-params
  "Return a vector of required parameter names"
  [params]
  (match-params params :required))

(defn encode-params [m]
  (->> m
    (map (partial apply encode))
    vec))

(defn hasreqs?
  "Determine whether the required params are present in the arguments"
  [params args]
  (let [coll (map #(contains? args %) (get-required-params params))]
    (reduce #(and %1 %2) true coll)))
