(ns last-train.cli
  "Command line options and the terminal game loop on *in* and *out*."
  (:require [last-train.game :as game]))

(defn- options [args]
  (loop [args args opts {}]
    (if-let [[arg & more] (seq args)]
      (if-let [[_ v] (re-matches #"--seed=(.*)" arg)]
        (recur more (assoc opts :seed v))
        (if (= "--seed" arg)
          (if-let [[value & remaining] (seq more)]
            (recur remaining (assoc opts :seed value))
            {:error (str "Missing value for " arg)})
          {:error (str "Unknown option: " arg)}))
      opts)))

(defn parse-args
  "Seed chosen by args: a number, or random (the default); rand-int picks a random seed."
  ([args] (parse-args args rand-int))
  ([args rand-int]
   (let [{:keys [error seed]} (options args)]
     (cond
       error {:error error}
       (or (nil? seed) (= "random" seed)) {:seed (rand-int 1000000000)}
       (re-matches #"\d{1,9}" seed) {:seed (parse-long seed)}
       :else {:error (str "Unknown seed: " seed ". Use a number or random.")}))))

(defn- print-lines [lines]
  (doseq [line lines] (println line))
  (flush))

(defn play
  "Run a game on *in* and *out* until it ends or input runs out."
  [seed]
  (let [{:keys [state output]} (game/start seed)]
    (print-lines output)
    (loop [state state]
      (when-not (:over? state)
        (print "> ")
        (flush)
        (when-let [line (read-line)]
          (let [step (game/handle state line)]
            (print-lines (:output step))
            (recur (:state step))))))))
