(ns rockpaperscissors.handlers
  (:require [clojure.string :as str]
            [slash.command :as cmd]
            [slash.response :as rsp :refer [channel-message ephemeral]]))

(cmd/defhandler reverse-handler
  ["reverse"]
  _
  [input words]
  (channel-message
   {:content (if words
               (->> #"\s+" (str/split input) reverse (str/join " "))
               (str/reverse input))}))

(cmd/defhandler mock-handler
  ["mock"]
  _
  [input]
  (channel-message
   {:content (->> input
                  (str/lower-case)
                  (map #(cond-> % (rand-nth [true false]) Character/toUpperCase))
                  str/join)}))

(cmd/defhandler unknown-handler
  [unknown] ; Placeholders can be used in paths too
  {{{user-id :id} :user} :member} ; Using the interaction binding to get the user who ran the command
  _ ; no options
  (-> (channel-message {:content (str "I don't know the command `" unknown "`, <@" user-id ">.")})
      ephemeral))

(cmd/defpaths command-paths
  (cmd/group ["fun"] ; common prefix for all following commands
             reverse-handler
             mock-handler
             unknown-handler))


