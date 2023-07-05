(ns rockpaperscissors.core
  (:gen-class)
  (:require [clojure.core.async :refer [<! >! chan close! go go-loop]]
            [rockpaperscissors.game-logic :as rps]))

(def games-channel-map (atom {}))

(defn rps-game [game-id player1 player2 chan-chan]
  (go-loop
   [state {:game-id game-id
           :player1 {:name player1
                     :action nil}
           :player2 {:name player2
                     :action nil}
           :rounds []
           :current-round 1}]
    (let [event (<! chan-chan)
          action (:action event)]
      (cond
        (nil? event)
        (do (println "Ending game")
            (swap! games-channel-map dissoc game-id))

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
                (recur state))))))

(defn start-new-game! [player1 player2]
  (let [ch (chan)
        game-id (random-uuid)
        game-ref {:game-id game-id
                  :player1 player1
                  :player2 player2
                  :channel ch}]
    (rps-game game-id player1 player2 ch)
    (swap! games-channel-map assoc game-id game-ref)
    game-id))

(comment (start-new-game! "Tom" "Tony"))
(comment (println @games-channel-map))
(comment (go (>! (-> @games-channel-map first val :channel) :game-state)))
(comment (go (>! (-> @games-channel-map first val :channel) {:action :rock :player "Tom"})))
(comment (go (>! (-> @games-channel-map first val :channel) {:action :paper :player "Tony"})))
(comment (close! (-> @games-channel-map first val :channel)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
