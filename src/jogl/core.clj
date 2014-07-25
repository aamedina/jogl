(ns jogl.core
  (:gen-class)
  (:require [clojure.core.async :as a :refer [go <! >! go-loop put! chan]]
            [com.stuartsierra.component :as c]
            [clojure.reflect :as r]
            [clojure.string :as str])
  (:import (javax.media.opengl GL GLBase GL2ES2 GLAutoDrawable GLCapabilities)
           (javax.media.opengl GLEventListener GLProfile DebugGLES2)
           (javax.media.opengl.fixedfunc GLMatrixFunc)
           (javax.media.opengl.awt GLCanvas)
           (com.jogamp.newt.event WindowAdapter WindowEvent)
           (com.jogamp.newt.opengl GLWindow)
           (com.jogamp.opengl.util FPSAnimator)
           (java.lang.reflect Method Parameter)
           (java.nio IntBuffer)))

(def ^:dynamic *gl* nil)

(defn fn-method
  [{:keys [name flags parameter-types] :as member}]
  (let [arg-range (range (count parameter-types))
        args (map #(symbol (str "arg" %)) arg-range)]
    (cons (into [] args)
          (list `(. *gl* ~(cons name args))))))

(defmacro generate-opengl-api
  [cls]
  (let [members (->> (:members (r/reflect (resolve cls)))
                     (filter #(and (instance? clojure.reflect.Method %)
                                   (:public (:flags %))
                                   (not (:static (:flags %))))))]
    `(do ~@(for [[name fn-methods] (group-by :name members)
                 :let [arg-counts (map (comp count :parameter-types) fn-methods)
                       fn-methods (if (== (count (distinct arg-counts)) 1)
                                    [(first fn-methods)]
                                    fn-methods)]]
             `(defn ~name
                ~@(map fn-method fn-methods))))))

(generate-opengl-api GL)
(generate-opengl-api GLBase)
(generate-opengl-api GL2ES2)

(defn load-shader
  [src type]
  (let [shader (glCreateShader (case type
                                 :vertex GL2ES2/GL_VERTEX_SHADER
                                 :fragment GL2ES2/GL_FRAGMENT_SHADER))
        compiled (IntBuffer/allocate 1)]
    (when-not (zero? shader)
      (glShaderSource shader (int 1) (into-array String [src]) nil)
      (glCompileShader shader)
      (glGetShaderiv shader GL2ES2/GL_COMPILE_STATUS compiled)
      (when (zero? (.get compiled))
        shader))))

(defprotocol Canvas
  (setup [canvas])
  (draw [canvas user-data]))

(defn hello-triangle
  "OpenGL ES 2.0 Programming Guide example pg. 22"
  []
  (reify Canvas
    (setup [canvas]
      (let [vertex (load-shader (slurp "resources/glsl/main.vert") :vertex)
              fragment (load-shader (slurp "resources/glsl/main.frag")
                                    :fragment)
            program (glCreateProgram)
            linked? (IntBuffer/allocate 1)
            vertices (float-array [0.0 0.5 0.0
                                   -0.5 -0.5 0.0
                                   0.5 -0.5 0.0])]
        (when-not (zero? program)
          (glAttachShader program vertex)
          (glAttachShader program fragment)
          (glBindAttribLocation program 0 "vPosition")
          (glLinkProgram program)
          (glGetProgramiv program GL2ES2/GL_LINK_STATUS linked?)
          (when (zero? (.get linked?))
            (glClearColor 0.0 0.0 0.0 1.0)
            {:program program
             :vertices vertices}))))
    (draw [canvas {:keys [program vertices]}]
      (glClear GL/GL_COLOR_BUFFER_BIT)
      (glUseProgram program)
      ;; (glVertexAttribPointer (int 0) (int 3)
      ;;                        GL/GL_FLOAT GL/GL_FALSE (int 0) vertices)
      ;; (glEnableVertexAttribArray 0)
      ;; (glDrawArrays GL/GL_TRIANGLES 0 3)
      )))

(defn gl-event-listener
  [f]
  (let [user-object (f)
        user-data (atom nil)]
    (proxy [GLEventListener] []
      (init [drawable]
        (binding [*gl* (.getGL2ES2 (.getGL drawable))]
          (reset! user-data (setup user-object))))
      (display [drawable] 
        (binding [*gl* (.getGL2ES2 (.getGL drawable))]
          (draw user-object @user-data)))
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

(defn -main
  [& args]
  (letfn [(setup-window [window]
            (doto window
              (.setSize 640 480)
              (.setVisible true)
              (.setTitle "JOGL")
              (.addWindowListener (proxy [WindowAdapter] []
                                    (windowDestroyNotify [e])))
              (.addGLEventListener (gl-event-listener hello-triangle)))
            (.start (FPSAnimator. window 60)))]
    (c/start-system (gl-system {:setup-window setup-window}))))
