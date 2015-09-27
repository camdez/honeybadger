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
  "0.2.0-SNAPSHOT")

(def notifier-homepage
  "https://github.com/camdez/honeybadger")

(def api-endpoint
  "https://api.honeybadger.io/v1/notices")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- underscore [key]
  (-> key name (str/replace "-" "_")))

(defn- deep-merge
  "Recursively merge maps. At each level, if there are any non-map
  vals, the last value (of any type) is used."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

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

(defn- error-patch [msg-or-ex]
  {:error (notice-error msg-or-ex)})

(defn- metadata-patch [{:keys [tags context component action request]}]
  (let [{:keys [method url params session]} request]
    {:error   {:tags tags}
     :request {:url url
               :component component
               :action action
               :params params
               :context (or context {})  ; diplays differently if nil
               :session session
               :cgi-data (some->> method
                                  name
                                  str/upper-case
                                  (array-map "REQUEST_METHOD"))}}))

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

(defn notify
  ([config msg-or-ex]
   (notify config msg-or-ex {}))
  ([{:keys [api-key env]} msg-or-ex metadata]
   (-> (base-notice env)
       (deep-merge (error-patch msg-or-ex)
                   (metadata-patch metadata))
       (post-notice api-key))))
