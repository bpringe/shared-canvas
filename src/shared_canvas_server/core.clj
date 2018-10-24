(ns shared-canvas-server.core
  (:gen-class)
  (:require [org.httpkit.server :refer [with-channel run-server on-close on-receive send!]]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found]]
            [clojure.data.json :as json]))

(defonce canvas-events (atom []))

(defn handle-message
  [message]
  (println "Received:" message)
  (swap! canvas-events conj (json/read-str message)))

(defn websocket-handler
  [request]
  (with-channel request channel
    (println "New user connected")
    (println "Sending canvas events")
    (send! channel (json/write-str @canvas-events))
    (on-close channel (fn [status] (println "channel closed:" status)))
    (on-receive channel handle-message)))

(defroutes app
  (GET "/" [] "Web server running.")
  (GET "/ws" [] websocket-handler)
  (not-found "Not found"))

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
  (start-server 8000)
  (println "Server running on port 8000"))
