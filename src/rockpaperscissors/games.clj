(ns rockpaperscissors.games
  (:require [clojure.core.async :refer [>! alts! chan close! go go-loop
                                        timeout]]
            [discljord.messaging :as m]
            [rockpaperscissors.game-logic :as rps]
            [rockpaperscissors.state :as state]))


(defn rps-game [{:keys [game-id
                        player1
                        player2
                        player1-interaction-token
                        player2-interaction-token
                        discord-channel
                        async-channel]}]
  (go-loop
   [state {:game-id game-id
           :player1 {:id player1
                     :interaction-token player1-interaction-token
                     :action nil}
           :player2 {:id player2
                     :interaction-token player2-interaction-token
                     :action nil}
           :rounds []
           :current-round 1}]
    (let [[event channel] (alts! [async-channel (timeout 1000)])
          action (:action event)]
      (if event
        (cond
          (nil? event)
          (do (println "Ending game")
              (swap! state/games-channel-map dissoc game-id))

          (= event :game-state)
          (do (println state)
              (recur state))

          (or (= action :rock)
              (= action :paper)
              (= action :scissors))
          (let [new-state (rps/player-choice state event)]
            (if (and (rps/both-actions-chosen? new-state)
                     (nil? (state :winner)))
              (recur (rps/conclude-round new-state))
              (recur new-state)))

          :else (do
                  (println event)
                  (recur state)))

        (do
          (swap! state/games-channel-map dissoc game-id)
          (m/create-message! state/message-conn discord-channel :content "Timed out")
          (m/edit-original-interaction-response! state/message-conn state/app-id (-> state :player1 :interaction-token) :content "Game stopped due to time out")
          (m/edit-original-interaction-response! state/message-conn state/app-id (-> state :player2 :interaction-token) :content "Game stopped due to time out"))))))

(defn start-new-game! [{:keys [player1
                               player2
                               player1-interaction-token
                               player2-interaction-token
                               discord-channel]}]
  (let [ch (chan)
        game-id (random-uuid)
        game-ref {:game-id game-id
                  :player1 player1
                  :player2 player2
                  :channel ch}]
    (rps-game {:game-id game-id
               :player1 player1
               :player1-interaction-token player1-interaction-token
               :player2 player2
               :player2-interaction-token player2-interaction-token
               :discord-channel discord-channel
               :channel ch})
    (swap! state/games-channel-map assoc game-id game-ref)
    game-id))

(comment (start-new-game! {:player1 "Tom" :player2 "Tony"
                           :player1-interaction-token nil
                           :player2-interaction-token nil
                           :discord-channel nil}))
(comment (println @state/games-channel-map))
(comment (go (>! (-> @state/games-channel-map first val :channel) :game-state)))
(comment (go (>! (-> @state/games-channel-map first val :channel) {:action :rock :player "Tom"})))
(comment (go (>! (-> @state/games-channel-map first val :channel) {:action :paper :player "Tony"})))
(comment (close! (-> @state/games-channel-map first val :channel)))
