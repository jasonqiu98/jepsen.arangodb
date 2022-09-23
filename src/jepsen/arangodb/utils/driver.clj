(ns jepsen.arangodb.utils.driver
  (:import (com.arangodb DbName)
           com.arangodb.entity.BaseDocument))

(defn get-db
  "Get an arango db instance (type: ArangoDatabase) with its name"
  [conn db-name]
  ; db = conn.db(DbName.of(name))
  (-> conn (.db (DbName/of db-name))))

(defn create-db
  "Create a new database by name"
  [conn db-name]
  (-> conn (get-db db-name) (.create)))

(defn get-collection
  "Get an arangodb collection (type: ArangoCollection) by collection name"
  [conn db-name collection-name]
  ; collection = db.collection(collection-name)
  (-> conn (get-db db-name) (.collection collection-name)))

(defn create-collection
  "Create an arangodb collection by collection name"
  [conn db-name collection-name]
  (-> conn (get-collection db-name collection-name) (.create)))

(defn create-document
  "Create a new document by key"
  [doc-key]
  (new BaseDocument doc-key))

(defn add-attribute
  "Add an attribute key-value pair to a doc"
  [doc attr-key attr-value]
  (-> doc (.addAttribute attr-key attr-value)))

(defn insert-document
  "Insert a document to a collection"
  [conn db-name collection-name doc]
  (-> conn (get-collection db-name collection-name) (.insertDocument doc)))

(def base-document-class
  ; BaseDocument.class, cannot directly access the reflection
  ; create a new instance and then call .getClass method
  (.getClass (new BaseDocument)))

(defn get-document
  "get arangodb document (type: BaseDocument) by key"
  [conn db-name collection-name doc-key]
  ; doc = collection.getDocument(key, BaseDocument.class)
  (-> conn (get-collection db-name collection-name) (.getDocument doc-key base-document-class)))

(defn read-document-key
  "Read the document key from an ArangoDB collection."
  [conn db-name collection-name doc-key]
  (try
    (-> conn (get-document db-name collection-name doc-key) (.getKey))
    (catch java.lang.NullPointerException e nil)))

(defn read-document-attr
  "Read one document attribute from an ArangoDB collection."
  [conn db-name collection-name doc-key attr-key]
  (try
    (-> conn (get-document db-name collection-name doc-key) (.getAttribute attr-key))
    (catch java.lang.NullPointerException e nil)))

(defn update-document
  "Update a document by its key"
  [conn db-name collection-name doc-key doc]
  (-> conn (get-collection db-name collection-name) (.updateDocument doc-key doc)))

(defn set-document-attr
  "Update an attribute of a document if it exists,
   otherwise create a new attribute of that document;
   If the document does not exist,
   create the document and then create the attribute"
  [conn db-name collection-name doc-key attr-key attr-value]
  (if (-> conn (read-document-key db-name collection-name doc-key))
    (let [doc (get-document conn db-name collection-name doc-key)]
      (add-attribute doc attr-key attr-value)
      (update-document conn db-name collection-name doc-key doc))
    (let [doc (create-document doc-key)]
      (add-attribute doc attr-key attr-value)
      (insert-document conn db-name collection-name doc))))

(defn delete-document
  "Delete a document by its key"
  [conn db-name collection-name doc-key]
  (-> conn (get-collection db-name collection-name) (.deleteDocument doc-key)))

(defn compare-and-set-attr
  "Set the document attribute to the new value if and only if
   the old value matches the current value of the attribute,
   and returns true. If the CaS fails, it returns false."
  [conn db-name collection-name doc-key attr-key old-val new-val]
  (if (= old-val (read-document-attr conn db-name collection-name doc-key attr-key))
    (do (set-document-attr conn db-name collection-name doc-key attr-key new-val) true)
    false))
