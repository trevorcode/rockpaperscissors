(ns rockpaperscissors.game-logic)

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

(defn player-choice [state {action :action player-name :player}]
  (let [player (if (= player-name (-> state :player1 :id))
                 :player1
                 :player2)]
    (print action)
    (assoc-in state [player :action] action)))