(ns jogl.core
  (:gen-class)
  (:require [com.stuartsierra.component :as c]
            [clojure.reflect :as r]
            [clojure.string :as str])
  (:import (javax.media.opengl GL GLBase GL2ES2 GLAutoDrawable GLCapabilities)
           (javax.media.opengl GLEventListener GLProfile)
           (javax.media.opengl.fixedfunc GLMatrixFunc)
           (javax.media.opengl.awt GLCanvas)
           (com.jogamp.newt.event WindowAdapter WindowEvent)
           (com.jogamp.newt.opengl GLWindow)
           (com.jogamp.opengl.util FPSAnimator)
           (java.lang.reflect Method Parameter)))

(def ^:dynamic *gl* nil)

(defmacro generate-opengl-api
  [cls]
  `(do ~@(for [{:keys [name flags parameter-types]
                :as member} (:members (r/reflect (resolve cls)))
                :let [method? (instance? clojure.reflect.Method member)
                      arg-range (range (count parameter-types))
                      args (map #(symbol (str "arg" %)) arg-range)]
                :when (and method? (:public flags) (not (:static flags)))]
           `(defn ~name
              ~(into [] args)
              (. *gl* ~(cons name args))))))

(generate-opengl-api GL)
(generate-opengl-api GLBase)
(generate-opengl-api GL2ES2)

(defn gl-event-listener
  []
  (let [x (atom (int (* (Math/random) 640)))
        y (atom (int (* (Math/random) 480)))]
    (proxy [GLEventListener] []
      (display [drawable]
        (binding [*gl* (.getGL2ES2 (.getGL drawable))]
          (swap! x #(mod (inc %) 640))
          (swap! y #(mod (inc %) 640))
          (glClearColor 0.0 0.0 1.0 1.0)
          (glClear GL/GL_COLOR_BUFFER_BIT)))
      (init [arg0])
      (dispose [arg0])
      (reshape [arg0 arg1 arg2 arg3 arg4]))))

(defrecord Context [capabilities profile]
  c/Lifecycle
  (start [ctx]
    (if-not capabilities
      (let [profile (or profile (GLProfile/getGL2ES2))]
        (assoc ctx
          :profile profile
          :capabilities (GLCapabilities. profile)))
      ctx))
  (stop [ctx]
    (dissoc ctx :profile :capabilities)))

(defrecord Window [window setup-window context]
  c/Lifecycle
  (start [this]
    (if-not window
      (assoc this
        :window (setup-window (GLWindow/create (:capabilities context))))
      this))
  (stop [this]
    (dissoc this :window)))

(defn gl-system
  [options]
  (c/system-map
   :context (map->Context {})
   :window (c/using (map->Window options) [:context])))

(defn load-shader
  [gl src type]
  (doto gl
    (.glCreateShader )))

(defn -main
  [& args]
  (letfn [(setup-window [window]
            (doto window
              (.setSize 640 480)
              (.setVisible true)
              (.setTitle "JOGL")
              (.addWindowListener (proxy [WindowAdapter] []
                                    (windowDestroyNotify [e])))
              (.addGLEventListener (gl-event-listener)))
            (.start (FPSAnimator. window 60)))]
    (c/start-system (gl-system {:setup-window setup-window}))))
