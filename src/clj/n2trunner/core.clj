(ns n2trunner.core
  (:require [n2trunner.ui])
  (:import [Hack.Gates GatesManager]
           [Hack.HardwareSimulator HardwareSimulator]
           [Hack.HardwareSimulator HardwareSimulator HardwareSimulatorApplication]
           [SimulatorsGUI HardwareSimulatorComponent]
           [SimulatorsGUI HardwareSimulatorControllerComponent]
           [java.io File]))

(defn sendCommand [gatepath testspath]
      (let [gatemanager (GatesManager/getInstance)
            sim (n2trunner.ui.)]
           (.setWorkingDir gatemanager (File. gatepath))
           (.loadgates sim gatepath)
           (.runscripts sim testspath)))

(defn -main []
  (sendCommand "/Users/jared314/Desktop/gates" 
               "/Users/jared314/Downloads/nand2tetris/projects/00"))