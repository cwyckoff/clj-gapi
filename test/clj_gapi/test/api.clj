(ns clj-gapi.test.api
  (:require [cheshire.core :as json]
            [expectations :refer :all]

            [clj-gapi.api :as a]))

(def method {:id "calendar.events.delete"
             :path "calendars/{calendarId}/events/{eventId}"
             :httpMethod "DELETE"
             :description "Deletes an event."
             :parameters {:calendarId {:type "string"
                                       :description "Calendar identifier. To retrieve calendar IDs call the calendarList.list method. If you want to access the primary calendar of the currently logged in user, use the \"primary\" keyword."
                                       :required true
                                       :location "path"}
                          :eventId {:type "string"
                                    :description "Event identifier."
                                    :required true
                                    :location "path"}
                          :sendNotifications {:type "boolean"
                                              :description "Whether to send notifications about the deletion of the event. Optional. The default is False."
                                              :location "query"}}
             :parameterOrder ["calendarId" "eventId"]
             :scopes ["https://www.googleapis.com/auth/calendar"]})

;; #'get-response
(expect-let [valid-response {:status 200
                             :body (json/generate-string {:foo "bar"})}]
  {:foo "bar"}
  (a/get-response valid-response))

(expect-let [invalid-response {:status 400
                               :body (json/generate-string {:error {:message "oops"}})}]
  {:error "oops"}
  (a/get-response invalid-response))

;; #'get-url
(expect-let [base-url "http://foo.com"
             path "/calendars/{calendarId}/events/{eventId}"
             params ["calendarId" "eventId"]
             args {"calendarId" "primary", "eventId" "per79kb798h2jjvpd7juqudmas"}]
  "http://foo.com/calendars/primary/events/per79kb798h2jjvpd7juqudmas"
  (a/get-url base-url path params args))


;; #'callfn "GET"
;; #'callfn "POST"
;; #'callfn "DELETE"
;; #'callfn "PUT"
;; #'callfn "PATCH"

(run-tests ['clj-gapi.test.api])
