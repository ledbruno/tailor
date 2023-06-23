# tailor

A Clojure library that helps you create AI prompts for code automation tasks

## Concept

### Scraper
Prompts for code automation rely on code snippets and examples in order to respond completions properly.

**Scrapers** are functions that cuts pieces of code from a single or multiple files, producing code snippets. 

**E.g:** my prompt goal is to create automated tests for a function.

```
Context: You are a code generator tool, that outputs clojure unit tests
Test Code:
```clojure
(defn myfn []
;some code.......
)
```
```
Generate unit tests for the code mentioned above.
Tests:
```

The code snippet labeled as "Test Code" comes from a **scrapping** on a specific file on my project.

### Prompt factory?
