(ns n2trunner.core
  (:require [n2trunner.ui])
  (:import [Hack.Gates GatesManager]
           [Hack.HardwareSimulator HardwareSimulator]
           [Hack.HardwareSimulator HardwareSimulator HardwareSimulatorApplication]
           [SimulatorsGUI HardwareSimulatorComponent]
           [SimulatorsGUI HardwareSimulatorControllerComponent]
           [java.io File]))

(defn getchips [path extension]
  (let [f (File. path)]
        (filter #(.endsWith (.getName %) (str "." extension)) (file-seq f))))

(defn sendCommand [gatepath testspath]
      (let [gatemanager (GatesManager/getInstance)
            sim (n2trunner.ui.)]
           (.setWorkingDir gatemanager (File. gatepath))
           (for [item (getchips gatepath "hdl")]
                (.loadpath sim (.getPath item)))))

; (defn gui []
;   (let [simcomponent (HardwareSimulatorComponent.)
;         controllerComponent (HardwareSimulatorControllerComponent.)]
;     (HardwareSimulatorApplication. controllerComponent simcomponent "scripts/defaultHW.txt" "help/hwUsage.html" "help/hwAbout.html")))


(defn -main []
  (sendCommand "/Users/jared314/Desktop/gates" 
               "/Users/jared314/Downloads/nand2tetris/projects/00"))