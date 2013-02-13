(defproject n2trunner "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"local" ~(str (.toURI (java.io.File. "repository")))}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [nand2tetris/Hack "0.1.0-SNAPSHOT"]
                 [nand2tetris/HackGUI "0.1.0-SNAPSHOT"]
                 [nand2tetris/Simulators "0.1.0-SNAPSHOT"]
                 [nand2tetris/SimulatorsGUI "0.1.0-SNAPSHOT"]
                 [nand2tetris/Compilers "0.1.0-SNAPSHOT"]]
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :main n2trunner.core
  :aot [n2trunner.ui builtInChips])
