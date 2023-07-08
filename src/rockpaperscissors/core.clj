(ns rockpaperscissors.core
  (:gen-class)
  (:require [clojure.core.async :as a :refer [>! alts! chan close! go timeout]]
            [clojure.tools.logging :as log]
            [discljord.connections :as c]
            [discljord.formatting :as fmt]
            [discljord.messaging :as m]
            [rockpaperscissors.config :as config]
            [slash.component.structure :refer :all]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(def ^:private ephemeral 64)

(def token config/token)
(def guild-id config/guild-id)
(def app-id config/app-id)
(def intents #{:guilds :guild-messages})

(def api (m/start-connection! token))
(def interaction-events (a/chan 100 (comp (filter (comp #{:interaction-create} first)) (map second))))
(def conn (c/connect-bot! token interaction-events :intents intents))

(def duel-options
  [{:type 6 ; The type of the option. In this case, 6 - user. See the link to the docs above for all types.
    :name "user"
    :description "The user you would like to challenge to a duel"
    :required true}])

@(m/create-guild-application-command! api app-id guild-id "duel" "It's time to duel!" :options duel-options)
@(m/create-guild-application-command! api app-id guild-id "accept" "Accept the duel")

(def open-game-challenges (atom {}))

(def rock-paper-scissors-component
  [(action-row
    (button :danger "Rock" :label "Rock" #_#_:emoji {:name "rock"})
    (button :success "Paper" :label "Paper" #_#_:emoji {:name "paper"})
    (button :primary "Scissors" :label "Scissors" #_#_:emoji {:name "scissors"}))])

(defn resolve-it [{{event-type :name} :data}]
  (log/info event-type)
  event-type)

(defmulti handle-command #'resolve-it)

(defmethod handle-command :default
  [event-data]
  (log/info event-data))

(defn challenge-player [player1 player2 token]
  (let [invite-channel (chan)]
    (go
      (swap! open-game-challenges assoc player2 {:player1-id player1
                                                 :player1-ui token
                                                 :player2-id player2
                                                 :invite-channel invite-channel})
      (let [[accepted channel] (alts! [invite-channel (timeout 90000)])]
        (if accepted
          (do
            (log/info "ACCEPTED")
            (m/edit-original-interaction-response! api app-id token :content "Play!" :components rock-paper-scissors-component))
          (do (log/info "TIMED OUT")
              (m/edit-original-interaction-response! api app-id token :content "Player has timed out on accepting the duel")
              (close! invite-channel)
              (swap! open-game-challenges dissoc player2)))))))

(defmethod handle-command "duel"
  [{:keys [id token channel-id] {{user-id :id} :user} :member {[{target-id :value}] :options} :data :as test}]
  (println token)
  (challenge-player user-id target-id token)
  (m/create-message! api channel-id :content (str user-id " has challenged you, " (fmt/mention-user target-id) " to a duel! Type /accept to accept :smile:"))
  (m/create-interaction-response! api id token 4
                                  :data {:flags ephemeral :content "Awaiting challenge"}))

(defmethod handle-command "accept"
  [{:keys [id token channel-id] {{user-id :id} :user} :member :as test}]
  (let [open-challenges @open-game-challenges
        challenge-for-user (get open-challenges user-id)
        invite-channel (:invite-channel challenge-for-user)]
    (if challenge-for-user
      (go
        (log/info invite-channel)
        (>! invite-channel "Accepted")
        (m/create-interaction-response! api id token 4
                                        :data {:flags ephemeral :content "Accepted Challenge" :components rock-paper-scissors-component}))
      (m/create-interaction-response! api id token 4 :data {:flags ephemeral :content "You have not been challenged by anyone."}))))

(a/go-loop []
  (when-let [interaction (a/<! interaction-events)]
    (handle-command  interaction)
    (recur)))

;; (def state (atom nil))


;; (defn greet-or-disconnect
;;   [event-type {{bot :bot} :author :keys [channel-id content] :as all}]
;;   (if (= content "!disconnect")
;;     (a/put! (:connection @state) [:disconnect])
;;     (when-not bot
;;       (log/info all)
;;       #_(m/create-message! (:messaging @state) channel-id :content "Hello, World!")
;;       (m/create-message! (:messaging @state) channel-id :content "Yo" :components my-components))))

;; (defn send-emoji
;;   [event-type {:keys [channel-id emoji] :as all}]
;;   (log/info all)
;;   (when (:name emoji)
;;     (m/create-message! (:messaging @state) channel-id
;;                        :content (if (:id emoji)
;;                                   (str "<:" (:name emoji) ":" (:id emoji) ">")
;;                                   (:name emoji)))))

;; (defn test
;;   [event-type {{bot :bot} :author :keys [channel-id content] :as all}]
;;   (log/info "Testing"))

;; (def handlers
;;   {:default [#'test]
;;    :message-create [#'greet-or-disconnect]
;;    :message-reaction-add [#'send-emoji]})

;; (go (let [event-ch (a/chan 100)
;;           connection-ch (c/connect-bot! token event-ch :intents intents)
;;           messaging-ch (m/start-connection! token)
;;           event-handler (-> slash/route-interaction
;;                             (partial (assoc gateway-defaults :application-command handlers/command-paths))
;;                             (wrap-response-return (fn [id token {:keys [type data]}]
;;                                                     (log/info data)
;;                                                     (m/create-interaction-response! messaging-ch id token type :data data))))
;;           new-handlers (assoc handlers :interaction-create [#(event-handler %2)])]
;;       (m/bulk-overwrite-guild-application-commands! connection-ch application-id guild-id [commands/echo-command])
;;       (try (e/message-pump! event-ch (partial e/dispatch-handlers new-handlers))
;;            (finally
;;              (m/stop-connection! messaging-ch)
;;              (c/disconnect-bot! connection-ch)))))