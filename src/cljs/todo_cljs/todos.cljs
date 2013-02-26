(ns todo-cljs.todos
  (:require [clojure.browser.repl :as repl]
            [clojure.browser.dom  :as dom]))

;; helpers, constants...
(def ENTER_KEY 13)
(defn by-id [id] (dom/get-element id))

(defn hello[] (js/alert "hello!"))
(def todo-list (atom []))
(def stat (atom {}))

(defn change-toggle-all-checkbox-state [] )

(defn save-todos []
  (.setItem js/localStorage "todos-cljs"
            (.stringify js/JSON (clj->js @todo-list))))

(defn compute-stats []
  (let [total     (count @todo-list)
        completed (count (filter #(= true (% "completed")) @todo-list))
        left      (- total completed)]
    (swap! stat conj {:total total :completed completed :left left})))

(defn redraw-todos-ui []
  (let [ul (by-id "todo-list")]
    (set! (.-innerHTML ul) "")
    (set! (.-value (by-id "new-todo")) "")
    (dorun ;; materialize lazy list returned by map below
     (map
      (fn [todo]
        (let [li (dom/element :li)
              checkbox        (dom/element :input)
              label           (dom/element :label)
              delete-link     (dom/element :button)
              div-display     (dom/element :div)
              input-edit-todo (dom/element :input)]
          (set! (.-className checkbox) "toggle")
          (.setAttribute checkbox "data-todo-id" (todo "id"))
          (set! (.-type checkbox) "checkbox")
          ;; TODO add event listener to checkbox

          (.setAttribute label "data-todo-id" (todo "id"))
          (dom/append label (.createTextNode js/document (todo "title")))
          ;; TODO add event listener to doubleclick on label

          (set! (.-className delete-link) "destroy")
          (.setAttribute delete-link "data-todo-id" (todo "id"))
          ;; TODO add even listener to click on button

          (set! (.-className div-display) "view")
          (.setAttribute div-display "data-todo-id" (todo "id"))
          (dom/append div-display checkbox label delete-link)

          (set! (.-id input-edit-todo) (str "input_" (todo "id")))
          (set! (.-className input-edit-todo) "edit")
          (set! (.-value input-edit-todo) (todo "title"))
          ;; TODO add even listener to input

          (set! (.-id li) (str "li_" (todo "id")))
          (dom/append li div-display input-edit-todo)

          (if (todo "completed")
            (do
              (set! (.-className li) "complete")
              (set! (.-checked checkbox) true)))

          (dom/append ul li)))
      @todo-list))))

(defn refresh-data []
  (do
    (save-todos)
    (compute-stats)
    (redraw-todos-ui)
    ;; (redraw-status-ui)
    (change-toggle-all-checkbox-state)))

(declare get-uuid)

(defn add-todo [text]
  (let [trimmed (.trim text)]
    (if (> (count trimmed) 0)
      (do
        (swap! todo-list conj {"id" (get-uuid) "title" trimmed, "completed" false})
        (refresh-data)))))

(defn new-todo-handler [ev]
  (if (= ENTER_KEY (.-keyCode ev))
    (add-todo (.-value (by-id "new-todo")))))

;; This get-uuid fn is almost equiv to the original
(defn get-uuid []
  (apply
   str
   (map
    (fn [x]
      (if (= x \0)
        (.toString (bit-or (* 16 (.random js/Math)) 0) 16)
        x))
    "00000000-0000-4000-0000-000000000000")))

(defn toggle-all-handler [ev]
  (let [checked (.-checked (.-target ev))
        toggled (map #(assoc % "completed" checked) @todo-list)]
    (reset! todo-list toggled)
    (refresh-data)))

(defn add-event-listeners []
  (.addEventListener (by-id "new-todo") "keypress" new-todo-handler false)
  (.addEventListener (by-id "toggle-all") "change" toggle-all-handler false))

(defn window-load-handler []
  (add-event-listeners))

;; Launch window-load-handler when window loads
(.addEventListener js/window "load" window-load-handler false)

;; connect a browser-attached repl:
(repl/connect "http://localhost:9000/repl")

;; debugging:
;; (in-ns 'todo-cljs.todos)
;; (add-todo "one")
;; (add-todo "two")
;; (add-todo "three")
;; (map #(js/alert %) @todo-list)
