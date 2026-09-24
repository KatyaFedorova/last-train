(ns last-train.site
  "Builds the static site into build/site/ and serves it locally.
  Kept out of src/ so test tools skip it."
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [last-train.serve-options :as options]
            [org.httpkit.server :as http]))

(def site-dir "build/site")

(def ^:private core-files ["logic" "english" "game" "puzzles" "terminal"])

(defn build!
  "Fresh build/site/: the web/ page plus the shared .cljc game core."
  []
  (fs/delete-tree site-dir)
  (fs/create-dirs (fs/path site-dir "last_train"))
  (doseq [file (fs/list-dir "web")]
    (fs/copy file (fs/path site-dir (fs/file-name file))))
  (doseq [file core-files]
    (fs/copy (str "src/last_train/" file ".cljc") (fs/path site-dir "last_train" (str file ".cljc"))))
  site-dir)

(def ^:private content-types
  {"html" "text/html; charset=utf-8" "css" "text/css; charset=utf-8"
   "cljs" "text/plain; charset=utf-8" "cljc" "text/plain; charset=utf-8"})

(defn- handle [{:keys [uri]}]
  (let [path (str/replace (if (str/ends-with? uri "/") (str uri "index.html") uri) #"^/+" "")
        file (fs/path site-dir path)]
    (if (and (not (str/includes? path "..")) (fs/regular-file? file))
      {:status 200
       :headers {"Content-Type" (get content-types (fs/extension file) "application/octet-stream")}
       :body (fs/file file)}
      {:status 404 :headers {"Content-Type" "text/plain"} :body "Not found"})))

(defn serve!
  "Build the site and serve it on port; returns a stop function."
  [port]
  (build!)
  (http/run-server handle {:port port}))

(defn -main [& args]
  (let [{:keys [error port]} (options/parse-args args)]
    (if error
      (do (binding [*out* *err*] (println error))
          (System/exit 2))
      (do (serve! port)
          (println (str "LAST TRAIN is running at http://localhost:" port "/"))
          @(promise)))))
