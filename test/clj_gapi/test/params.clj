(ns clj-gapi.test.params
  (:require [expectations :refer :all]
            [clj-gapi.params :as p]))

;; #'match-params
(expect-let [params {:calendarId {:required true}
                     :eventId {:required true}
                     :sendNotifications {:required false}
                     :launchMissiles {}}]
  ["calendarId" "eventId"]
  (p/match-params params :required))

;; #'get-path-params
(expect-let [params {:calendarId {:location "path"}
                     :eventId {:location "foo"}}]
  ["calendarId"]
  (p/get-path-params params))

;; #'get-required-params
(expect-let [params {:calendarId {:required true}
                     :sendNotifications {:required false}}]
  ["calendarId"]
  (p/get-required-params params))

;; #'hasreqs?
(expect-let [params {:calendarId {:required true}
                     :eventId {:required false}
                     :sendNotification {:require true}}]
  true
  (p/hasreqs? params {"calendarId" 1 "sendNotification" true}))


(run-tests ['clj-gapi.test.params])
