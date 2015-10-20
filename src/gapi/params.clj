(ns gapi.params)

(defn match-params
  "Filter the parameters by a given function"
  [params func]
  (let [required (filter (fn [[k v]] (func v)) params)]
    (map (fn [[k v]] (name k)) required)))

(defn get-path-params
  "Return a vector of parameter names which appear in the URL path"
  [params]
  (match-params params (fn [p] (= "path" (:location p)))))

(defn get-required-params
  "Return a vector of required parameter names"
  [params]
  (match-params params (fn [p] (:required p))))

(defn hasreqs?
  "Determine whether the required params are present in the arguments"
  [params args]
  (reduce #(and %1 %2) true (map #(contains? args %1) (get-required-params params))))
