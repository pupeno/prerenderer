;;;; Copyright © 2015 Carousel Apps, Ltd. All rights reserved.

(ns prerenderer.re-frame
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defn- current-time []
  (.getTime (js/Date.)))

(defn render-by-timeout
  ([component send-to-browser]
   (render-by-timeout component send-to-browser 300 3000))
  ([component send-to-browser event-timeout total-timeout]
   (let [events-in-last-timeout-window (atom 0)
         start-time (current-time)]
     (re-frame/add-post-event-callback (fn [event-v queue]
                                         (println (first event-v))
                                         (swap! events-in-last-timeout-window inc)))
     (go-loop []
       (<! (timeout event-timeout))
       (when-not (or (= @events-in-last-timeout-window 0)
                     (> (- (current-time) start-time) total-timeout))
         (reset! events-in-last-timeout-window 0)
         (recur))
       (send-to-browser (reagent/render-to-string component))))))
