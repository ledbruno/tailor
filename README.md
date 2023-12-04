# tailor

A Clojure library that helps you shear/cut clojure source code for automation tasks

## Shear
The main objective of Tailor is to Shear source code starting from a top level form. From that initial form, it goes down on every
other "usage" that is available on the provided classpath.

The **Shearing** expected result is valid/evaluable clojure source code, following this rules/limitations:

- If a fn A depends on fn B, fn B should be declared first
- If a fn A refers to fn B with an alias, it should be able to build the require form properly.
- Code from different namespaces will be declared on its specific namespace, but outputed as a single string.
- Can be evaluated on a clean repl if only points to clojure.core and available source code (a.k.a: classpath)

## How to use
```
(deep-shear 'my-ns/entrypoint-fn ["./my-project/src/"])
```
Supose you have  'my-ns/entrypoint function, that calls a external lib and also a inner source function

```
(ns my-ns (:require [my-inner :as inner]
                    [external.lib :as external))

(defn entrypoint-fn[]
 (external/call)
 (inner/my-fn)
)

(def used-by-other "foo")
```

```
(ns my-inner)
(defn my-fn []
 (println "zambas")
)
```

The result of deep shear will follow ```entrypoint-fn``` path, shearing it and also ```'my-inner/my-fn```.

```
(ns my-ns (:require [my-inner :as inner]))

(defn entrypoint-fn[]
 (external-lib/call)
 (inner/my-fn)
)
(ns my-inner)
(defn my-fn []
  (println "zambas")
)
```

## Use cases 
It enables other libs/engineers to build:
- Automated refactoring tools that could help merge source code from multiple projects/modules. 
- AI prompts with contextualized source code, enabling code suggestion or code automation tasks to be executed with better results
- And others! Feel free to add other ideas!
