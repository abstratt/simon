---
name: docs-prefer-top-down-structure
description: "For language/syntax reference docs in this project, structure top-down (concepts → worked example → drill-down → lexical appendix), not bottom-up (lexical → constructs → bigger constructs)"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: c8d0c29c-bc68-47f1-ad3d-b98ec5c7602f
---

Structure prose docs top-down. Open with the big-picture concepts (what the
language is, what its core relationship is — e.g. "models vs. metamodels"),
then a single worked example exercising every construct introduced later
(reader anchor), then unfold the surface progressively: file shape → object
form → values → references → modifiers → composing it all into a language
definition. Push lexical details (identifier rules, grammar) to
appendix-style sections at the end.

**Why:** I wrote the original `docs/language.md` bottom-up (file structure →
lexical → object → slots → components → modifiers → defining a metamodel →
resolution → casing → example) and the user redirected explicitly: "I'd
rather have a top-down instead of a bottom up doc." Bottom-up forces readers
through low-level token rules before they see the big picture.

**How to apply:** When drafting or restructuring a `docs/*.md`, write the
section outline before any prose. Verify the outline reads
concept → example → unfold → cross-cutting → lexical-detail in that order.
A worked example near the top is the right anchor for everything that
follows. See [[docs-examples-must-match-conventions]] for the related
"examples must obey the doc's own rules" rule.
