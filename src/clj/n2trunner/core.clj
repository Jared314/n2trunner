(ns n2trunner.core
    (:import [Hack.Gates GatesManager]
             [Hack.Controller TestController]
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
      (let [files (getfiles path "tst")
            testcontroller (TestController.)]
           (dorun (filter true? 
                          (map #(.runScript testcontroller (HardwareSimulator2.) (.getPath %)) 
                               files)))))

; Loads the hdl files found in the directory into the GateClass cache
(defn loadgates [path] 
      (dorun (map #(GateClass/getGateClass (.getPath %) true) 
                  (getfiles path "hdl"))))

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