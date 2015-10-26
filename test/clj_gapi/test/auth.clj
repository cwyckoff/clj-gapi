(ns clj-gapi.test.auth
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]

            [cheshire.core :as json]
            [expectations :refer :all]
            
            [clj-gapi.auth :as a])
  (:use clj-http.fake))

(def config
  {:secret "foo"
   :client-id "bar"
   :scope ["https://www.googleapis.com/auth/calendar"]
   :service-api "https://www.googleapis.com/discovery/v1/apis/calendar/v3/rest"
   :callback-url "http://foo.com/bar"})

(defn generate-oauth [& [opts]]
  (let [{:keys [secret client-id scope service-api callback-url]} config
        auth (a/create-auth client-id secret callback-url)]
    (if opts
      (apply assoc auth (-> opts vec flatten))
      auth)))

;; #'create-auth: simple
(expect clj_gapi.auth.SimpleAuth (a/create-auth "foobar"))

;; #'create-auth: auth
(expect-let [{:keys [secret client-id scope service-api callback-url]} config]
  clj_gapi.auth.Auth
  (a/create-auth client-id secret callback-url))

;; #'call-params: oauth
(expect-let [auth (generate-oauth {:token "foo"})
      params {"calendarId" 1}
      expected {:headers {"Authorization" "Bearer foo"} "calendarId" 1}]
  expected
  (a/call-params auth params))

;; #'call-params: simple auth
(expect-let [auth (a/create-auth "foobar")
      params {"calendarId" 1}
      expected {:query-params {"key" "foobar"}, "calendarId" 1}]
  expected
  (a/call-params auth params))

;; #'call-params: default
(expect-let [params {"calendarId" 1}]
  params
  (a/call-params nil params))

;; #'is-valid?
(let [tomorrow (-> 1 t/days t/from-now tc/to-long)
      yesterday (-> 1 t/days t/ago tc/to-long)
      valid-auth (generate-oauth {:token "foo" :expires tomorrow})
      invalid-auth (generate-oauth {:token "foo" :expires yesterday})]
  (expect true (a/is-valid? valid-auth))
  (expect false (a/is-valid? invalid-auth))
  (expect false (a/is-valid? (dissoc valid-auth :token))))

;; #'generate-auth-url
(let [auth (generate-oauth)
      {:keys [client-id callback-url scope secret]} config
      auth (a/generate-auth-url auth ["foo/bar/boo"])
      auth-url (:auth-url auth)]
  (expect true (contains? auth :state))
  (expect true (contains? auth :auth-url))
  (expect #"https://accounts.google.com/o/oauth2/auth?client_id=bar&redirect_uri=http%3A%2F%2Ffoo.com%2Fbar&scope=foo%2Fbar%2Fboo&state=.*&response_type=code&access_type=offline&approval_prompt=auto"))

;; #'exchange-token
(let [checkstate "abc123"
      auth {:client-id "foo"
            :redirect-url "http://foo.com/bar"
            :client-secret "blarg"
            :state checkstate}
      code "def456"
      body (json/generate-string {:access_token "access-token"
                                  :refresh_token "refresh-token"
                                  :expires_in 123456789})]

  ;; checkstate matches auth state
  (expect
    :success
    (with-fake-routes {a/token-url (fn [req] {:status 200 :headers {} :body body})}
      (:exchange-status (a/exchange-token auth code checkstate))))

  (expect
    true
    (with-fake-routes {a/token-url (fn [req] {:status 200 :headers {} :body body})}
      (-> (a/exchange-token auth code checkstate)
        (every? [:token :exchange-status :refresh :expires]))))
  
  ;; checkstate DOES NOT match auth state
  (expect-let [checkstate "def456"]
    :fail
    (with-fake-routes {a/token-url (fn [req] {:status 200 :headers {} :body body})}
      (:exchange-status (a/exchange-token auth code checkstate)))))


;; #'refresh-token
(let [auth {:client-id "foo"
            :client-secret "blarg"
            :token "old-access-token"
            :refresh "abc123"}
      body (json/generate-string {:access_token "new-access-token"
                                  :expires_in 123456789})]

  ;; refresh token exists
  (expect
    "new-access-token"
    (with-fake-routes {a/token-url (fn [req] {:status 200 :headers {} :body body})}
      (:token (a/refresh-token auth))))
  
  ;; refresh token DOES NOT exist
  (expect-let [auth (dissoc auth :refresh)]
    "old-access-token"
    (with-fake-routes {a/token-url (fn [req] {:status 200 :headers {} :body body})}
      (:token (a/refresh-token auth)))))

(run-tests ['clj-gapi.test.auth])
