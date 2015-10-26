clj-gapi
==========

A simple client for Google web service APIs that are described by the discovery document API. This library is based on Ian Barber's library (https://github.com/ianbarber/clj-gapi), with some changes (list changes).  See https://developers.google.com/console for a full list of APIs that can be addressed by this. Ensure that a console project has been created for any access you wish to do!

Installation
-------------------------
In your `project.clj` file, just include the following dependency:
`[clj-gapi "1.0.3"]`

Quick Start
-------------------------

For very simple API access, you can make an immediate call - in this case to the Google+ API.

    (def auth (clj-gapi.auth/create-auth "YOUR_API_KEY"))
    (im auth "plus.activities/search" {"query" "clojure"})
    
Other Usage
-------------------------

To list the available APIs and version, you can query the discovery document: 

    (pprint (list-apis))

For simple API access, we need to pass in our API key to the auth class. This requires a key generated from a project in the Google developers API console at https://developers.google.com/console. Create a project and go to "API Access" on the left and look for the Simple API Access API key

  (def auth (clj-gapi.auth/create-auth "YOUR_API_KEY"))

To retrieve the calls for an API, you pass in the API string. In this case, the Google+ public data API. 

    (def service (build "https://www.googleapis.com/discovery/v1/apis/plus/v1/rest"))
  
Too see which methods are available:

    (list-methods service)
    (print (get-doc service "plus.people/listByActivity"))
    (get-scopes service "plus.people/listByActivity")
    
To call a function, we need to pass in the auth and the params. 

    (def results (call auth service "plus.activities/search"  {"query" "clojure"}))
    (map #(str (%1 :url) "-" (%1 :title)) (results :items))
    
To use OAuth2, we need to generate a ClientID and ClientSecret, and set a redirect URL. We can create a new "web application" client in the Google developers console

    (def auth (clj-gapi.auth/create-auth "YOUR_CLIENT_ID" 
      "YOUR_CLIENT_SECRET" "http://YOUR_REDIRECT_URL"))
 
We then need to generate a token URL to authenticate against. The user will be redirected back to the redirect_url, which GET parameters for code and state, which we can use to exchange a token. We'll need to define any scopes we need to authenticate. Here we'll use the scopes for creating an activity in Google+ history: 
  
    (def scopes ["https://www.googleapis.com/auth/plus.me" "https://www.googleapis.com/auth/urlshortener"])
    (clj-gapi.auth/generate-auth-url auth scopes)
    (clj-gapi.auth/exchange-token auth "CODE" "STATE")
    
To make a call, we can then use the service as before

    (def me (call auth service "plus.people/get" {"userId" "me"}))
    (pprint (me :displayName))
  
We can write as well. For example, here we can use the URL shortener API (which you'll need to enable in the developer API console!) to create a short URL to clojure.org:

    (def service (build "https://www.googleapis.com/discovery/v1/apis/urlshortener/v1/rest"))
    (def result (call auth service "urlshortener.url/insert" {} {"longUrl" "http://clojure.org"}))
    (pprint result)
    
License
-------------------------

Copyright (C) 2012 Google

Distributed under the Apache 2.0 license
