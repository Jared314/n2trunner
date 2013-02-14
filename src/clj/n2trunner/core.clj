(ns n2trunner.core
    (:import [Hack.Gates GatesManager]
             [Hack.Controller HackController2]
             [Hack.Gates GateClass]
             [Hack.HardwareSimulator HardwareSimulator2]
             [java.io File]))

(defn getfiles [path extension]
      (let [f (File. path)
            ext (str "." extension)]
           (if (.isDirectory f)
               (filter #(.endsWith (.getName %) ext) (file-seq f))
               (if (and (.exists f) (.endsWith (.getName f) ext))
                   [f]
                   []))))

(defn setPaths [builtinNs gatePath]
      (let [ns (if (instance? File builtinNs) builtinNs (File. builtinNs))
            path (if (instance? File gatePath) gatePath (File. gatePath))]
           (.setBuiltInDir (GatesManager/getInstance) ns)
           (.setWorkingDir (GatesManager/getInstance) path)))

(defn runscripts [path] 
      (let [files (getfiles path "tst")]
           (dorun (filter #(.runScript %) 
                          (map #(HackController2. (HardwareSimulator2.) (.getPath %))
                               files)))))

; Loads the hdl files found in the directory into the GateClass cache
(defn loadgates [path] 
      (dorun (map #(GateClass/getGateClass (.getPath %) true) (getfiles path "hdl"))))

(defn runtests [gatepath testspath]
      (setPaths "builtInChips" gatepath)
      (loadgates gatepath)
      (runscripts testspath))

(defn -main [& args]
      (runtests "/Users/jared314/Desktop/gates" 
;                "/Users/jared314/Desktop/tests"))))
                "/Users/jared314/Downloads/nand2tetris/projects/01"))