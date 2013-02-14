(ns n2trunner.core)

(defn runtests [gatepath testspath]
      (doto (n2trunner.ui.)
            (.setBuiltinNamespace "builtInGates")
            (.setGatesDirectory gatepath)
            (.loadgates gatepath)
            (.runscripts testspath)))

(defn -main [& args]
      (runtests "/Users/jared314/Desktop/gates" 
                "/Users/jared314/Desktop/tests")
      nil)