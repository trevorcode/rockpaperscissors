(ns rockpaperscissors.core
  (:gen-class)
  (:require [clojure.core.async :refer [<! >! chan close! go go-loop]]))

(def games-channel-map (atom {}))

(def le-state {:game-id #uuid "c5baa417-e419-43bf-ae62-60dc8af85f5d"
               :player1 {:name "Trevor"
                         :action :scissors}
               :player2 {:name "Olivia"
                         :action :scissors}
               :rounds []
               :current-round 1})

(defn determine-round-winner
  "Determines the round winner of the rock paper scissors"
  [p1Action p2Action]
  (cond
    (= p1Action p2Action)
    :tie

    (or (and (= p1Action :rock)
             (= p2Action :scissors))
        (and (= p1Action :paper)
             (= p2Action :rock))
        (and (= p1Action :scissors)
             (= p2Action :paper)))
    :player1

    :else :player2))

(defn both-actions-chosen?
  "Are both rock paper scissors actions chosen?"
  [state]
  (and (some? (-> state :player1 :action))
       (some? (-> state :player2 :action))))

(defn check-for-match-winner
  "Receives a list of rounds and a first-to number of wins
   and determines if there is a match winner"
  [rounds first-to]
  (let [p1-wins (count (filter #(= (:winner %) :player1) rounds))
        p2-wins (count (filter #(= (:winner %) :player2) rounds))]
    (cond
      (>= p1-wins first-to) :player1
      (>= p2-wins first-to) :player2
      :else nil)))

(defn conclude-round [state]
  (let [player1-action (-> state :player1 :action)
        player2-action (-> state :player2 :action)

        old-round {:round (state :current-round)
                   :player1-action player1-action
                   :player2-action player2-action
                   :winner (determine-round-winner player1-action player2-action)}]
    (-> state
        (assoc-in [:player1 :action] nil)
        (assoc-in [:player2 :action] nil)
        (update :current-round inc)
        (update :rounds conj old-round)
        (as-> $ (assoc $ :winner (check-for-match-winner (:rounds $) 3))))))

(defn player-action [state {action :action player-name :player}]
  (let [player (if (=  player-name (-> state :player1 :name))
                 :player1
                 :player2)]
    (print action)
    (assoc-in state [player :action] action)))

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
        (let [new-state (player-action state event)]
          (if (and (both-actions-chosen? new-state)
                   (nil? (state :winner)))
            (recur (conclude-round new-state))
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
