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

(defn determine-winner [{{p1Action :action} :player1
                         {p2Action :action} :player2}]
  (cond
    (= p1Action p2Action)
    :tie

    (or (and (= p1Action :rock)
             (= p2Action :paper))
        (and (= p1Action :paper)
             (= p2Action :rock))
        (and (= p1Action :scissors)
             (= p2Action :paper)))
    :player1

    :else :player2))

(defn calc-winner? [state]
  (and (some? (-> state :player1 :action))
       (some? (-> state :player2 :action))))

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
          (if (calc-winner? new-state)
            (recur new-state)
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

(comment (start-new-game! "Tim" "Tammy"))
(comment (println @games-channel-map))
(comment (go (>! (-> @games-channel-map first val :channel) :game-state)))
(comment (go (>! (-> @games-channel-map first val :channel) {:action :scissors :player "Tim"})))
(comment (close! (-> @games-channel-map first val :channel)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
