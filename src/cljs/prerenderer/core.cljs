;;;; Copyright © 2015 Carousel Apps, Ltd. All rights reserved.

(ns prerenderer.core
  (:require [cljs.nodejs :as nodejs]
            [cljs.tools.cli :refer [parse-opts]]
            [clojure.string :as string]))

(nodejs/enable-util-print!)

(def http (nodejs/require "http"))

(def express (nodejs/require "express"))

(def fs (nodejs/require "fs"))

(def cookie-parser (nodejs/require "cookie-parser"))

; Set an environment that ressembles a browser, with ajax and alert.
(def xmlhttprequest (nodejs/require "@pupeno/xmlhttprequest"))
(goog.object/set js/global "XMLHttpRequest" (.-XMLHttpRequest xmlhttprequest))
(goog.object/set js/global "alert" println)

(def cli-options
  [["-p PORT_FILE" "--port-file PORT_FILE" "File to which to write the port number opened for requests"
    :validate [(complement clojure.string/blank?) "A port file must be provided"]]
   ["-h HOST_NAME" "--default-ajax-host HOST_NAME" "Hostname to connect to for AJAX requests"
    :default "localhost"]
   ["-a PORT_NUMBER" "--default-ajax-port PORT_NUMBER" "Port number to use for AJAX requests"
    :default 3000]
   ["-h" "--help"]])

(defn program-name []
  (string/join " " (first (split-with #(re-find #"(\.js|node)$" %) (.-argv nodejs/process)))))

(defn usage [name options-summary]
  (->> [(str name "'s Universal JavaScript engine for server side pre-rendering single page applications.")
        ""
        (str "Usage: " (program-name) " [options]")
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (.exit js/process status))

(defn create [render name]
  (fn [& args]
    (let [{:keys [options _arguments errors summary]} (parse-opts args cli-options)]
      (cond
        (:help options) (exit 0 (usage name summary))
        errors (exit 1 (error-msg errors)))
      (aset (.-defaults xmlhttprequest) "host" (:default-ajax-host options))
      (aset (.-defaults xmlhttprequest) "port" (:default-ajax-port options))
      (let [app (-> (express)
                    (.use (cookie-parser))
                    (.get "/" (fn [_req res] (.send res "Universal JavaScript engine for server side pre-rendering single page applications.")))
                    (.get "/render" render))
            server (.createServer http app)]
        (.listen server 0 (fn [] (.writeFile fs (:port-file options) (.-port (.address server)))))))))
