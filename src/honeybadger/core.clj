(ns honeybadger.core
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [clj-stacktrace.core :as st]
            [clj-stacktrace.repl :as st-repl]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [manifold.deferred :as d]))

(def notifier-name
  "Honeybadger for Clojure")

(def notifier-version
  "0.0.1")

(def notifier-homepage
  "https://github.com/camdez/honeybadger")

(def api-endpoint
  "https://api.honeybadger.io/v1/notices")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- underscore [key]
  (-> key name (str/replace "-" "_")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def hostname
  (.getHostName (java.net.InetAddress/getLocalHost)))

(def project-root
  (.getCanonicalPath (clojure.java.io/file ".")))

(defn- base-notice [environment]
  {:notifier {:name notifier-name
              :language "clojure"
              :version notifier-version
              :url notifier-homepage}
   :server   {:project-root project-root
              :environment-name environment
              :hostname hostname}})

(defn- format-stacktrace-elem [{:keys [line file] :as elem}]
  {:number line
   :file   file
   :method (st-repl/method-str elem)})

(defn- format-stacktrace [st]
  (->> st st/parse-trace-elems (map format-stacktrace-elem)))

(defmulti  notice-error class)
(defmethod notice-error String
  [message]
  {:message message})
(defmethod notice-error Throwable
  [ex]
  {:message (.getMessage ex)
   :class (.getName (.getClass ex))
   :backtrace (format-stacktrace (.getStackTrace ex))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- post-notice [n api-key]
  (d/chain (http/post api-endpoint
                      {:accept :json
                       :content-type :json
                       :headers {"X-API-Key" api-key}
                       :body (json/write-str n :key-fn underscore)})
    :body
    bs/to-string
    #(json/read-str % :key-fn keyword)
    :id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(defn notify [{:keys [api-key env]} msg-or-ex]
  (-> (base-notice env)
      (assoc :error (notice-error msg-or-ex))
      (post-notice api-key)))
