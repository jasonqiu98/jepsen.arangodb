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

(defn get-document
  "get arangodb document (type: BaseDocument) by key"
  [conn db-name collection-name doc-key]
  ; doc = collection.getDocument(key, BaseDocument.class)
  (-> conn (get-collection db-name collection-name) (.getDocument doc-key (.getClass (new BaseDocument)))))

; single operation, by Java Driver
(defn read-attr
  "Read one document attribute from an ArangoDB collection."
  [conn db-name collection-name doc-key attr-key]
  (try
    (-> conn (get-document db-name collection-name doc-key) (.getAttribute attr-key))
    (catch java.lang.NullPointerException e nil)))

;; transactional ensured by AQL query
(defn write-attr
  "Update an attribute of a document if it exists,
   otherwise create a new attribute of that document;
   If the document does not exist,
   create the document and then create the attribute"
  [conn db-name collection-name doc-key attr-key attr-value]
  ; the doc-key should be a string quoted with \"\" (handled before passing into this function)
  ; e.g. INSERT {_key: "1", val: 4} INTO example OPTIONS {overwriteMode: "update"}
  (let [query (str "INSERT {_key: " doc-key ", " attr-key ": " attr-value "} INTO " collection-name " OPTIONS {overwriteMode: \"update\"}")]
    (-> conn (get-db db-name) (.query query nil))))

;; transactional ensured by AQL query
(defn cas-attr
  "Set the document attribute to the new value if and only if
   the old value matches the current value of the attribute,
   and returns true. If the CaS fails, it returns false."
  [conn db-name collection-name doc-key attr-key old-val new-val]
  ; e.g. FOR d IN example FILTER d._key == "1" AND d.val == 4 UPDATE d WITH {val: 5} IN example RETURN true
  ; [true] for success / [] for failure
  (let [query (str "FOR d IN " collection-name " FILTER d._key == " doc-key " AND d." attr-key " == " old-val " UPDATE d WITH {" attr-key ": " new-val "} IN " collection-name " RETURN true")]
    (-> conn (get-db db-name) (.query query Boolean) (.hasNext))))
