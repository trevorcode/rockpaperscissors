(ns rockpaperscissors.handlers
  (:require [clojure.core.async :refer [>! alts! chan close! go timeout]]
            [clojure.tools.logging :as log]
            [discljord.formatting :as fmt]
            [discljord.messaging :as m]
            [rockpaperscissors.components :as components]
            [rockpaperscissors.games :as games]
            [rockpaperscissors.state :as state :refer [ephemeral]]))

(defn resolve-it [{{event-type :name
                    custom-id :custom-id
                    component-type :component-type} :data}]
  (log/info event-type)
  (if (= 2 component-type)
    custom-id
    event-type))

(defmulti handle-command #'resolve-it)

(defn handle-rpc-action [action {:keys [id token channel-id] {{user-id :id} :user} :member :as test}]
  (let [game (games/find-game-by-player-id @state/games-channel-map user-id)]
    (when game
      (go (>! (-> game :channel) {:action (keyword action)
                                  :player user-id
                                  :interaction-token token
                                  :interaction-id id})
          (m/create-interaction-response!
           state/message-conn
           id
           token
           7
           :data {:flags ephemeral :content (str "You chose " action) :components nil})
          #_(m/create-message! state/message-conn channel-id :content (str (fmt/mention-user user-id) " has chosen " action))))))

(defmethod handle-command "rock"
  [event]
  (handle-rpc-action "rock" event))

(defmethod handle-command "paper"
  [event]
  (handle-rpc-action "paper" event))

(defmethod handle-command "scissors"
  [event]
  (handle-rpc-action "scissors" event))

(defmethod handle-command :default
  [event-data]
  (log/info event-data))

(defmethod handle-command "test"
  [{:keys [id token channel-id] {{user-id :id} :user} :member {[{target-id :value}] :options} :data :as test}]
  (m/create-interaction-response! state/message-conn id token 4
                                  :data {:flags ephemeral :content "Awaiting challenge"})
  (m/create-followup-message! state/message-conn state/app-id token
                              :content "Awaiting challenge x2" :flags ephemeral))

(defn challenge-player [player1 player2 token channel-id]
  (let [invite-channel (chan)]
    (go
      (swap! state/open-game-challenges assoc player2 {:player1-id player1
                                                       :player1-ui token
                                                       :player2-id player2
                                                       :invite-channel invite-channel})
      (let [[player2-interaction-token channel] (alts! [invite-channel (timeout 90000)])]
        (swap! state/open-game-challenges dissoc player2)
        (if player2-interaction-token
          (do
            (log/info "ACCEPTED")
            (m/edit-original-interaction-response! state/message-conn state/app-id token :content "Play!" :components components/rock-paper-scissors-component)
            (games/start-new-game!
             {:player1 player1
              :player2 player2
              :player1-interaction-token token
              :player2-interaction-token player2-interaction-token
              :discord-channel channel-id}))

          (do
            (log/info "TIMED OUT")
            (m/edit-original-interaction-response! state/message-conn state/app-id token :content "Player has timed out on accepting the duel")
            (close! invite-channel)))))))

(defmethod handle-command "duel"
  [{:keys [id token channel-id] {{user-id :id} :user} :member {[{target-id :value}] :options} :data :as test}]
  (challenge-player user-id target-id token channel-id)
  (m/create-message! state/message-conn channel-id :content (str (fmt/mention-user user-id) " has challenged you, " (fmt/mention-user target-id) " to a duel! Type /accept to accept :smile:"))
  (m/create-interaction-response! state/message-conn id token 4
                                  :data {:flags ephemeral :content "Awaiting challenge"}))

(defmethod handle-command "accept"
  [{:keys [id token channel-id] {{user-id :id} :user} :member :as test}]
  (let [open-challenges @state/open-game-challenges
        challenge-for-user (get open-challenges user-id)
        invite-channel (:invite-channel challenge-for-user)]
    (if challenge-for-user
      (do
        (m/create-interaction-response! state/message-conn id token 4
                                        :data {:flags ephemeral
                                               :content "Accepted Challenge!"
                                               :components components/rock-paper-scissors-component})
        (go (>! invite-channel token)))

      (m/create-interaction-response! state/message-conn id token 4 :data {:flags ephemeral :content "You have not been challenged by anyone."}))))
