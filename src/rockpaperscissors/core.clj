(ns rockpaperscissors.core
  (:gen-class)
  (:require [clojure.core.async :as a]
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

(def token config/token)
(def guild-id config/guild-id)
(def app-id config/app-id)
(def intents #{:guilds :guild-messages})

(def api (m/start-connection! token))
(def interaction-events (a/chan 100 (comp (filter (comp #{:interaction-create} first)) (map second))))
(def conn (c/connect-bot! token interaction-events :intents intents))

(def greet-options
  [{:type 6 ; The type of the option. In this case, 6 - user. See the link to the docs above for all types.
    :name "user"
    :description "The user to greet"
    :required true}])

; Params: guild id (omit for global commands), command name, command description, optionally command options
@(m/create-guild-application-command! api app-id guild-id "hello-there" "Say hi to someone" :options greet-options)

(def duel-options
  [{:type 6 ; The type of the option. In this case, 6 - user. See the link to the docs above for all types.
    :name "user1"
    :description "The user to greet"
    :required true}
   {:type 6 ; The type of the option. In this case, 6 - user. See the link to the docs above for all types.
    :name "user2"
    :description "The user to greet"
    :required true}])

@(m/create-guild-application-command! api app-id guild-id "duel" "It's time to duel!" :options duel-options)

(defn resolve-it [{{event-type :name} :data}]
  (log/info event-type)
  event-type)

(defmulti handle-command #'resolve-it)

(defmethod handle-command :default
  [event-data]
  (log/info event-data))

(defmethod handle-command "hello-there"
  [{:keys [id token] {{user-id :id} :user} :member {[{target :value}] :options} :data :as test}]
  (log/info "Hello there!")
  (m/create-interaction-response! api id token 4 :data {:content (str "Hello, " (fmt/mention-user (or target user-id)) " :smile:")}))

(defmethod handle-command "duel"
  [{:keys [id token] {{user-id :id} :user} :member {[{target :value} {target2 :value}] :options} :data :as test}]
  (m/create-interaction-response! api id token 4 :data {:content (str "Duel, " (fmt/mention-user target) " and " (fmt/mention-user target2) " :smile:")}))

(a/go-loop []
  (when-let [interaction (a/<! interaction-events)]
    (handle-command  interaction)
    (recur)))


;; (def state (atom nil))

;; (def my-components
;;   [(action-row
;;     (button :danger "unsubscribe" :label "Turn notifications off")
;;     (button :success "subcribe" :label "Turn notifications on"))
;;    (action-row
;;     (select-menu
;;      "language"
;;      [(select-option "English" "EN" :emoji {:name "🇬🇧"})
;;       (select-option "French" "FR" :emoji {:name "🇫🇷"})
;;       (select-option "Spanish" "ES" :emoji {:name "🇪🇸"})]
;;      :placeholder "Language"))])

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