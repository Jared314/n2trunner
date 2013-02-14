(ns n2trunner.ui
    (:import [Hack.HardwareSimulator HardwareSimulator]
             [Hack.Controller HackController]
             [java.io File])
    (:gen-class
      :extends Hack.HardwareSimulator.HardwareSimulator
      :methods [[loadgates [String] void]
                [runscripts [String] void]]))

(defn- getfiles [path extension]
       (let [f (File. path)
             ext (str "." extension)]
            (if (.isDirectory f)
                (filter #(.endsWith (.getName %) ext) (file-seq f))
                (if (and (.exists f) (.endsWith (.getName f) ext))
                    [f]
                    []))))
  
(defn -loadgates [this path] 
      (map #(.loadGate this (.getName %) true) (getfiles path "hdl")))

(defn -runscript [this path]
      (HackController. this path))

(defn -runscripts [this path] 
      (let [controller (HackController. this nil)]
           (map #(.runscript this %) (getfiles path "tst"))))
