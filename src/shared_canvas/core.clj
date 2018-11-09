(ns shared-canvas.core
  (:gen-class)
  (:require [org.httpkit.server :refer :all]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found]]
            [clojure.data.json :as json]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [environ.core :refer [env]])
  (:import java.net.InetAddress)) 

;;;; Configuration

(defonce port (-> (or (env :port) "8000") Integer/parseInt))

;;;; State

(defonce canvas-events (atom []))
(defonce channels (atom {}))

;;;; Functionality

(defn edn->json
  [edn]
  (json/write-str edn))

(defn json->edn
  [json]
  (json/read-str json))

(defn add-event-to-event-store
  [event]
  (swap! canvas-events conj (json->edn event)))

(defn broadcast-event
  [from-channel event]
  (doseq [channel (keys @channels)]
    (when-not (= from-channel channel)
      (send! channel event))))

(defn handle-message
  [from-channel message]
  (println "Received:" message)
  (println "Sender:" from-channel)
  (add-event-to-event-store message)
  (broadcast-event from-channel message))
  
;;;; Handlers

(defn websocket-handler
  [request]
  (with-channel request channel
    (println "New channel connected. Adding to channels vector.")
    (swap! channels assoc channel request)
    (println "Sending canvas events to new channel.")
    (send! channel (edn->json @canvas-events))
    (on-close channel (fn [status] (swap! channels dissoc channel)))
    (on-receive channel (fn [message] (handle-message channel message)))))

;;;; Middleware

(defn wrap-root-to-index
  [handler]
  (fn [request]
    (handler (update-in request [:uri]
                #(if (= "/" %) "/index.html" %)))))

;;;; Routes

(defroutes routes
  (GET "/ws" [] #'websocket-handler)
  (GET "/hostname" [] (.getHostName (. InetAddress getLocalHost)))
  (not-found "Not found"))

(def app
  (-> #'routes
      (wrap-resource "public")
      (wrap-content-type)
      (wrap-not-modified)
      (wrap-root-to-index)))

;;;; Server

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn start-server
  [port]
  (reset! server (run-server #'app {:port port})))

(defn -main
  [& args]
  (start-server port)
  (println "Server running on port" port))
