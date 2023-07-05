(ns rockpaperscissors.game-logic-test
  (:require [clojure.test :refer :all]
            [rockpaperscissors.game-logic :refer :all]))

(def test-state {:game-id #uuid "c5baa417-e419-43bf-ae62-60dc8af85f5d"
               :player1 {:name "Joe"
                         :action :scissors}
               :player2 {:name "Jen"
                         :action :scissors}
               :rounds []
               :current-round 1})

(deftest determine-round-winner-test
  (testing "FIXME, I fail."
    (is (= 0 1))))
