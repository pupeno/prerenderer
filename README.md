# Prerenderer

[![Join the chat at https://gitter.im/carouselapps/prerenderer](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/carouselapps/prerenderer?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This library helps implementing server side prerendering of ClojureScript heavy applications such SPAs (Single Page
Applications) by use of NodeJS.

SPAs are the ones that ship a minimal HTML and a JavaScript application to the browser and then the
JavaScript running in the browser renders the application. Some notable examples of this technique is Google Maps (the
one that started it all) and GMail. If you want to see a live demo of a SPA, check out our screencast
[What is a Single Page Application?](https://carouselapps.com/2015/11/19/what-is-a-single-page-application/).

This method of developing applications lead to a much better user experience as the application is snappier so it feels
similar to a native application and at the same time it can react to user events that the traditional mode of
development tend to ignore. In the Clojure world libraries such as
[Reagent](https://github.com/reagent-project/reagent), [re-frame](https://github.com/Day8/re-frame), [Om](https://github.com/omcljs/om),
etc. help with this task.

The problem with developing applications like this is that not all user agents execute JavaScript. Notably search engine
crawlers will fetch those pages and index them as no content, as the content would come later when JavaScript runs. Also
sites such as Facebook, LinkedIn, Twitter, when you submit a link, they fetch content and embed a snippet. These would
also fail with an SPA. Over time more and more of these agents will execute JavaScript but I wouldn't count on this.

The solution is of course to prerender the application on the server and then send this prerendered version to the web
agent, whether it is a browser or a crawler. When the application is implemented mostly in ClojureScript, to render it
on the server, we either need to re-implement it in Clojure or find a way to execute that ClojureScript. This library
helps you execute that ClojureScript on the server.

This technique is sometimes called isomorphic JavaScript, but a lot of people have a profound dislike for that name and
have proposed universal JavaScript instead.

## Design

NodeJS was not our first choice of technology for this task and indeed it came with its many complexities that this
library tries to abstract away. We first [attempted to use Nashorn](https://carouselapps.com/2015/09/11/isomorphic-clojurescriptjavascript-for-pre-rendering-single-page-applications-part-1/),
a JavaScript engine shipped with Java 8 but we found it tool limited. For example, it doesn't implement XMLHttpRequest
which is very likely used in a SPA.

When Nashorn was abandoned, we looked at the many possibilities, like PhantomJS. Ultimately we decided to
[give NodeJS a try]https://carouselapps.com/2015/10/02/isomorphic-javascript-with-clojurescript-for-pre-rendering-single-page-applications-part-3/
because it felt like a first class in the ClojureScript world as the compiler can target it natively, but also
because of the performance of the V8 engine and the wide availability of modules to implement the functionality we need
such as XMLHttpRequest, file system access, web servers, etc.

[Prerenderer](https://carouselapps.com/prerenderer) starts a NodeJS process in the background that loads your
application. It creates a web server that binds a random port and reports the port to a know file. The Clojure side of
Prerenderer picks up that port and whenever you request to pre-render a page it'll send a request to that port. Back
inside NodeJS, Prerender will call a function that you define that will do the actual pre-render and then return it back
to Clojure.

Prerenderer abstracts away as much as possible the details of running NodeJS, starting a secondary web process in it,
sending requests to it, and sending back the results of pre-rendering. As a user of Prerenderer it looks almost as if
you are calling ClojureScript from Clojure. For the NodeJS web server, it uses the popular [express](http://expressjs.com/)
micro-framework.

For supporting AJAX, Prerenderer uses the [XMLHttpRequest NodeJS module](https://github.com/driverdan/node-XMLHttpRequest)
but unfortunately we found [a bug in it](https://github.com/driverdan/node-XMLHttpRequest/pull/115) and we also needed a
feature: [to set up default destination for AJAX calls that use relative paths](https://github.com/driverdan/node-XMLHttpRequest/pull/116),
a very common technique in JavaScript applications. Pull requests have been submitted and until then, Prerenderer uses
our [own release of node-XMLHttpRequest: @pupeno/node-XMLHttpRequest](https://www.npmjs.com/package/@pupeno/xmlhttprequest).

The elephant in the room of course is that SPAs are never *done*. Imagine a SPA that has a timer and every second sends
a request to the server, and the server replies with the current time, which then the application displays. When is it
done rendering? Never. But Prerenderer needs to, at some point, decide that the page is *done enough* and ship it to
the browser.

If you are using plain Reagent it's up to you to decide when the application is done enough. If you are using Re-frame,
Prerenderer ships with a simple helper to help you deal with it. For other libraries/frameworks, such as Om, Om Next,
Petrol, you'll also have to find the appropriate solution and pull requests are welcome.

### re-frame

Doing prerendering with re-frame requires version 0.6.0 or later due to the new callback mechanism and queue systems.
The way it works is that Prerender will watch for events and once nothing happened for a period of time (300ms by
default) it'll consider the application done and if a certain amount of time went by (3s by default) even if the
application is still active, it'll stop and send it to the browser.

The current default times are completely arbitrary so don't give them too much credit. It's likely that each application
will require their own tuning for maximum performance and results. Please, do [let us know](https://carouselapps.com/contact-us/)
your finding here and we'll use that information for providing better defaults in future releases.

This solution is far from perfect. Particularly it means that all pages have an extra 300ms load time. There are two
possible solutions for that:
- [Check whether there's a pending AJAX call](https://github.com/carouselapps/prerenderer/issues/5)
- [Have a way for the app to signal that it's done rendering](https://github.com/carouselapps/prerenderer/issues/6)

### Om

We don't use Om. Pull requests are welcome.

### Om Next

We don't use Om Next. Pull requests are welcome.

### Others

Pull requests are welcome.

## Usage

Include the library on your `project.clj`:

```clojure
[com.carouselapps/prerenderer "0.2.0"]
```

### Clojure

On the Clojure side first, as your application is starting, you need to start the JavaScript engine:

```clojure
(prerenderer/start! {:path "target/js/server-side.js"}))
```

Most likely you want to keep the JavaScript engine in an atom:

```clojure
(def js-engine (atom nil))

(reset! js-engine (prerenderer/run {:path "target/js/server-side.js"})))
```

When you run Prerenderer like that, if `target/js/server-side.js` is not present, it'll raise an exception. You can tell
it to wait for it to appear, useful in development mode, by passing the attribute `:wait`:

```clojure
(reset! js-engine (prerenderer/run {:path "target/js/server-side.js"
                                    :wait true})))
```

If your JavaScript app runs AJAX requests with relative paths (very common) such as `GET /users`, the app will make the
request to `localhost:3000`. You can define both of this by passing `:default-ajax-host` and `:default-ajax-port`:

```clojure
(reset! js-engine (prerenderer/run {:path              "target/js/server-side.js"
                                    :default-ajax-host "192.168.1.1"
                                    :default-ajax-port 12345})))
```

For an actual example of this, look at [Ninja Tool's core.clj, around line 23](https://github.com/carouselapps/ninjatools/blob/master/src/clj/ninjatools/core.clj#L23).
You want them to point to where the Clojure server is running. In many cases for example, the port will be random.

Also, you may want to specify the working directory for the Node.js process like this:

```clojure
(reset! js-engine (prerenderer/run {:path              "js/server-side.js"
                                    :working-directory "target"})))
```

After that, prerendering happens by simply doing:

```clojure
(prerenderer/render @js-engine url headers)
```

where `url` is the URL you are prerendering and `headers` is map of the headers you want the ClojureScript to see
(important for cookies for example, [altough currently not properly supported](https://github.com/carouselapps/prerenderer/issues/14)).
If you are using Ring, you can do something such as:

```clojure
(prerenderer/render @js-engine (ring.util.request/request-url request) (:headers request))
```

where `request` is the Ring request.

### ClojureScript

The ClojureScript side of Prerenderer is a bit more involved. Prerenderer uses NodeJS and a few JavaScript libraries.
To install these libraries it uses [npm](https://www.npmjs.com/) so you need the [lein-npm plug in](https://github.com/RyanMcG/lein-npm)
in your project. Something like:

```clojure
:plugins [; other plugins
          [lein-npm "0.6.1"]]
```

Running `lein deps` will install the necessary modules to your project's node_modules directory which I recommend adding
to your list of ignored files for your source control system (`.gitignore`, `.hgignore`, etc.).

You need to compile the application for running in NodeJS and you'll also need to include some extra code that is NodeJS
specific and you don't want to ship with your application. If you have your ClojureScript in `src/cljs`, I'd recommend
`src/node`; and if you have it on `src-cljs`, I'd go for `src-node`. It's up to you. Let's say your cljsbuild
configuration looks like this:

```clojure
:cljsbuild {:builds {:app {:source-paths ["src/cljs"]
                           :compiler     {:output-dir "resources/public/js/app"
                                          :output-to  "resources/public/js/app.js"}}}}
```

You'll want to add a second build so it'll look like this:

```clojure
:cljsbuild {:builds {:app         {:source-paths ["src/cljs"]
                                   :compiler     {:output-dir "resources/public/js/app"
                                                  :output-to  "resources/public/js/app.js"}}
                     :server-side {:source-paths ["src/cljs" "src/node"]
                                   :compiler     {:output-dir "target/js/server-side"
                                                  :output-to  "target/js/server-side.js"
                                                  :main       "projectx.node"
                                                  :target     :nodejs}}}}
```

Important parts are:
- inclusion of `src/node`
- defining main as `projectx.node`
- targeting NodeJS

Remember to also add it to your dev and uberjar profiles as needed but I'd refrain from any sort of optimizations. I
found them from problematic to just-not-working-on-NodeJS due to the NodeJS modules being out of scope for the compiler
and they are not really needed.

As a reference, this is what I would use for a dev profile:

```clojure
:server-side {:compiler {:optimizations :none
                         :source-map    true
                         :pretty-print  true
                         :verbose       true}}
```

and this for an uberjar:

```clojure
:server-side {:compiler {:optimizations :none
                         :source-map    true
                         :pretty-print  true}}
```

Yes, pretty print, why not? And I included source maps in case NodeJS could pick it up and give me better stack traces
but I didn't look into it yet.

`projectx.node` will implement your NodeJS specific part of the application, which will look something like this:

```clojure
(ns projectx.node
  (:require [prerenderer.core :as prerenderer]))

(defn render-and-send [page-path send-to-browser]
  (send-to-browser (render page-path)))

(set! *main-cli-fn* (prerenderer/create render-and-send "ProjectX"))
```

`prerenderer.core/create` creates the prerenderer and takes two arguments: the rendering function and the name of the
application. The name of your application is only used for logging and reporting purposes and it's just a simple string,
whatever you want.

`render-and-send` receives two attributes, `page-path` and `send-to-browser`. `page-path` is the path that is being
requested, the one you have to render, while `send-to-browser` is a function that will send the data back to the
browser, that is, triggering a NodeJS Express response.

### re-frame

It's common in re-frame to start with a minimalistic HTML, trigger and event that then builds the page optionally
triggering many other events. We need to render the page to a string only after all events have been handled. Currently
we ship a simple heuristic: wait for 300ms of no events being triggered or 3s total, whichever happens first. When it
looks like the page is rendered, a thunk is called back that should return the desired string to be sent to the client.

Since all of this is asyncronous and JavaScript is single-threaded, we cannot just wait for it to finish and then call
`send-to-browser`. That's why Prerenderer ships with a helper function, called `render-by-timeout`, which implements the
heuristics previously described and this is how you use it:

```clojure
(defn render-and-send [page-path send-to-browser]
  (re-frame/dispatch-sync [:initialize-db])
  (re-frame/dispatch-sync [:whatever-is-needed-to-render page-path])
  (re-frame-prerenderer/render-by-timeout [views/main-panel] send-to-browser)))

(set! *main-cli-fn* (prerenderer/create render-and-send "Project X"))
```

The first argument, is the actual component to be render. In this case, Prerenderer will run~

```clojure
(reagent/render-to-string [views/main-panel])
```

and use ```send-to-browser``` to dispatch the result back to the browser. You can specify your own timeouts if you want:

```clojure
(re-frame-prerenderer/render-by-timeout [views/main-panel] send-to-browser) 400 4000)
```

For a real life example of its usage, please, check [Ninja Tool's node.cljs](https://github.com/carouselapps/ninjatools/blob/master/src/node/ninjatools/node.cljs).

### Heroku

If you are deploying to Heroku, you'll quickly find out that NodeJS is not installed on your Clojure dynos. Adding the
NodeJS buildpack won't help because it'll try to detect whether your application is a NodeJS one and it'll fail. Making
it look like a NodeJS application and adding an empty `package.json` will make lein-npm not work.

If you want to stay up to date on this matter, I'd recommend following this issue [heroku-buildpack-clojure/issues/44](https://github.com/heroku/heroku-buildpack-clojure/issues/44).
In the meantime, you need to use the nodejs branch of the Clojure buildpack:

```bash
$ heroku buildpacks:set https://github.com/heroku/heroku-buildpack-clojure#nodejs
```

You also need Heroku to run `lein deps` to fetch your dependencies when it's building your uberjar. That can be achieved
by adding:

```clojure
:prep-tasks  ["deps" "javac" "compile"]
```

to your uberjar profile. You can get some background about this issue in
[lein-npm/issues/28](https://github.com/RyanMcG/lein-npm/issues/28).

## Changelog

### v0.2.0
- Changed the ClojureScript API to hide NodeJS details.
- New re-frame implementation that depends on re-frame 0.6.0 but not on a fork.
- [Added an option to specify the working directory for the JavaScript engine. Courtesy of Andrey Subbotin.](https://github.com/carouselapps/prerenderer/pull/12)
- Added Function to stop JavaScript engine.
- Renamed run to start! to match stop!
- Added option :noop-when-stopped that will make prerenderer just issue a warning when the JavaScript engine is not
running.

### v0.1.0 - 2015-09-23
- Initial version. For more information, check out https://carouselapps.com/2015/10/02/isomorphic-javascript-with-clojurescript-for-pre-rendering-single-page-applications-part-3/

## License

This library has been extracted from the project [Ninja Tools](http://tools.screensaver.ninja).

Copyright © 2015 Carousel Apps, Ltd. All rights reserved.

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
