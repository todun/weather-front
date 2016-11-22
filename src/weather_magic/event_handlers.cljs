(ns weather-magic.event-handlers
  (:require
   [weather-magic.state :as state]
   [thi.ng.geom.gl.camera :as cam]
   [thi.ng.geom.rect  :as rect]
   [weather-magic.world :as world]
   [thi.ng.geom.gl.core  :as gl]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.matrix :as mat :refer [M44]]
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.core :as g]
   [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(defonce zoom-level (atom 110))
(defonce click-variable (atom false))
(defonce mouse-pressed (atom false))
(defonce last-xy-pos (atom {:x-val 0 :y-val 0}))
(defonce relative-mousemovement (atom {:x-val 0 :y-val 0}))

(defn zoom-camera
  "Returns the camera given in camera-map modified zooming by scroll-distance."
  [camera-map scroll-distance]
  (reset! zoom-level (:fov camera-map))
  (cam/perspective-camera
   (assoc camera-map :fov (min 140 (+ @zoom-level (* @zoom-level scroll-distance 5.0E-4))))))

(defn resize-handler [_]
  "Handles the aspect ratio of the webGL rendered world"
  (let [element (.getElementById js/document "main")
        actual-width (.-clientWidth element)
        actual-height (.-clientHeight element)
        webgl-width (.-width element)
        webgl-height (.-height element)]
    (when-not (and (= actual-width webgl-width) (= actual-height webgl-height))
      (set! (.-width (.-canvas state/gl-ctx)) actual-width)
      (set! (.-height (.-canvas state/gl-ctx)) actual-height)
      (swap! state/camera #(cam/perspective-camera
                            (assoc % :aspect (rect/rect actual-width actual-height))))
      (gl/set-viewport state/gl-ctx (:aspect @state/camera)))))

; BÖRJADE MED EN FUNKTION HÄR SOM SKA RETURNERA ROTATION RUNT Y OCH Z AXELN
;(defn get-uprighting-angles
  ;(let [current-up-vector (m/* @state/earth-orientation (vec3 0 1 0))
  ;      phi (atan2 current-up-vector.z current-up-vector.x)
  ;      current-up-vector]))


(defn update-pan
  "Updates the atom holding the rotation of the world"
  [rel-x rel-y]
  (reset! state/earth-orientation (-> M44
                                      ;(g/rotate-z (m/radians 0.5))
                                      (g/rotate-z (* (Math/atan2 rel-y rel-x) -1))
                                      (g/rotate-y (m/radians (* (* (Math/pow (+ (Math/pow rel-y 2) (Math/pow rel-x 2)) 0.5) @zoom-level) 1.0E-3)))
                                      (g/rotate-z (Math/atan2 rel-y rel-x))
                                      (m/* @state/earth-orientation))))

(defn update-pan2
  "Updates the atom holding the rotation of the world"
  [rel-x rel-y step delta-fov]
  (swap! state/camera zoom-camera -15.0)
  (reset! state/earth-orientation (-> M44
                                      (g/rotate-z (* (m/radians (* 0.5 step)) -1))
                                      (g/rotate-z (* (Math/atan2 rel-y rel-x) -1))
                                      (g/rotate-y (m/radians (* (* (Math/pow (+ (Math/pow rel-y 2) (Math/pow rel-x 2)) 0.5) @zoom-level) 1.0E-3)))
                                      (g/rotate-z (Math/atan2 rel-y rel-x))
                                      (g/rotate-z (* (m/radians (* 0.5 (- step 1))) 1))
                                      (m/* @state/earth-orientation))))

(defn move-fcn
  "Handles the movements of the mouse during panning"
  [event]
  (let [last-pos @last-xy-pos
        current-x (.-clientX event)
        current-y (.-clientY event)
        rel-x (- current-x (:x-val last-pos))
        rel-y (- current-y (:y-val last-pos))]
    (update-pan rel-x rel-y))
  (reset! last-xy-pos {:x-val (.-clientX event) :y-val (.-clientY event)}))

(defn mouse-up
  "If the mouse is released during panning"
  [_]
  (reset! click-variable false)
  (reset! mouse-pressed false)
  (.removeEventListener (.getElementById js/document "main") "mousemove" move-fcn false))

(defn continuous-panning
  "Breaks up the panning into smaller steps"
  [x-pos y-pos nr-of-steps]
  (println "panning"))

(defn pointer-zoom-handler
  "Rotates the globe to the point which is dubble clicked"
  [event]
  (let [x-pos (.-clientX event)
        y-pos (.-clientY event)
        element (.getElementById js/document "main")
        width (.-clientWidth element)
        height (.-clientHeight element)]
    (reset! state/pointer-zoom-info {:state true :delta-fov (/ (- 120 (:fov @state/camera)) 100) :delta-x (/ (- (/ width 2) x-pos) 100) :delta-y (/ (- (/ height 2) y-pos) 100) :steps 100}))
    (println @state/pointer-zoom-info))
    ;(dotimes [n 1] (update-pan (- (/ width 2) x-pos) (- (/ height 2) y-pos)))))

(defn pan-handler
  "Handles the mouse events for panning"
  [event]
  (reset! last-xy-pos {:x-val (.-clientX event) :y-val (.-clientY event)})
  (reset! mouse-pressed true)
  (reset! state/earth-animation-fn world/stop-spin!)
  (println "mouse-presed")
  (when (= @mouse-pressed true)
    (.addEventListener (.getElementById js/document "main") "mousemove" move-fcn false)
    (.addEventListener (.getElementById js/document "main") "mouseup" mouse-up false)))

(defn hook-up-events!
  "Hook up all the application event handlers."
  []
  (.addEventListener (.getElementById js/document "main") "wheel"
                     (fn [event] (swap! state/camera zoom-camera (.-deltaY event))) false)
  (.addEventListener js/window "load" resize-handler false)
  (.addEventListener js/window "resize" resize-handler false)
  (.addEventListener (.getElementById js/document "main") "mousedown" pan-handler false)
  (.addEventListener (.getElementById js/document "main") "dblclick" pointer-zoom-handler false)
  true)
