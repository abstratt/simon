# Agent notes

Persistent notes for AI coding assistants working on this repository. Each
file is a self-contained note in front-matter-prefixed Markdown. Anything
in this directory is meant to be read by humans too — these are project
conventions and gotchas, not opaque machine state.

The repository root [`AGENTS.md`](../../AGENTS.md) is the auto-discovery
entry point that summarizes the most important rules and points here for
detail.

## Conventions and rules

- [Never "Closes"/"Fixes" in commit messages](feedback_commit_messages.md) —
  avoid GitHub auto-close keywords; user controls when issues close.
- [Docs: prefer top-down structure](feedback_docs_top_down_structure.md) —
  concepts → worked example → drill-down → lexical-detail appendix; never
  lexical-first.
- [Docs: examples must match stated conventions](feedback_docs_examples_match_conventions.md) —
  every code block obeys the doc's own convention claims; audit before
  publishing.
- [Docs: distinguish grammar from convention](feedback_docs_grammar_vs_convention.md) —
  asymmetric lookup folding encodes a convention even when the grammar
  accepts any case.

## Reference notes

- [GH Pages: link to .html, not .md](reference_gh_pages_md_vs_html.md) —
  Pages+Jekyll serves the `.md` URL raw and the `.html` URL rendered;
  always link the rendered URL on the published site.

## Adding a note

Drop a new `<type>_<topic>.md` file in this directory and link it from the
relevant section above. Keep front matter consistent with existing notes:

```
---
name: short-kebab-case-slug
description: one-line summary; the relevance hook for future agents
metadata:
  type: feedback | reference | project | user
---
```

Feedback and project notes should include `**Why:**` (the motivating
incident or constraint) and `**How to apply:**` (when the rule kicks in)
lines so the rule is portable to similar-but-not-identical situations.
