(ns rockpaperscissors.components 
  (:require [slash.component.structure :refer [action-row button]]))

(def rock-paper-scissors-component
  [(action-row
    (button :danger "rock" :label "Rock" #_#_:emoji {:name "rock"})
    (button :success "paper" :label "Paper" #_#_:emoji {:name "paper"})
    (button :primary "scissors" :label "Scissors" #_#_:emoji {:name "scissors"}))])