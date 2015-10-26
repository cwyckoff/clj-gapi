(ns clj-gapi.test.resource
  (:require [expectations :refer :all]
            [clj-gapi.resource :as r]))

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

;; #'docstring
(expect
  "Deletes an event.\nRequired parameters: calendarId eventId\n"
  (r/docstring method))

;; #'get-method-name
(expect
  "calendar.events/delete"
  (r/get-method-name (:id method)))

;; #'arglists
(expect
  '[[auth parameters]]
  (r/arglists method))

(expect-let [method (assoc method :httpMethod "POST")]
  '[[auth parameters body]]
  (r/arglists method))

(defn check-resource [resource]
  (let [method-map (resource "calendar.events/delete")]
    (expect map? resource)
    (expect "calendar.events/delete" (-> resource first key))
    (expect map? method-map)
    (expect '(:fn :doc :arglists :scopes) (keys (resource "calendar.events/delete")))
    (expect fn? (:fn method-map))))

;; #'extract-methods
(let [base-url "https://www.googleapis.com/calendar/v3/"
      resource (r/extract-methods base-url {:methods {:delete method}})]
  (check-resource resource))

;; #'build
(let [base-url "https://www.googleapis.com/calendar/v3/"
      r {:resources {:acl {:methods {:delete method}}}}
      resource (r/build base-url r)]
  (check-resource resource))

(run-tests ['clj-gapi.test.resource])
