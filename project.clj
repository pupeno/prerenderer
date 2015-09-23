;;;; Copyright © 2015 Carousel Apps, Ltd. All rights reserved.

(defproject com.carouselapps/prerenderer "0.1.0"
  :description "Server pre-rendering for Single Page Applications using ClojureScript/JavaScript by use of NodeJS."
  :url "https://CarouselApps.com/prerenderer"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :lein-release {:deploy-via :clojars}
  :signing {:gpg-key "F2FB1C6F"}
  :scm {:name "git"
        :url  "https://github.com/carouselapps/prerenderer"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "1.7.48"]
                 [org.clojure/tools.cli "0.3.3"]
                 [clj-http "2.0.0"]]
  :plugins [[lein-npm "0.6.1"]]
  :npm {:dependencies [["@pupeno/xmlhttprequest" "1.7.0"]
                       [express "4.13.3"]
                       [cookie-parser "1.4.0"]]}

  :source-paths ["src/clj" "src/cljs"])
