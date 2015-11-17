(ns clj-gapi.auth
  (:require [clojure.string :as string]

            [clj-http.client :as http]
            [cheshire.core :as json]

            [clj-gapi.params :as params]))

(def auth-url "https://accounts.google.com/o/oauth2/auth")
(def token-url "https://accounts.google.com/o/oauth2/token")

(defrecord SimpleAuth [api-key])
(defrecord Auth [client-id client-secret redirect-url token])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Helper Fns

(defn encode-auth-url-params [auth scopes oauth2-state opts]
  (let [{:keys [client-id redirect-url]} auth
        {:keys [access-type approval-prompt]} opts
        params {"client_id" client-id
                "redirect_uri" redirect-url
                "scope" (string/join " " scopes)
                "state" oauth2-state
                "access_type" access-type
                "approval_prompt" approval-prompt
                "response_type" "code"}]
    (string/join "&" (params/encode-params params))))

(defn encode-exchange-token-params [auth code]
  (let [{:keys [client-id redirect-url client-secret]} auth
        params {"code" code
                "client_id" client-id
                "redirect_uri" redirect-url
                "client_secret" client-secret
                "grant_type" "authorization_code"}]
    (string/join "&" (params/encode-params params))))

(defn encode-refresh-token-params [auth]
  (let [{:keys [client-id client-secret refresh-token]} auth
        params {"client_id" client-id
                "client_secret" client-secret
                "refresh_token" refresh-token
                "grant_type" "refresh_token"}]
    (string/join "&" (params/encode-params params))))

(defn expires [expires-in]
  (+ (System/currentTimeMillis) (* expires-in 1000)))

(defn generate-state
  "Generate a random string for the state"
  []
  (let [buff (make-array Byte/TYPE 10)]
    (-> (java.security.SecureRandom.)
      (.nextBytes buff))
    (-> (org.apache.commons.codec.binary.Base64.)
      (.encode buff)
      (String.))))

(defn post-token [params]
  (let [params {:body params
                :content-type "application/x-www-form-urlencoded"}]
    (-> (http/post token-url params)
      :body
      (json/parse-string true))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public Fns

(defn create-auth
  "Return a state map used to manage the authentication session. 
   Can accept either an API key for simple API access or a client
   ID, client secret and redirect URL for OAuth2. See
   https://developers.google.com/console to generate keys."
  ([api-key]
   (->SimpleAuth api-key))
  ([client-id, client-secret, redirect-url]
   (->Auth client-id client-secret redirect-url nil)))

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
  (let [{:keys [headers] :or {}} params]
    (assoc params :headers (assoc headers "Authorization" (str "Bearer " (:token auth))))))

(defmethod call-params :simple [auth params]
  (assoc params :query-params (assoc (:query-params params) "key" (:api-key auth))))

(defmethod call-params :default [auth params]
  params)

(defn is-valid?
  "Returns true if the authentication is valid, and in date."
  [auth]
  (if (:token auth)
    (< (System/currentTimeMillis) (:expires auth))
    false))

(defn generate-auth-url
  "Retrieve a URL suitable for redirecting the user to for auth permissions. 
   Scopes should be supplied as a vector of required scopes. An optional third
   param is a map with access_type and approval_prompt keys."
  ([auth scopes]
   (generate-auth-url auth scopes {:access-type "offline"
                                   :approval-prompt "auto"}))
  ([auth scopes opts]
   (let [oauth2-state (generate-state)
         params (encode-auth-url-params auth scopes oauth2-state opts)
         url (str auth-url "?" params)]
     (assoc auth :state oauth2-state :auth-url url))))

(defn exchange-token
  "Handle the user response from the oauth flow and retrieve a valid
   auth token. Returns true on success, false on failure."
  [auth code checkstate]
  (if (= (:state auth) checkstate)
    (let [params (encode-exchange-token-params auth code)
          {:keys [access_token refresh_token expires_in]} (post-token params)]
      (assoc auth :token access_token :exchange-status :success :refresh-token refresh_token :expires (expires expires_in)))
    (assoc auth :exchange-status :fail)))

(defn refresh-token
  "Generate a new authentication token using the refresh token"
  [auth]
  (if (:refresh-token auth)
    (let [params (encode-refresh-token-params auth)
          {:keys [expires_in access_token]} (post-token params)]
      (assoc auth :token access_token :expires (expires expires_in)))
    auth))
