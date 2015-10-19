(ns gapi.auth
  (:import
   [java.net URLEncoder])
  (:require
   [clojure.string :as string]

   [clj-http.client :as http]
   [cheshire.core :as json]))

(defrecord SimpleAuth [api_key])
(defrecord Auth [client_id client_secret redirect_url token])

(def ^{:private true} auth_url "https://accounts.google.com/o/oauth2/auth")
(def ^{:private true} token_url "https://accounts.google.com/o/oauth2/token")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helper Fns

(defn encode
  "Combine the key and value with an = and URL encode each part"
  [key val]
  (str (URLEncoder/encode (str key) "UTF-8") "=" (URLEncoder/encode (str val) "UTF-8")))

(defn generate-state
  "Generate a random string for the state"
  []
  (let [buff (make-array Byte/TYPE 10)]
    (-> (java.security.SecureRandom.)
      (.nextBytes buff))
    (-> (org.apache.commons.codec.binary.Base64.)
      (.encode buff)
      (String.))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public Fns

(defn create-auth
  "Return a state map used to manage the authentication session. 
   Can accept either an API key for simple API access or a client
   ID, client secret and redirect URL for OAuth2. See
   https://developers.google.com/console to generate keys."
  ([api_key]
   (->SimpleAuth api_key))
  ([client_id, client_secret, redirect_url]
   (->Auth client_id client_secret redirect_url nil)))

(defmulti call-params
  "Update the call parameters with the authentication details"
  (fn [auth params]
    (if (nil? auth)
      :none
      (if (string? (:token auth))
        :oauth
        :simple))))

(defmethod call-params :oauth [auth params]
  ;; TODO: check for expired auth token and call refresh if possible
  (let [headers (if (params :headers) (params :headers) {})]
    (assoc params :headers (assoc headers "Authorization" (str "Bearer " (:token auth))))))

(defmethod call-params :simple [auth, params]
  (assoc params :query-params (assoc (params :query-params) "key" (:api_key auth))))

(defmethod call-params :default [auth, params]
  params)

(defn is-valid?
  "Returns true if the authentication is valid, and in date."
  [auth]
  (if (:token auth)
    (< (System/currentTimeMillis) (:expires auth))
    false))

(defn with-auth-url
  "Retrieve a URL suitable for redirecting the user to for auth permissions. 
   Scopes should be supplied as a vector of required scopes. An optional third
   param is a map with access_type and approval_prompt keys."
  ([auth scopes] (with-auth-url auth scopes {:access_type "offline"
                                                   :approval_prompt "auto"}))
  ([auth scopes opts]
   (let [oauth2-state (generate-state)
         params [(encode "client_id" (:client_id auth))
                 (encode "redirect_uri" (:redirect_url auth))
                 (encode "scope" (string/join " " scopes))
                 (encode "state" oauth2-state)
                 "response_type=code"
                 (encode "access_type" (opts :access_type))
                 (encode "approval_prompt" (opts :approval_prompt))]
         auth-url (str auth_url "?" (string/join "&" params))]
     (assoc auth :state oauth2-state :auth_url auth-url))))

(defn with-exchanged-token
  "Handle the user response from the oauth flow and retrieve a valid
   auth token. Returns true on success, false on failure."
  [auth, code, checkstate]
  (if (= (:state auth) checkstate)
    (let [params [(encode "code" code)
                  (encode "client_id" (:client_id auth))
                  (encode "redirect_uri" (:redirect_url auth))
                  (encode "client_secret" (:client_secret auth))
                  "grant_type=authorization_code"]
          http_resp (http/post token_url {:body (string/join "&" params)
                                          :content-type "application/x-www-form-urlencoded"})
          resp (json/parse-string (http_resp :body) true)]
      (assoc auth :token (resp :access_token)
        :exchange-status :success
        :refresh (resp :refresh_token)
        :expires (+ (System/currentTimeMillis) (* (resp :expires_in) 1000))))
    (assoc auth :exchange-status :fail)))

(defn with-refreshed-token
  "Generate a new authentication token using the refresh token"
  [auth]
  (if (:refresh auth)
    (let [params [(encode "client_id" (:client_id auth))
                  (encode "client_secret" (:client_secret auth))
                  (encode "refresh_token" (:refresh auth))
                  "grant_type=refresh_token"]
          http_resp (http/post token_url {:body (string/join "&" params)
                                          :content-type "application/x-www-form-urlencoded"})
          resp (json/parse-string (http_resp :body) true)
          expires (+ (System/currentTimeMillis)
                    (* (resp :expires_in) 1000))]
      (assoc auth :token (:access_token resp) :expires expires))
    auth))
