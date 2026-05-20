---
name: docs-distinguish-grammar-from-convention
description: "When grammar permits any case but lookup rules apply asymmetric folding, the folding encodes a convention — don't write \"no convention\" just because the parser is permissive"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: c8d0c29c-bc68-47f1-ad3d-b98ec5c7602f
---

Asymmetric runtime lookup rules encode a naming convention even when the
grammar is permissive. Folding a first letter *upward* before lookup (and
not the other way) only pays off if the canonical name is in the
upper-folded shape. That asymmetry is the convention. Don't claim "the
language imposes no convention" just because the lexer accepts any case.

**Why:** I rewrote the Simon casing section to say "Simon imposes no naming
convention" — meaning to credit the metamodel author for the names — and
the user pushed back: "why do you think there is no casing convention?"
The case-folding in `SimonBuilder` (`StringUtils.capitalize` on the
type-keyword position, first-letter folding on enum-modifier refs) implies
a TitleCase classifier convention even though the grammar is permissive.
The convention lives in the lookup machinery, not the grammar.

**How to apply:** Before writing "no convention" about a language with
runtime lookup helpers, check whether the lookup is symmetric (no folding,
truly convention-agnostic) or asymmetric (folds one way → presumes the
canonical form is in the other shape). Asymmetric folding ⇒ convention.
Frame the convention as two-sided: metamodel-side canonical names, and
source-side shorthand the folding enables.
