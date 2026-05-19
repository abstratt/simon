---
name: docs-examples-must-match-conventions
description: Every code example in a doc must obey the conventions that doc itself states — audit examples against the convention sections before publishing
metadata: 
  node_type: memory
  type: feedback
  originSessionId: c8d0c29c-bc68-47f1-ad3d-b98ec5c7602f
---

When a doc states a convention ("in position X the convention is camelCase"
/ "TitleCase" / "lowercase first letter"), go back through every code block
and verify position X complies. Examples that violate the doc's own rules
erode trust faster than missing examples would.

**Why:** I shipped a Simon language doc whose §9 said the type-keyword
position should be lowercase first letter (`objectType`, `application`,
`button`) — but the worked examples elsewhere still had `Application myApp`,
`Screen login`, `Button …`, `Reference parent`, `Package Foo`. The user
called it out: "The examples do not comply with the casing convention."
A quick audit pass would have caught it.

**How to apply:** After writing or editing a doc that establishes
conventions, scan every fenced code block for patterns that should match the
convention. For the Simon doc specifically, the conventions to audit are:
type-keyword position (lowercase first letter — `objectType`, `application`),
boolean modifiers (lowercase — `[abstract]`, `[multivalued]`), role
modifiers (lowercase via the case-folding — `[name]`, `[modifier]`),
instance names / type refs / enum literals (TitleCase preserved).
