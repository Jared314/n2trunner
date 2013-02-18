(ns n2trunner.core
    (:require [clojure.java.io :refer [file]])
    (:import [Hack.Gates GatesManager]
             [Hack.Controller TestController]
             [Hack.Gates GateClass]
             [Hack.HardwareSimulator HardwareSimulator2]
             [java.io File]))

(defn getfiles [path extension]
      (let [f (file path)
            ext (str "." extension)]
           (if (.isDirectory f)
               (filter #(.endsWith (.getName %) ext) (file-seq f))
               (if (and (.exists f) (.endsWith (.getName f) ext))
                   [f]
                   []))))

(defn setPaths [builtinNs gatePath]
      (.setBuiltInDir (GatesManager/getInstance) (file builtinNs))
      (.setWorkingDir (GatesManager/getInstance) (file gatePath)))

(defn runscripts [path] 
      (let [files (getfiles path "tst")
            testcontroller (TestController.)
            sim (HardwareSimulator2.)]
           (dorun (filter true? 
                          (map #(.runScript testcontroller sim %) files)))))

; Loads the hdl files found in the directory into the GateClass cache
(defn loadgates [path] 
  (try
      (dorun (map #(GateClass/getGateClass (.getPath %) true) 
                  (getfiles path "hdl")))
      (catch Exception ex (println (.getMessage ex)))))

(defn runtests [gatepath testspath]
      (setPaths "builtInChips" gatepath)
      (loadgates gatepath)
      (runscripts testspath))

(defn expand-home-path [path]
      (if (.startsWith path (str "~" File/separator))
          (str (System/getProperty "user.home") (.substring path 1))
          path))

(defn -main [& args]
      (let [chipspath (expand-home-path (first args))
            testspath (expand-home-path (second args))]
           (runtests chipspath testspath)))