;;;; Copyright © 2015 Carousel Apps, Ltd. All rights reserved.

(ns prerenderer.re-frame
  (:require [re-frame.core :as re-frame]
            [re-frame.router :as router]
            [cljs.core.async :refer [chan timeout mult tap alts!]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn dispatch-super-sync
  ([event callback]
   (dispatch-super-sync event callback 300 3000))
  ([event callback event-timeout total-timeout]
   (let [start-time (.getTime (js/Date.))]
     (let [event-chan (chan)]
       (tap router/event-chan-multiplexer event-chan)
       (go-loop []
                (let [timer-chan (timeout event-timeout)
                      [_event chan] (alts! [event-chan timer-chan])]
                  (if (or (= chan timer-chan) (> (- (.getTime (js/Date.)) start-time) total-timeout))
                    (callback)
                    (recur)))))
     (re-frame/dispatch-sync event))))
