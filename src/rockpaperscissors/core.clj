(ns rockpaperscissors.core
  (:gen-class)
  (:require [clojure.core.async :as a]
            [mount.core :as mount :refer [defstate]]
            [rockpaperscissors.commands :as commands]
            [rockpaperscissors.handlers :as handlers]
            [rockpaperscissors.state :as state]))

(defn run-interaction-handlers []
  (let [exit-chan (a/chan)]
    (a/go-loop []
      (a/alt!
        exit-chan nil

        state/interaction-events-ch
        ([interaction] (do (handlers/handle-command interaction)
                           (recur)))))
    exit-chan))

(declare interaction-handlers)
(defstate interaction-handlers
  :start (run-interaction-handlers)
  :stop (a/put! interaction-handlers true))

(defn -main
  [& args]
  (mount/start)
  (commands/register-commands))