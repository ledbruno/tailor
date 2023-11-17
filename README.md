# tailor

A Clojure library that helps you shear/cut clojure source code for automation tasks

## Shear
The main objective of Tailor is to Shear source code starting from a top level form. Form that initial form, it goes down on every
other form used (dependency or :var-usage) that is available on the provided classpath.

The **Shearing** expected result is valid/evaluable clojure source code, following this rules/limitations:

- If a fn A depends on fn B, fn B should be declared firt
- If a fn A refers to fn B with an alias, it should be able to build the require form properly.
- Code from different namespaces will be declared on its specific namespace, but outputed as a single string.
- Can be evaluated on a clean repl if only points to clojure.core and available source code (a.k.a: classpath)

## Use cases 

It enables other libs/engineers to build:
- Automated refactoring tools that could help merge source code from multiple projects/modules. 
- AI prompts with contextualized source code, enabling code suggestion or code automation tasks to be executed with better results
