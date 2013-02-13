(ns n2trunner.ui
  (:import [Hack.HardwareSimulator HardwareSimulator])
  (:gen-class
    :extends Hack.HardwareSimulator.HardwareSimulator
    :methods [[loadpath [String] void]]))

(defn -loadpath [this path] 
  (.loadGate this path true))