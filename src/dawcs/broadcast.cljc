(ns dawcs.broadcast)

(def ^:private bus (atom nil))

(defn- gen-sub-id []
  (-> "dawcs.broadcast/subscription-" gensym keyword))

(defn pub!
  "Publishes message to channel (::default if not specified). If message is a map, it's passed as is, otherwise is turned into map with ::message key"
  ([msg] (pub! ::default msg))
  ([channel msg]
   (let [message (if (map? msg) msg {::message msg})]
     (reset! bus (merge {::channel channel} message)))))

(defn sub!
  "Adds subscription to channel (::default if not specified). When channel receives a message it's passed to specified handler without ::channel key. If specified channel is ::all, subscription applies to all channels and message contains ::channel key. Returns function which accepts 0 args and unsubscribes from channel"
  ([handler]
   (sub! ::default handler))
  ([channel handler]
   (let [sub-id (gen-sub-id)]
     (add-watch bus
                sub-id
                (fn [_ _ _ data]
                  (cond
                    (= channel (::channel data))
                    (handler (dissoc data ::channel))

                    (= channel ::all)
                    (handler data))))
     (fn [] (remove-watch bus sub-id)))))

(defn once!
  "Works the same as `sub!` but unsubscribes from channel after first message"
  ([handler]
   (once! ::default handler))
  ([channel handler]
   (let [sub-id (gen-sub-id)]
     (add-watch bus sub-id
                (fn [_ _ _ data]
                  (cond
                    (= channel (::channel data))
                    (do (handler (dissoc data ::channel))
                        (remove-watch bus sub-id))

                    (= channel ::all)
                    (do (handler data)
                        (remove-watch bus sub-id)))))
     (fn [] (remove-watch sub-id)))))

(defn unsub-all!
  "Unsubscribes all listeners"
  []
  (let [subs #?(:clj (-> bus .getWatches keys)
                :cljs (-> bus .-watches keys))]
    (doseq [sub-id subs]
      (remove-watch bus sub-id))))
