(ns rockpaperscissors.commands
  (:require [slash.command.structure :refer :all]))

(def input-option (option "input" "Your input" :string :required true))

(def echo-command
  (command
   "echo"
   "Echoes your input"
   :options
   [input-option]))