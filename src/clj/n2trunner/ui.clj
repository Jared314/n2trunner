(ns n2trunner.ui
    (:import [Hack.Gates GatesManager]
             [Hack.HardwareSimulator HardwareSimulator]
             [Hack.Controller HackController HackController2]
             [java.io File])
    (:gen-class
      :extends Hack.HardwareSimulator.HardwareSimulator
      :methods [[loadgates [String] void]
                [runscripts [String] void]
                [setBuiltinNamespace [String] void]
                [setGatesDirectory [String] void]]))

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

(defn- runscript [sim path]
       (println path)
       (HackController2. sim path))

(defn -runscripts [this path] 
      (let [sim this
            files (getfiles path "tst")]
           (doall (map #(runscript sim (.getPath %))
                files))))

(defn -setBuiltinNamespace [this ns]
      (.setBuiltInDir (GatesManager/getInstance) (File. ns)))

(defn -setGatesDirectory [this path]
      (.setWorkingDir (GatesManager/getInstance) (File. path)))
