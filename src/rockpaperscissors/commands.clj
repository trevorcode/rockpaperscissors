(ns rockpaperscissors.commands
  (:require [discljord.messaging :as m]
            [rockpaperscissors.state :as state]
            [slash.command.structure :refer :all]))

(def duel-options
  [{:type 6 ; The type of the option. In this case, 6 - user. See the link to the docs above for all types.
    :name "user"
    :description "The user you would like to challenge to a duel"
    :required true}])

(defn register-commands []
  (m/create-guild-application-command! state/message-conn state/app-id state/guild-id "duel" "It's time to duel!" :options duel-options)
  (m/create-guild-application-command! state/message-conn state/app-id state/guild-id "accept" "Accept the duel"))