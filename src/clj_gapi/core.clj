(ns clj-gapi.core
  (:require [clojure.string :as string]

            [cheshire.core :as json]
            [clj-http.client :as http]

            [clj-gapi.resource :as resource]))

;; The base Discovery Service URL we will process
(def ^{:private true} discovery-url "https://www.googleapis.com/discovery/v1/apis")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public Fns

(defn list-apis
  "Directly eturn a list of APIs available to built, and endpoint discovery document URLs."
  []
  (let [discovery-doc (-> (http/get discovery-url)
                        :body
                        (json/parse-string true))]
    (->> (:items discovery-doc)
      (filter #(= (:preferred %) true))
      (map (juxt :name :version :discoveryRestUrl)))))

(defn build-resource
  "Given a discovery document URL, construct an map of names to functions that
   will make the various calls against the resources. Each call accepts a gapi.auth
   state map, and list of argument values, an in some cases a JSON encoded body to send
   (for write calls)"
  [api-url]
  (let [r (-> (http/get api-url)
            :body
            (json/parse-string true))]
    (resource/build (:baseUrl r) r)))

;; Memoized versions of API calls
(def ^{:private true} memoized-api-list (memoize list-apis))
(def ^{:private true} memoized-build (memoize build-resource))

(defn list-methods
  "List the available methods in a service"
  [service]
  (keys service))

(defn get-doc
  "Retrieve a docstring for the service call"
  [service method]
  (-> (service method) :doc))

(defn get-scopes
  "Retrieve a vector of scopes for the service"
  [service method]
  (-> (service method) :scopes))

(defn call
  "Call a service function"
  [method auth service & args]
  (println method)
  (-> (service method)
    :fn
    (apply auth args)))

(defn call-with-service
  "Call a service, constructing it if necessary"
  [method auth & args]
  (let [service-name (-> method
                       (string/split #"[\.\/]")
                       first)
        api (->> (memoized-api-list)
              (filter #(= (first %) service-name))
              last) 
        service (memoized-build (last api))]
    (apply call method auth service args)))

(defn call-without-auth
  "Call to a service without authentication"
  [method & args]
  (apply call-with-service nil method args))
