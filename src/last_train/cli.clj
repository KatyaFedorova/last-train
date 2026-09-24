(ns last-train.cli
  "Command line options and the terminal game loop on *in* and *out*."
  (:require [clojure.string :as str]
            [last-train.game :as game]
            [last-train.puzzles :as puzzles]))

(defn- seed-names []
  (concat (map :name puzzles/catalog) ["random"]))
(def ^:private voices #{"template"})

(defn- options [args]
  (loop [args args opts {}]
    (if-let [[arg & more] (seq args)]
      (if-let [[_ k v] (re-matches #"--(seed|voices)=(.*)" arg)]
        (recur more (assoc opts k v))
        (if-let [[_ k] (re-matches #"--(seed|voices)" arg)]
          (if-let [[value & remaining] (seq more)]
            (recur remaining (assoc opts k value))
            {:error (str "Missing value for " arg)})
          {:error (str "Unknown option: " arg)}))
      opts)))

(defn parse-args
  "Puzzle and voices chosen by args; rand-int picks the random seed's puzzle."
  ([args] (parse-args args rand-int))
  ([args rand-int]
   (let [opts (options args)
         seed (get opts "seed" "reference")
         voice (get opts "voices" "template")]
     (cond
       (:error opts) opts
       (not (some #{seed} (seed-names)))
       {:error (str "Unknown seed: " seed ". Supported: " (str/join ", " (seed-names)))}
       (not (voices voice)) {:error (str "Unknown voices: " voice ". Supported: template")}
       :else {:puzzle (or (puzzles/by-name seed) (puzzles/pick rand-int nil)) :voices voice}))))

(defn- print-lines [lines]
  (doseq [line lines] (println line))
  (flush))

(defn play
  "Run a game on *in* and *out* until it ends or input runs out."
  [puzzle]
  (let [{:keys [state output]} (game/start puzzle)]
    (print-lines output)
    (loop [state state]
      (when-not (:over? state)
        (print "> ")
        (flush)
        (when-let [line (read-line)]
          (let [step (game/handle state line)]
            (print-lines (:output step))
            (recur (:state step))))))))
