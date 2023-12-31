(ns rockpaperscissors.game-logic 
  (:require [clojure.tools.logging :as log]))

(def losing-move
  "Map associating a move to a move that beats it"
  {:rock     :paper
   :paper    :scissors
   :scissors :rock})

(defn who-wins? [move-1 move-2]
  (cond
    (= move-1 move-2) :tie
    (= move-1 (losing-move move-2)) :player1
    (= move-2 (losing-move move-1)) :player2))

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
                   :winner (who-wins? player1-action player2-action)}]
    (-> state
        (assoc-in [:player1 :action] nil)
        (assoc-in [:player2 :action] nil)
        (update :current-round inc)
        (update :rounds conj old-round)
        (as-> $ (assoc $ :winner (check-for-match-winner (:rounds $) 3))))))

(defn get-player-by-action [state player]
  (log/info (str "comparing " player "to " (get-in state [:player1 :id])))
  (if (= player (-> state :player1 :id))
    :player1
    :player2))

(defn player-choice [state {action :action
                            player :player
                            interaction-token :interaction-token
                            :as event}]
  (let [player (get-player-by-action state player)]
    (-> state
        (assoc-in [player :action] action)
        #_(assoc-in [player :interaction-token] interaction-token))))