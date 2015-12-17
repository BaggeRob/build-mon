(ns build-mon.test.core
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [build-mon.core :as c]))

(def succeeded-build   {:result "succeeded"})
(def in-progress-build {:result nil :status "inProgress"})
(def failed-build      {:result "failed"})

(facts "determine-background-colour"
       (c/determine-background-colour succeeded-build :anything) => :green
       (c/determine-background-colour failed-build :anything) => :red
       (c/determine-background-colour in-progress-build succeeded-build) => :yellow
       (c/determine-background-colour in-progress-build failed-build) => :orange)

(facts "determine-status-text"
       (c/determine-status-text succeeded-build) => "succeeded"
       (c/determine-status-text failed-build) => "failed"
       (c/determine-status-text in-progress-build) => "inProgress")

(let [default-refresh-interval 20]
  (tabular
    (facts "determine-refresh-interval"
           (c/determine-refresh-interval ?params) => ?expected)
    ?params               ?expected
    {"refresh" "true"}    default-refresh-interval
    {"refresh" "yes"}     default-refresh-interval
    {"refresh" "30"}      30
    {"refresh" "false"}   nil
    {"refresh" "no"}      nil
    {"refresh" "rubbish"} nil))

(fact "there are no missing favicons"
      (let [filenames-in-public-directory (map str (.list (io/file (io/resource "public"))))
            required-favicon-filenames (map c/background-colour-key->favicon-filename (keys c/background-colours))]
        filenames-in-public-directory => (contains required-favicon-filenames :in-any-order :gaps-ok)))

(facts "about generating html"
       (let [build {:result "succeeded" :buildNumber "2015.12.17.04"}
             successful-build-html (c/generate-html build succeeded-build "great commit" 20)]
         (fact "build status is displayed"
               successful-build-html => (contains "succeeded"))
         (fact "build number is displayed"
               successful-build-html => (contains "2015.12.17.04"))
         (fact "commit message is displayed"
               successful-build-html => (contains "great commit"))
         (fact "favicon filename is included in link tag"
               successful-build-html => (contains "<link rel=\"shortcut icon\" href=\"favicon_green.ico\" />"))
         (fact "body has a green background"
               successful-build-html => (contains "<body style=\"background-color:green;\">"))
         (fact "font colour is white"
               successful-build-html => (contains "<h1 style=\"color:white;font-size:400%;text-align:center;\">")))

       (fact "refresh line is generated when refresh value is passed"
             (c/generate-html succeeded-build succeeded-build "commit" 20) => (contains "<meta http-equiv=\"refresh\" content=\"20\" />")
             (c/generate-html succeeded-build succeeded-build "commit" 60) => (contains "<meta http-equiv=\"refresh\" content=\"60\" />"))
       (fact "refresh line is not generated if nil refresh value is passed"
             (c/generate-html succeeded-build succeeded-build "commit "nil) =not=> (contains "meta")))
