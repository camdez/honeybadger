(ns honeybadger.core
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [clj-stacktrace.core :as st]
            [clj-stacktrace.repl :as st-repl]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.walk :refer [keywordize-keys]]
            [honeybadger.schemas :refer [Event EventFilter]]
            [honeybadger.utils
             :refer
             [deep-merge some-chain underscore update-contained-in]]
            [manifold.deferred :as d]
            [schema.core :as s]))

(def notifier-name
  "Honeybadger for Clojure")

(def notifier-version
  "0.3.1-SNAPSHOT")

(def notifier-homepage
  "https://github.com/camdez/honeybadger")

(def api-endpoint
  "https://api.honeybadger.io/v1/notices")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ex-chain
  "Return all exceptions in the exception chain."
  [e]
  (->> e
       (iterate #(when (some? %) (.getCause %)))
       (take-while some?)))

(defn- format-stacktrace-elem [{:keys [line file] :as elem}]
  {:number line
   :file   file
   :method (st-repl/method-str elem)})

(defn- format-stacktrace [st]
  (->> st st/parse-trace-elems (map format-stacktrace-elem)))

(defn error-map-no-causes
  "Return a map representing an HB error without causes.

  See http://docs.honeybadger.io/guides/api.html#sample-payload for the error
  schema."
  [^Throwable t]
  {:message (str t)
   :class (.. t getClass getName)
   :backtrace (format-stacktrace (.getStackTrace t))})

(defprotocol Notifiable
  (error-map [this]))

(extend-protocol Notifiable
  String
  (error-map [this] {:message this})

  Throwable
  (error-map [this]
    (let [exs (rest (ex-chain this))]
      (assoc (error-map-no-causes this)
             :causes (map error-map-no-causes exs)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def hostname
  (.getHostName (java.net.InetAddress/getLocalHost)))

(def project-root
  (.getCanonicalPath (io/file ".")))

(defn- base-notice [environment]
  {:notifier {:name notifier-name
              :language "clojure"
              :version notifier-version
              :url notifier-homepage}
   :server   {:project-root project-root
              :environment-name environment
              :hostname hostname}})

(defn- error-patch [notifiable]
  {:error (error-map notifiable)})

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

(s/defn ^:private normalize-event :- Event
  "Normalize data to a standard form that user-provided filters can
  make sense of and transform."
  [api-key env msg-or-ex metadata]
  (-> {:api-key   api-key
       :env       (keyword env)
       :exception msg-or-ex
       :metadata  metadata}
      keywordize-keys
      (update-contained-in [:metadata :tags] #(set (map keyword %)))
      (update-contained-in [:metadata :request :method] keyword)
      (update-contained-in [:metadata :request :params]  #(or % {}))
      (update-contained-in [:metadata :request :session] #(or % {}))
      (->> (deep-merge {:metadata {:tags #{}
                                   :request {}
                                   :context {}
                                   :component nil
                                   :action nil}}))))

(s/defn ^:private apply-filters :- (s/maybe Event)
  "Successively apply all transformation functions in `filters` to
  exception details, halting the chain if any filter returns nil."
  [filters :- [EventFilter]
   event   :- Event]
  (some-chain event filters))

(s/defn ^:private event->notice
  "Convert data to the appropriate form for the Honeybadger API.

   If the exception has a cause, all causes in the chain are sent to HB."
  [{:keys [env exception metadata]} :- Event]
  (deep-merge (base-notice env)
              (error-patch exception)
              (metadata-patch metadata)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public

(defn notify
  ([config msg-or-ex]
   (notify config msg-or-ex {}))
  ([{:keys [api-key env filters]} msg-or-ex metadata]
   (if-let [e (->> (normalize-event api-key env msg-or-ex metadata)
                   (apply-filters filters))]
     (post-notice (event->notice e) api-key)
     (d/success-deferred nil))))
