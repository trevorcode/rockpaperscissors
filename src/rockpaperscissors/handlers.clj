(ns rockpaperscissors.handlers
  (:require [clojure.core.async :refer [>! alts! chan close! go timeout]]
            [clojure.tools.logging :as log]
            [discljord.formatting :as fmt]
            [discljord.messaging :as m]
            [rockpaperscissors.components :as components]
            [rockpaperscissors.games :as games]
            [rockpaperscissors.state :as state :refer [ephemeral]]))

(defn resolve-it [{{event-type :name} :data}]
  (log/info event-type)
  event-type)

(defmulti handle-command #'resolve-it)

(defmethod handle-command :default
  [event-data]
  (log/info event-data))

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
      (go
        (log/info invite-channel)
        (>! invite-channel token)
        (swap! state/open-game-challenges dissoc user-id)
        (m/create-interaction-response! state/message-conn id token 4
                                        :data {:flags ephemeral :content "Accepted Challenge" :components components/rock-paper-scissors-component}))
      (m/create-interaction-response! state/message-conn id token 4 :data {:flags ephemeral :content "You have not been challenged by anyone."}))))