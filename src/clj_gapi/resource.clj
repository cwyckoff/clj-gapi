(ns clj-gapi.resource
  (:require
   [clj-http.client :as http]
   [clojure.string :as string]

   [cheshire.core :as json]

   [clj-gapi.params :as params]
   [clj-gapi.api :as api]))

(defn docstring
  "Return a description for this method"
  [method]
  (let [required (params/get-required-params (:parameters method))]
    (str (:description method) "\n"
      "Required parameters: " (string/join " " required)
      "\n")))

(defn get-method-name
  "Get a friendly namespace-esque string for the method"
  [name]
  (let [parts (clojure.string/split name #"\.")]
    (str (clojure.string/join "." (pop parts)) "/" (last parts))))

(defn arglists
  "Return an argument list for the method"
  [method]
  (let [base_args
        (if (= (:httpMethod method) "POST")
          '[[auth parameters body]]
          '[[auth parameters]])]
    base_args))

(defn extract-methods
  "Retrieve all methods from the given resource"
  [base_url resource]
  (reduce
    (fn [methods [key method]]
      (assoc methods
        (get-method-name (:id method))
        {:fn (api/callfn base_url method)
         :doc (docstring method)
         :arglists (arglists method)
         :scopes (:scopes method)}))
    {}
    (:methods resource)))

(defn build [base r]
  (reduce
    (fn [methods [key resource]]
      (merge methods
        (extract-methods base resource)
        (build base resource)))
    {}
    (:resources r)))
