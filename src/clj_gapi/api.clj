(ns clj-gapi.api
  (:require
   [clj-http.client :as http]
   [clojure.string :as string]

   [cheshire.core :as json]
   [clj-http.util :refer [url-encode]]

   [clj-gapi.auth :as auth]
   [clj-gapi.params :as params]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helper Fns

(defn get-response
  "Check an HTTP response, JSON decoding the body if valid"
  [res]
  (let [acceptable-responses #{200 201 204}]
    (if (acceptable-responses (:status res))
      (json/parse-string (:body res) true)
      (let [body (json/parse-string (:body res) true)]
        {:error (-> body :error :message)}))))

(defn get-url
  "Replace URL path parameters with their values"
  [base-url path params args]
  (let [replace-fn #(string/replace %1 (str "{" %2 "}") (url-encode (args %2)))]
    (str base-url
      (reduce replace-fn path params))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public Fns

(defmulti callfn
  "Retrieve an anonymous function that makes the proper call for the
   supplied method description."
  (fn [base-url method] (:httpMethod method)))

(defmethod callfn "GET" [base-url {path :path method_params :parameters}]
  (fn ([state args]
       {:pre [(params/hasreqs? method_params args)]}
       (let [url (get-url base-url path (params/get-path-params method_params) args)
             opts {:throw-exceptions false
                   :query-params args}
             params (auth/call-params state opts)]
         (get-response (http/get url params))))))

(defmethod callfn "POST" [base-url {path :path method_params :parameters}]
  (fn ([state args body]
       {:pre [(params/hasreqs? method_params args)]}
       (let [url (get-url base-url path (params/get-path-params method_params) args)
             opts {:throw-exceptions false
                   :body (json/generate-string body)
                   :content-type :json
                   :query-params args}
             params (auth/call-params state opts)]
         (get-response (http/post url params))))))

(defmethod callfn "DELETE"
  [base-url {path :path method_params :parameters}]
  (fn ([state args]
       {:pre [(params/hasreqs? method_params args)]}
       (let [url (get-url base-url path (params/get-path-params method_params) args)
             opts {:throw-exceptions false
                   :content-type :json
                   :query-params args}
             params (auth/call-params state opts)]
         (get-response (http/delete url params))))))

(defmethod callfn "PUT" [base-url {path :path method_params :parameters}]
  (fn ([state args body]
       {:pre [(params/hasreqs? method_params args)]}
       (let [url (get-url base-url path (params/get-path-params method_params) args)
             opts {:throw-exceptions false
                   :body (json/generate-string body)
                   :content-type :json
                   :query-params args}
             params (auth/call-params state opts)]
         (get-response (http/put url params))))))

(defmethod callfn "PATCH" [base-url {path :path method_params :parameters}]
  (fn ([state args body]
       {:pre [(params/hasreqs? method_params args)]}
       (let [url (get-url base-url path (params/get-path-params method_params) args)
             opts {:throw-exceptions false
                   :body (json/generate-string body)
                   :content-type :json
                   :query-params args}
             params (auth/call-params state opts)]
         (get-response (http/patch url params))))))
