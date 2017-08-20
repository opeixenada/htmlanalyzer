# HTTP Analysis

Web-service for HTML pages analysis.
 
Report includes the following:

- HTML version of the document;
- page title;
- number of headings grouped by heading level;
- number of hypermedia links in the document, grouped by "internal" links to the same domain and 
"external" links to other domains;
- does the page contain a login form;
- validation for each linked resource, if it's available via HTTP(S).

## Launch

```
sbt test run
```

## Implementation

Service is written in Scala with Play framework and Bootstrap.

### HTML version

 - If doctype is `<!doctype html>` -> `HTML5`
 - otherwise get HTML version from `<!DOCTYPE HTML PUBLIC "-//W3C//DTD <HTML version>//EN">`

If HTML version can't be detected, consider is undefined.

### Login form detection

Login form is defined as a form that contains:
- a field of `text` type and name containing {`login`, `loginname`, `user`, `username`} token 
(case-ignorant);
- a field of `password` type.

Detection does not work if login field is named in an unexpected manner or if login and password
 fields are not in the same form.

### Linked resources validation

To check if linked resources are reachable we try to access them in parallel in Futures and then
 collect the results. We wait for the results for 1 minute, and if it takes longer to reach all 
 the resources we just omit this part of analysis.