(ns rockpaperscissors.games
  (:require [clojure.core.async :refer [>! alts! chan close! go go-loop
                                        timeout]]
            [clojure.tools.logging :as log]
            [discljord.formatting :as fmt]
            [discljord.messaging :as m]
            [rockpaperscissors.components :as components]
            [rockpaperscissors.game-logic :as rps :refer [get-player-by-action]]
            [rockpaperscissors.state :as state :refer [ephemeral]]))

(defn find-game-by-player-id [games player-id]
  (->> games
       vals
       (filter #(or (= player-id (:player1 %))
                    (= player-id (:player2 %))))
       first))

(defn print-round-conclusion [state discord-channel]
  (let [prev-round (last (:rounds state))
        player1-wins (->> (:rounds state)
                          (filter #(= :player1 (:winner %)))
                          (count))
        player2-wins (->> (:rounds state)
                          (filter #(= :player2 (:winner %)))
                          (count))
        round-result-string (str
                             (fmt/mention-user (get-in state [:player1 :id])) " chose " (:player1-action prev-round)
                             " and " (fmt/mention-user (get-in state [:player2 :id])) " chose " (:player2-action prev-round)
                             " Player 1 wins: " player1-wins
                             " vs Player 2 wins: " player2-wins)]
    (m/create-message!
     state/message-conn discord-channel
     :content round-result-string)))

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
    (let [[event channel] (alts! [async-channel (timeout 50000)])
          action (:action event)]
      (log/info event)
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
          (let [new-state (rps/player-choice state event)
                player (get-player-by-action state event)]
            (m/delete-original-interaction-response!
             state/message-conn
             state/app-id
             (-> state player :interaction-token))

            (if (rps/both-actions-chosen? new-state)
              (let [round-conclusion (rps/conclude-round new-state)]
                (print-round-conclusion round-conclusion discord-channel)
                (m/create-followup-message! state/message-conn state/app-id (get-in state [:player1 :interaction-token])
                                            :flags ephemeral
                                            :components components/rock-paper-scissors-component)
                (m/create-followup-message! state/message-conn state/app-id (get-in state [:player2 :interaction-token])
                                            :flags ephemeral
                                            :components components/rock-paper-scissors-component)
                (recur round-conclusion))
              (recur new-state)))

          :else (do
                  (println event)
                  (recur state)))

        (do
          (swap! state/games-channel-map dissoc game-id)
          (m/create-message! state/message-conn discord-channel :content "Timed out")
          (m/edit-original-interaction-response! state/message-conn state/app-id (-> state :player1 :interaction-token) :content "Game stopped due to time out" :components nil)
          (m/edit-original-interaction-response! state/message-conn state/app-id (-> state :player2 :interaction-token) :content "Game stopped due to time out" :components nil))))))

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
               :async-channel ch})
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

(def temp {:rounds [{:round 1, :player1-action :paper, :player2-action :paper, :winner :tie}]})
