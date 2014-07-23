(ns jogl.core
  (:gen-class)
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
        (let [id (.getId (Thread/currentThread))
              g2 (.getGL2 (.getGL drawable))]
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

(defn -main
  [& args]
  (let [profile (GLProfile/getDefault)
        capabilities (GLCapabilities. profile)
        window (GLWindow/create capabilities)]
    (doto window
      (.setSize 640 480)
      (.setVisible true)
      (.setTitle "Starfleet")
      (.addWindowListener (proxy [WindowAdapter] []
                            (windowDestroyNotify [e]
                              (System/exit 0))))
      (.addGLEventListener (gl-event-listener)))
    
    (doto (FPSAnimator. window 60)
      (.start))

    window))
