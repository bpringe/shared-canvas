(ns shared-canvas-server.core
  (:gen-class)
  (:require [org.httpkit.server :refer [with-channel run-server on-close on-receive send!]]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found]]))

(defn message-handler
  [request]
  (with-channel request channel
    (on-close channel (fn [status] (println "channel closed:" status)))
    (on-receive channel (fn [data] (do (println "Received:" data)
                                       (send! channel data))))))

(defroutes app
  (GET "/" [] "Web server running.")
  (GET "/ws" [] message-handler)
  (not-found "Not found"))

(defn -main
  [& args]
  (run-server app {:port 8000})
  (println "Server running on port 8080"))
