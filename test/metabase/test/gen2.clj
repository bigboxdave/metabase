(ns metabase.test.gen2
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.test.check.generators :as gen]
            [metabase.models :refer [Activity Card Dashboard DashboardCard User Collection Pulse PulseCard Database
                                     Table Field Metric FieldValues Dimension MetricImportantField PermissionsGroup
                                     Permissions PermissionsGroupMembership DashboardCardSeries NativeQuerySnippet]]
            [metabase.test :as mt]
            [reifyhealth.specmonstah.core :as rs]
            [reifyhealth.specmonstah.spec-gen :as rsg]
            [toucan.db :as tdb]

            [java-time :as t]))



;; * items
(def id-seq (atom 0))
(s/def ::id (s/with-gen pos-int? #(gen/fmap (fn [_] (swap! id-seq inc)) (gen/return nil))))
(s/def ::engine #{:postgres :h2})
(s/def ::not-empty-string (s/and string? not-empty #(< (count %) 10)))
(s/def ::name ::not-empty-string)
(s/def ::first_name ::not-empty-string)
(s/def ::last_name ::not-empty-string)

(s/def ::database (s/keys :req-un [::id ::engine ::name]))
(s/def ::color #{"#A00000" "#FFFFFF"})
;(s/def ::email (s/and ::not-empty-string #(str/starts-with? #"a@a." %)))
(s/def ::password ::not-empty-string)
(s/def ::str? (s/or :nil nil? :string string?))
(s/def ::topic ::not-empty-string)
(s/def ::details ::not-empty-string)
(s/def ::timestamp #{(t/instant)})

(s/def ::user_id ::id)
(s/def ::group_id ::id)

(def non-blank-str?
  (fn [s]
    (not (clojure.string/blank? s))))

(def str-with-gen (s/with-gen string?
                        #(gen/such-that non-blank-str?
                                        gen/string-alpha-numeric)))

(def email-gen
  "Generator for email addresses"
  (gen/fmap
    (fn [[name host tld]]
      (str name "@" host "." tld))
    (gen/tuple gen/string-alphanumeric gen/string-alphanumeric gen/string-alphanumeric)))

(s/def ::email
  (s/with-gen
    #(re-matches #".+@.+\..+" %)
    (fn [] email-gen)))


;; * card
(s/def ::display #{:table})
(s/def ::visualization_settings string?)
(s/def ::dataset_query string?)

;; * dashboardcard_series
(s/def ::position pos-int?)

;; * dimension
(s/def ::type #{"internal"})

;; * field
(s/def ::database_type #{"VARCHAR"})
(s/def ::base_type #{:type/Text})

;; * metric
(s/def ::definition #{ {} })
(s/def ::description string?)

;; * table
(s/def ::active boolean?)

;; * native-query-snippet
(s/def ::content ::not-empty-string)
(s/def ::core-user (s/keys :req-un [::id ::first_name ::last_name ::email ::password]))
(s/def ::collection (s/keys :req-un [::id ::name ::color ]))
(s/def ::activity (s/keys :req-un [::id ::topic ::details ::timestamp]))
(s/def ::pulse (s/keys :req-un [::id ::name]))
(s/def ::permissions-group (s/keys :req-un [::id ::name]))
(s/def ::permissions-group-membership (s/keys :req-un [::user_id ::group_id]))
(s/def ::card (s/keys :req-un [::id ::display ::name ::visualization_settings ::dataset_query]))
(s/def ::dashboard_card_series (s/keys :req-un [::id ::position]))
(s/def ::dimension (s/keys :req-un [::id ::name ::type]))

(s/def ::field (s/keys :req-un [::id ::name ::base_type ::database_type ::position]))

(s/def ::metric (s/keys :req-un [::id ::name ::definition ::description]))
(s/def ::table  (s/keys :req-un [::id ::active ::name ]))

(s/def ::native-query-snippet (s/keys :req-un [::id ::name ::description ::content]))

;(gen/generate (s/gen ::activity))

;; * schema
(def schema
  {:permissions-group            {:prefix  :perm-g
                                  :spec    ::permissions-group
                                  :insert! {:model PermissionsGroup}}
   :permissions-group-membership {:prefix    :perm-g-m
                                  :spec      ::permissions-group-membership
                                  :relations {:group_id [:permissions-group :id]
                                              :user_id  [:core-user :id]}
                                  :insert!   {:model PermissionsGroupMembership}}
   :core-user                    {:prefix  :u
                                  :spec    ::core-user
                                  :insert! {:model User}}
   :activity                     {:prefix    :ac
                                  :spec      ::activity
                                  :relations {:user_id [:core-user :id]}
                                  :insert!   {:model Activity}}
   :database                     {:prefix  :db
                                  :spec    ::database
                                  :insert! {:model Database}}
   :collection                   {:prefix    :coll
                                  :spec      ::collection
                                  :insert!   {:model Collection}
                                  :relations {:personal_owner_id [:core-user :id]}}
   :pulse                        {:prefix    :pulse
                                  :spec      ::pulse
                                  :insert!   {:model Pulse}
                                  :relations {:creator_id    [:core-user :id]
                                              :collection_id [:collection :id]}}
   :card                         {:prefix    :c
                                  :spec      ::card
                                  :insert!   {:model Card}
                                  :relations {:creator_id  [:core-user :id]
                                              :database_id [:database :id]}}
   :dashboard-card-series        {:prefix  :dcs
                                  :spec    ::dashboard_card_series
                                  :insert! {:model DashboardCardSeries}}
   :dimension                    {:prefix  :dim
                                  :spec    ::dimension
                                  :insert! {:model Dimension}}
   :field                        {:prefix      :field
                                  :spec        ::field
                                  :insert!     {:model Field}
                                  :relations   {:table_id [:table :id]}
                                  :constraints {:table_id #{:uniq}}}
   :metric                       {:prefix    :metric
                                  :spec      ::metric
                                  :insert!   {:model Metric}
                                  :relations {:creator_id [:core-user :id]
                                              :table_id   [:table :id]}}
   :table                        {:prefix    :t
                                  :spec      ::table
                                  :insert!   {:model Table}
                                  :relations {:db_id [:database :id]}
                                  ;; :constraints {:name #{:uniq}} ;
                                  }
   :native-query-snippet {:prefix :nqs
                          :spec ::native-query-snippet
                          :insert! {:model NativeQuerySnippet}
                          :relations {:creator_id [:core-user :id]
                                      :collection_id [:collection :id]} ;; optional
                          }
   ;; :pulse-card { }
   ;; :pulse-channel {}
   ;; :revision {}
   ;; :segment {}
   ;; :task-history {}

   })

;; * inserters
(def mock-db (atom {}))
(defn insert*
  [{:keys [data] :as db} {:keys [ent-type spec-gen]}]
  (swap! mock-db conj [ent-type spec-gen]))

(defn insert [query]
  (reset! id-seq 0)
  (reset! mock-db [])
  (-> (rsg/ent-db-spec-gen {:schema schema} query)
      (rs/visit-ents-once :inserted-data insert*))
  nil)

;; * gen
(defn generate []
  (let [entities [Card Dashboard DashboardCard Collection Pulse Database Table Field PulseCard Metric
                  FieldValues Dimension MetricImportantField Activity]]
    (mt/with-model-cleanup entities
      (-> (rsg/ent-db-spec-gen {:schema schema} {:collection [[1]]})
          (rs/attr-map :spec-gen)))))

(defn spec-gen
  [query]
  (rsg/ent-db-spec-gen {:schema schema} query))

(def table-field-position (atom 0))
(defn adjust [sm-db {:keys [schema-opts attrs ent-type visit-val] :as visit-opts}]

  ;; some fields have to be semantically correct, or db correct. fields have position, and they do have to be unique.
  ;; In the table-field-position, for now it's just incrementing forever, without scoping by table_id (which would be
  ;; cool)
  ;;
  (cond
    (= :field ent-type)
    (assoc visit-val :position (swap! table-field-position inc))
    ;; tables have a name and a db_id. it has to be unique also
    :else
    visit-val
    ;; (swap! table-field-position update (:table_id (:spec-gen attrs)) inc)
    ;; (update-in visit-opts [:attrs :spec-gen :position] (get @table-field-position (:table_id (:spec-gen attrs))))
    ))

(defn remove-ids [_ {:keys [visit-val] :as visit-opts}]
  (dissoc visit-val :id))

(defn insert!
  [query]
  (-> (spec-gen query)
      (rs/visit-ents :spec-gen remove-ids)
      (rs/visit-ents :spec-gen adjust)
      (rs/visit-ents-once
       :insert! (fn [sm-db {:keys [schema-opts attrs] :as visit-opts}]
                  (try
                    (tdb/insert! (:model schema-opts)
                      (rsg/spec-gen-assoc-relations
                       sm-db
                       (assoc visit-opts :visit-val (:spec-gen attrs))))
                    (catch Throwable e (println e))
                    )))
      (rs/attr-map :insert!)))

;; (rs/attr-map (rsg/ent-db-spec-gen {:schema schema} {:core-user [[1]]}) :spec-gen)
