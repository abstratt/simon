---
name: gh-pages-md-and-html-urls
description: GitHub Pages with default Jekyll serves the .md URL raw (Content-Type text/markdown) and the parallel .html URL as a themed rendered page — link to .html for site URLs
metadata: 
  node_type: memory
  type: reference
  originSessionId: c8d0c29c-bc68-47f1-ad3d-b98ec5c7602f
---

When a markdown file lands on `gh-pages` and Jekyll is enabled (the
default), GitHub Pages exposes the same source under two URLs:

- `https://<owner>.github.io/<repo>/path/foo.md` — raw markdown,
  `Content-Type: text/markdown`. Browsers display plain text.
- `https://<owner>.github.io/<repo>/path/foo.html` — themed HTML
  rendered by Jekyll, with a `<title>` derived from the first H1 and the
  site's theme chrome.

Always link to the `.html` URL for the published docs site (the rendered
view). The `.md` URL is useful for sharing raw source but reads as plain
text in browsers. In-repo links on github.com itself should stay `.md` —
github.com renders markdown inline there.

**How to apply:** When updating a README link or a doc cross-reference
that points to the published site, use `.html`. Verified for this
project at `https://abstratt.github.io/simon/docs/language.html`
(May 2026).
