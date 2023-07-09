(ns rockpaperscissors.core
  (:gen-class)
  (:require [clojure.core.async :as a]
            [rockpaperscissors.commands :as commands]
            [rockpaperscissors.handlers :as handlers]
            [rockpaperscissors.state :as state]
            [mount.core :as mount]))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(mount/start)
(commands/register-commands)

(a/go-loop []
  (when-let [interaction (a/<! state/interaction-events)]
    (handlers/handle-command interaction)
    (recur)))

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