(defproject jogl "0.1.0-SNAPSHOT"
  :description ""
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.jogamp.gluegen/gluegen-rt-main "2.1.5-01"]
                 [org.jogamp.jogl/jogl-all-main "2.1.5-01"]
                 [org.jogamp.joal/joal-main "2.1.5-01"]
                 [org.jogamp.jocl/jocl-main "2.1.5-01"]]
  :main ^:skip-aot jogl.core
  :profiles {:uberjar {:aot :all}})
