(ns gapi.api
  (:require
   [clj-http.client :as http]
   [clojure.string :as string]

   [cheshire.core :as json]
   [clj-http.util :refer [url-encode]]

   [gapi.auth :as auth]
   [gapi.params :as params]))

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
  [base_url path params args]
  (str base_url
    (reduce #(string/replace %1 (str "{" %2 "}") (url-encode (args %2))) path params)))

(defmulti callfn
  "Retrieve an anonymous function that makes the proper call for the
   supplied method description."
  (fn [base_url method] (:httpMethod method)))

(defmethod callfn "GET" [base_url {path :path method_params :parameters}]
  (fn ([state args]
       {:pre [(params/hasreqs? method_params args)]}
       (let [url (get-url base_url path (params/get-path-params method_params) args)
             params (auth/call-params state {:throw-exceptions false
                                             :query-params args})]
         (get-response (http/get url params))))))

(defmethod callfn "POST" [base_url {path :path method_params :parameters}]
  (fn ([state args body]
       {:pre [(params/hasreqs? method_params args)]}
       (let [url (get-url base_url path (params/get-path-params method_params) args)
             params (auth/call-params state {:throw-exceptions false
                                             :body (json/generate-string body)
                                             :content-type :json
                                             :query-params args})]
         (get-response (http/post url params))))))

(defmethod callfn "DELETE"
  [base_url {path :path method_params :parameters}]
  (fn ([state args]
       {:pre [(params/hasreqs? method_params args)]}
       (let [url (get-url base_url path (params/get-path-params method_params) args)
             params (auth/call-params state {:throw-exceptions false
                                             :content-type :json
                                             :query-params args})]
         (get-response (http/delete url params))))))

(defmethod callfn "PUT" [base_url {path :path method_params :parameters}]
  (fn ([state args body]
       {:pre [(params/hasreqs? method_params args)]}
       (let [url (get-url base_url path (params/get-path-params method_params) args)
             params (auth/call-params state {:throw-exceptions false
                                             :body (json/generate-string body)
                                             :content-type :json
                                             :query-params args})]
         (get-response (http/put url params))))))

(defmethod callfn "PATCH" [base_url {path :path method_params :parameters}]
  (fn ([state args body]
       {:pre [(params/hasreqs? method_params args)]}
       (let [url (get-url base_url path (params/get-path-params method_params) args)
             params (auth/call-params state {:throw-exceptions false
                                             :body (json/generate-string body)
                                             :content-type :json
                                             :query-params args})]
         (get-response (http/patch url params))))))
