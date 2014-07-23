(ns jogl.core
  (:gen-class)
  (:require [com.stuartsierra.component :as c])
  (:import (javax.media.opengl GL GL2 GLAutoDrawable GLCapabilities)
           (javax.media.opengl GLEventListener GLProfile)
           (javax.media.opengl.fixedfunc GLMatrixFunc)
           (com.jogamp.newt.event WindowAdapter WindowEvent)
           (com.jogamp.newt.opengl GLWindow)
           (com.jogamp.opengl.util FPSAnimator)))

(defn gl-event-listener
  []
  (let [x (atom (int (* (Math/random) 640)))
        y (atom (int (* (Math/random) 480)))]
    (proxy [GLEventListener] []
      (display [drawable]
        (let [g2 (.getGL2 (.getGL drawable))]
          (swap! x (comp #(mod % 640) inc))
          (swap! y (comp #(mod % 640) inc))
          (doto g2
            (.glClearColor 0.0 0.0 0.3 1.0)
            (.glClear GL/GL_COLOR_BUFFER_BIT)
            (.glMatrixMode GLMatrixFunc/GL_PROJECTION)
            (.glLoadIdentity)
            (.glOrtho 0 640 0 480 1 100)
            (.glMatrixMode GLMatrixFunc/GL_MODELVIEW)
            (.glLoadIdentity)
            (.glTranslated 0 0 -1)
            (.glBegin GL2/GL_QUADS)
            (.glVertex2d @x (+ @y 10))
            (.glVertex2d @x @y)
            (.glVertex2d (+ @x 10) @y)
            (.glVertex2d (+ @x 10) (+ @y 10))
            (.glEnd))))
      (init [arg0])
      (dispose [arg0])
      (reshape [arg0 arg1 arg2 arg3 arg4]))))

(defrecord Context [profile capabilities]
  c/Lifecycle
  (start [ctx]
    (let [profile (or profile (GLProfile/getDefault)) ]
      (assoc ctx
        :profile profile
        :capabilities (or capabilities (GLCapabilities. profile)))))
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
    (c/start (gl-system {:setup-window setup-window}))))
