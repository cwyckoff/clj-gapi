(ns gapi.core
  (:require
   [clj-http.client :as http]
   [clojure.string :as string]

   [cheshire.core :as json]
   [clj-http.util :refer [url-encode]]

   [gapi.auth :as auth]
   [gapi.resource :as resource]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;; SETUP ;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; The base Discovery Service URL we will process
(def ^{:private true} discovery_url "https://www.googleapis.com/discovery/v1/apis")
(def ^{:private true} cache (atom {}))

(defn- build-ns
  "Create an entry in a namespace for the method"
  [auth [mname method]]
  (let [name (str "gapi." mname)
        parts (clojure.string/split name #"[\.\/]")
        namespace (symbol (clojure.string/join "." (pop parts)))]
    (if (= nil (find-ns namespace)) (create-ns namespace))
    (intern namespace
      (with-meta (symbol (last parts)) {:doc (:doc method) :arglists (:arglists method)})
      (partial (:fn method) auth))
    name))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;  API  ;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-apis
  "Directly eturn a list of APIs available to built, and endpoint discovery document URLs."
  []
  (let [discovery-doc (json/parse-string ((http/get discovery_url) :body) true)]
    (map #(vector (:name %1) (:version %1) (:discoveryRestUrl %1))
      (filter #(= (:preferred %1) true) (:items discovery-doc)))))

(defn build
  "Given a discovery document URL, construct an map of names to functions that
   will make the various calls against the resources. Each call accepts a gapi.auth
   state map, and list of argument values, an in some cases a JSON encoded body to send
   (for write calls)"
  [api_url]
  (let [r (json/parse-string ((http/get api_url) :body) true)]
    (resource/build-resource (:baseUrl r) r)))

(defn list-methods
  "List the available methods in a service"
  [service]
  (keys service))

(defn get-doc
  "Retrieve a docstring for the service call"
  [service method]
  ((service method) :doc))

(defn get-scopes
  "Retrieve a vector of scopes for the service"
  [service method]
  ((service method) :scopes))

(defn call
  "Call a service function"
  [auth service method & args]
  (apply ((service method) :fn) auth args))

;; Memoized versions of API calls
(def ^{:private true} m-list-apis (memoize list-apis))
(def ^{:private true} m-build (memoize build))

(defn im
  "Call a service, constructing if necessary"
  ([auth method_name & args]
   (let [service_name (first (clojure.string/split method_name #"[\.\/]"))
         api (last (filter #(= (first %1) service_name) (m-list-apis)))
         service (m-build (last api))]
     (apply call auth service method_name args))))

(defn anon-im
  "Call to a service without authentication"
  [method_name & args]
  (apply im nil method_name args))

(defn api-ns
  "Create a namespace for the API calls. TODO: details"
  ([api_url]
   (api-ns nil api_url))
  ([auth api_url]
   (let [service (m-build api_url)
         build-fn (partial build-ns auth)]
     (map build-fn service))))
