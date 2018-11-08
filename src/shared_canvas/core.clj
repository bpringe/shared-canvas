(ns shared-canvas.core
  (:gen-class)
  (:require [org.httpkit.server :refer :all]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found]]
            [clojure.data.json :as json]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [environ.core :refer [env]])) 

;;;; Configuration

(defonce port (-> (or (env :port) "8000") Integer/parseInt))

;;;; State

(defonce canvas-events (atom []))
(defonce channels (atom {}))

;;;; Handlers

(defn broadcast-message
  [sender-channel message]
  (println "Received:" message)
  (println "Sender:" sender-channel)
  (swap! canvas-events conj (json/read-str message))
  (doseq [channel (keys @channels)]
    (when-not (= sender-channel channel) 
      (send! channel message))))

(defn websocket-handler
  [request]
  (with-channel request channel
    (println "New user connected")
    (swap! channels assoc channel request)
    (println "Sending canvas events")
    (send! channel (json/write-str @canvas-events))
    (on-close channel (fn [status] (swap! channels dissoc channel)))
    (on-receive channel (fn [message] (broadcast-message channel message)))))

;;;; Middleware

(defn wrap-root-to-index
  [handler]
  (fn [request]
    (handler (update-in request [:uri]
                #(if (= "/" %) "/index.html" %)))))

;;;; Routes

(defroutes routes
  (GET "/ws" [] #'websocket-handler)
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
  (println "Server running on port 8000"))
