(ns rockpaperscissors.state
  (:require [clojure.core.async :as a]
            [discljord.connections :as c]
            [discljord.messaging :as m]
            [mount.core :refer [defstate]]
            [rockpaperscissors.config :as config]))

(def open-game-challenges (atom {}))
(def games-channel-map (atom {}))
(def ephemeral 64)

(def token config/token)
(def guild-id config/guild-id)
(def app-id config/app-id)
(def intents #{:guilds :guild-messages})

(declare message-conn)
(defstate message-conn :start (m/start-connection! token))

(declare interaction-events)
(defstate interaction-events :start (a/chan 100 (comp (filter (comp #{:interaction-create} first)) (map second))))

(declare conn)
(defstate conn :start (c/connect-bot! token interaction-events :intents intents))