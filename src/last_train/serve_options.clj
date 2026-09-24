(ns last-train.serve-options
  "Options for serving the web game locally: bin/last-train-web [--port N].")

(defn- split-port
  "Args with a leading --port=value split into --port and value."
  [args]
  (if-let [[_ v] (some->> (first args) (re-matches #"--port=(.*)"))]
    (list* "--port" v (drop 1 args))
    args))

(defn parse-args
  "Server options: {:port n} or {:error message}."
  [args]
  (let [[option value & more] (split-port args)
        port (some-> value parse-long)]
    (cond
      (empty? args) {:port 8080}
      (not= "--port" option) {:error (str "Unknown option: " option)}
      (nil? value) {:error "Missing value for --port"}
      (not (and port (pos? port) (< port 65536))) {:error (str "Bad port: " value)}
      (seq more) {:error (str "Unknown option: " (first more))}
      :else {:port port})))
