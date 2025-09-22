(ns s-exp.router-test
  (:require
   [clojure.test :as test :refer [are deftest]]
   [s-exp.legba.router :as router]))

(deftest test-basics
  (let [routes {[:get "/"] :get-index
                [:get "/map"] {:something :else}
                [:get "/login"] :get-login
                [:post "/login"] :post-login
                [:get "/article/{id}"] :get-article
                [:get "/article/{id}/update"] :get-article-any-update
                [:get "/{id}"] :get-any
                [:get "/*"] :get-all}
        matcher (router/make-matcher routes)]
    (are [p key params] (= [key params] (router/match matcher {:request-method (first p)
                                                               :uri (second p)}))
      [:get "/"] :get-index {}
      [:get "/map"] {:something :else} {}
      [:get "/login"] :get-login {}
      [:post "/login"] :post-login {}
      [:get "/article/123"] :get-article {:id "123"}
      [:get "/article"] :get-any {:id "article"}
      [:get "/article/123/update"] :get-article-any-update {:id "123"}
      [:get "/any"] :get-any {:id "any"}
      [:get "/any/other"] :get-all {:* "any/other"})))

(deftest test-wildcards
  (let [routes {[:get "/*"] :a
                [:get "/x/*"] :b}
        matcher (router/make-matcher routes)]
    (are [p key params] (= [key params] (router/match matcher {:request-method (first p)
                                                               :uri (second p)}))
      [:get "/"] :a {}
      [:get "/x"] :b {}
      [:get "/x/y"] :b {:* "y"})))

(deftest test-edge-cases
  (let [routes {[:get "/a/{id}/b"] :ab
                [:get "/*"] :all}
        matcher (router/make-matcher routes)]
    (are [p key params] (= [key params] (router/match matcher {:request-method (first p)
                                                               :uri (second p)}))
      [:get "/a/{id}/b"] :ab {:id "{id}"})))
