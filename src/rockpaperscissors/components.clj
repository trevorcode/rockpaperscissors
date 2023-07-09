(ns rockpaperscissors.components 
  (:require [slash.component.structure :refer [action-row button]]))

(def rock-paper-scissors-component
  [(action-row
    (button :danger "Rock" :label "Rock" #_#_:emoji {:name "rock"})
    (button :success "Paper" :label "Paper" #_#_:emoji {:name "paper"})
    (button :primary "Scissors" :label "Scissors" #_#_:emoji {:name "scissors"}))])