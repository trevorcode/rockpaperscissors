(ns rockpaperscissors.state
  (:require [aero.core :refer [read-config]]
            [clojure.core.async :as a]
            [clojure.java.io :as io]
            [discljord.connections :as c]
            [discljord.messaging :as m]
            [mount.core :refer [defstate]]))

(def open-game-challenges (atom {}))
(def games-channel-map (atom {}))
(def ephemeral 64)

(def conf (read-config (io/resource "config.edn")))
(def token (:token conf))
(def guild-id (:guild-id conf))
(def app-id (:app-id conf))
(def intents #{:guilds :guild-messages})

(declare message-conn)
(defstate message-conn :start (m/start-connection! token))

(declare interaction-events-ch)
(defstate interaction-events-ch :start (a/chan 100 (comp (filter (comp #{:interaction-create} first)) (map second))))

(declare conn)
(defstate conn :start (c/connect-bot! token interaction-events-ch :intents intents))