# Working on this repository

`AGENTS.md` at the repo root is the conventional discovery file for agentic
coding tools (Claude Code, Cursor, Aider, Continue, etc.). Treat it as the
single source of truth for project-specific conventions an AI assistant
should follow on this repository.

The detailed agent notes live in [`docs/agent-notes/`](docs/agent-notes/) —
one file per rule or fact, with front matter and a `**Why:** / **How to
apply:**` body. The list below distills them. When a rule's nuance matters,
follow the link to the full note.

## Conventions

- **Never use GitHub auto-close keywords in commit messages.** No `Closes
  #N` / `Fixes #N` / `Resolves #N`. Issue closure is the user's call.
  See [`feedback_commit_messages.md`](docs/agent-notes/feedback_commit_messages.md).

- **Documentation is structured top-down.** Concepts → worked example →
  drill-down → cross-cutting → lexical details. Not lexical-first. See
  [`feedback_docs_top_down_structure.md`](docs/agent-notes/feedback_docs_top_down_structure.md).

- **Doc examples must match the doc's own stated conventions.** If §N
  says "use lowercase first letter here," every code block must comply.
  Audit before publishing. See
  [`feedback_docs_examples_match_conventions.md`](docs/agent-notes/feedback_docs_examples_match_conventions.md).

- **Asymmetric lookup folding encodes a naming convention.** Don't claim
  "no convention" when the grammar is permissive but the runtime
  capitalizes the first letter at lookup time. See
  [`feedback_docs_grammar_vs_convention.md`](docs/agent-notes/feedback_docs_grammar_vs_convention.md).

## Reference

- **Codebase architecture**: [`docs/architecture.md`](docs/architecture.md)
  is the map of modules, the core contracts (`Metamodel`,
  `MetamodelSource`, `Backend`, `SimonCompiler`), the compilation flow,
  the self-describing bootstrap, and how-to-extend recipes. Read it before
  adding a backend, a metamodel source, or a language.

- **Published docs URLs**: link to `.html`, not `.md`. GitHub Pages with
  Jekyll serves the `.md` source raw (browsers show plain text) and the
  parallel `.html` URL as a themed rendered page. The in-repo
  `docs/*.md` link is fine for github.com (it renders markdown inline).
  See [`reference_gh_pages_md_vs_html.md`](docs/agent-notes/reference_gh_pages_md_vs_html.md).

## Updating this file

Add new conventions or facts as bullet lines that link to a detail file
in `docs/agent-notes/`. Keep this file short — it loads into every
session's context, and long-form rationale belongs in the linked notes.
