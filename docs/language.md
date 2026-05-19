# The Simon Language

Simon is a textual notation for declarative models. A Simon source file
is read against a *metamodel* — a definition of the language elements
the file is allowed to use — and produces a tree of model objects with
cross-tree links.

The same notation describes both **models** (instances of a language)
and **metamodels** (definitions of a language) — the only difference is
which metamodel is in scope. The bootstrap metamodel for declaring new
metamodels is `Simon`, which is itself self-described in `simon.simon`.

This document is organized top-down: it starts with the big picture
(models vs. metamodels), introduces a worked example so every later
section has something concrete to point at, then peels back the layers
of the language — file anatomy, object declarations, slot values,
containments and references, modifiers — before showing how those
constructs combine to define a metamodel of your own. Resolution and
casing rules come after, since they make most sense once you have a
mental model of the surface language. A grammar reference and a notes
section close out.

Examples are drawn from the example metamodels under
`example-languages/src/main/resources/com/abstratt/simon/examples/`.

---

## 1. Models and metamodels

Every Simon file is an object graph typed against one or more
metamodels. A *metamodel* declares the element kinds a file may
instantiate, what features each kind has, and how those kinds compose.
A *model* is a graph of instances of those kinds.

```
metamodel  ── defines ──►  element kinds (object types, record types, …)
                                 ▲
                                 │ instantiates
model      ── made of ──►  objects of those kinds
```

A few properties to keep in mind:

- **The notation is the same on both sides.** A metamodel is also a
  Simon file. It is itself typed against a metamodel — the bootstrap
  metamodel called `Simon` — so writing a metamodel is just writing a
  model in the `Simon` language.
- **Models can be graphs, not just trees.** Object containments form a
  tree of ownership, but cross-tree links via references let the model
  describe arbitrary directed graphs.
- **Names matter.** Almost every model element has a name. Cross-tree
  references resolve names against the model and against imported
  models.
- **Metamodels can be authored in multiple host languages.** This repo
  ships Java- and Kotlin-defined twins of the same metamodels, plus
  the Simon-source twins. The wire-format (Ecore) and runtime
  behavior are identical.

---

## 2. A worked example

Here is a small but complete model in the `UI` metamodel. Every
construct introduced later in the document appears here.

```
@language UI
@import 'colors'

(* The customer-facing demo app. *)
[root] application myApp {
    screens {
        screen login {
            children {
                button submit (label: 'Sign in' backgroundColor = #(red = 0 green = 120 blue = 215))
                link forgotPassword (label: 'Forgot password?') {
                    targetScreen: myApp.recovery
                }
            }
        }
        screen recovery {}
    }
}
```

Reading top-to-bottom:

- The `@language` and `@import` lines establish what metamodel is in
  scope and pull in another source so cross-file names resolve.
- A `(* … *)` comment becomes documentation on the element that
  follows it.
- `[root] application myApp` declares a root-level `Application`
  instance named `myApp`. The `[root]` modifier is a flag on the
  `Application` type permitting top-level use; `application` is the
  type keyword (lowercase first letter — see §10); `myApp` is the
  instance name.
- The brace block holds *components*: nested children grouped under
  containment feature names (`screens`, `children`) and the occasional
  *link* line (`targetScreen: myApp.recovery`) that points to a named
  element elsewhere in the model.
- Inside parens are *slots*: attribute values like `label: 'Sign in'`
  and the record literal `backgroundColor = #(red = 0 …)`.

The rest of this document drills into each of these constructs.

---

## 3. File anatomy

Every Simon file has three regions, in order:

```
@language NAME
@import 'sourceName'
…

(* model comment *)

rootObject1 { … }
rootObject2 { … }
…
```

1. **Header directives.** Zero or more `@language` and `@import`
   directives, mixed freely. These establish what metamodels are in
   scope and where the compiler can resolve names from.
2. **Root objects.** Zero or more object declarations at the top
   level. Each becomes an entry in the compilation result.
3. **Comments.** May appear anywhere a top-level item or component
   is expected (see §11).

### 3.1 `@language NAME`

Names a metamodel that this file's objects are typed against. Multiple
`@language` declarations are allowed; their type spaces are merged for
name resolution. `NAME` is the package name of the metamodel (e.g.
`UI`, `IM`, `Simon`), matched case-sensitively against the metamodel's
declared name.

A file with no `@language` declaration cannot define any root objects;
the compiler reports `No languages defined`.

### 3.2 `@import 'sourceName'`

Pulls in another source by name, parsed against the same languages
already in scope, so cross-file references resolve. The string is a
logical source identifier — the compiler resolves it against the
configured `SourceProvider` (typically classpath or filesystem
lookup), not a path literal.

Imports are transitive: an imported file's own `@import` directives
are followed in turn.

### 3.3 Root objects

A root object is an object declaration (see §4) whose type permits
top-level use. In the meta-metamodel that's signaled by `[root]` on
the object type; instances of non-root types can only appear nested
inside a containment block.

---

## 4. Object declarations

The object form is the universal building block. It describes one
model element — its type, its optional name, its optional flat slots,
and its optional nested components:

```
[modifier]* TypeName objectName? (slot1: v1 slot2: v2 …)? { components }?
```

Concrete example:

```
[root] application myApp (documentation: 'demo') {
    screens {
        screen login {}
    }
}
```

The parts of an object declaration:

- **Modifiers** — zero or more bracketed identifiers; see §7.
- **Type** — a (possibly qualified) identifier resolving to an
  `objectType`, `recordType`, `enumType`, or `primitiveType` in some
  language currently in scope. The type must be *instantiable* (i.e.
  not declared `[abstract]`); otherwise the compiler reports
  `Language element not instantiable`.
- **Name** — an optional identifier. Required when the type has a
  `[name]`-marked attribute and the surrounding context expects a
  named element. Sets the element's name slot.
- **Slots** — a parenthesized list of `key: value` (or `key = value`)
  pairs; see §5.
- **Components** — a brace block of further nested objects and link
  lines, organized by containment or reference feature; see §6.

A naked `foo` (just the type keyword) is a valid object with no name,
no slots, and no components — useful where defaults suffice.

---

## 5. Slot values

A *slot* assigns a value to an attribute on the object being declared.
Slots live inside the optional parens after the object name:

```
reference parent (opposite: 'children') { type: Container }
item myItem  (tags: ['red', 'green', 'blue'])
```

The grammar is `featureName keyValueSep slotValue`, where the
separator is `:` or `=` (chosen for readability — both behave
identically). `featureName` is the attribute name on the surrounding
type. The value depends on the attribute's declared type and
multiplicity.

### 5.1 Single-valued slots

For a non-multivalued attribute the slot value is a single literal of
the appropriate kind:

```
attribute name (documentation: 'the name of this thing')
button submit (label = 'Sign in')
```

A list literal supplied here is a `TypeError`.

### 5.2 Multivalued slots

For a `[multivalued]` primitive attribute the slot value must be a
list literal. Each element is parsed as the attribute's element type:

```
package Foo (builtIns: ['primitives', 'extras'])
```

A single literal supplied to a multivalued slot is a `TypeError`.
Omitting the slot is equivalent to an empty list `[]`.

### 5.3 Record-typed slots

Record types are composite attribute values without identity. They
are written inline using the `#(…)` literal:

```
button (backgroundColor = #(red = 100 green = 0 blue = 50))
```

The `#(…)` body uses the same slot grammar as object headers; only
attributes are admitted there.

### 5.4 Literal forms

The literal kinds available as slot values and inside list/record
literals:

| Kind | Form | Example |
|------|------|---------|
| String | `'…'` | `'hello'` |
| Number | digits, optional `.` | `42`, `3.14` |
| Boolean | `true` / `false` | `true` |
| Enum literal | bare identifier | `Vertical` |
| Record | `#(…)` | `#(red = 255 green = 0 blue = 0)` |
| List | `[…]` | `['a', 'b', 'c']` |

String literals use single quotes; there is no escape syntax, so
strings cannot contain a single quote or a line break. List literals
appear only as slot values for multivalued primitive attributes.

---

## 6. Containments and references

The brace block after an object header carries two kinds of nested
construct:

- **Containment blocks** — feature-named blocks whose children are
  themselves objects, becoming children of the surrounding element.
- **Links** — single-line `featureName: target` entries that pair a
  reference feature with a named element resolved elsewhere in the
  model.

Both share the same outer syntax:

```
application myApp {
    screens {              // a containment block
        screen login {}
        screen home  {}
    }
}

reference parent (opposite: 'children') { type: Container }
                                          //  ^ this brace block holds a link
```

### 6.1 Containment blocks

A containment block names a *contained* feature on the surrounding
type and lists child objects nested directly under it. Each child is
itself an object declaration following the rules in §4. Children are
added to the feature in source order; if the feature is single-valued
the block must hold at most one child.

The containment block's feature name is matched case-sensitively
against the feature names declared on the surrounding type.

### 6.2 Links

A `featureName: target` line inside a brace block names a *reference*
feature and links it to an existing named object. The target can be:

- A simple name (`screen1`) — resolved within the same model.
- A qualified name (`UI.Application`) — for cross-package references.
- A path through a containment chain (`myApp.screen2`) — for
  resolving by location in the model.

Link targets are resolved at the end of compilation, so forward
references and cross-file references work regardless of order.

### 6.3 Opposite references

A reference feature may declare its inverse — the opposite navigable
direction — via the `opposite` slot:

```
reference application (opposite: 'screens') { type: Application }
```

When two references are paired by opposite name, setting one
automatically maintains the other.

---

## 7. Modifiers

A modifier `[X]` decorates an object header to set a *flag* on the
resulting element. There are two flavors, both resolved against the
target type's attributes.

### 7.1 Boolean modifiers

`[X]` where the surrounding type has a `boolean` attribute named `X`
sets that attribute to `true`. Useful flags carry their truth-state in
their name:

```
[abstract] objectType Named { ... }      // sets Named.abstract = true
[root] objectType Package { ... }        // sets Package.root = true
[optional] attribute documentation { … } // sets documentation.optional = true
[multivalued] containment children { … } // sets children.multivalued = true
```

The match is **case-sensitive** on the attribute name (see §10).

### 7.2 Enum-typed modifiers

`[X]` may also resolve against an enum-typed attribute (one whose type
is an `enumType`). The compiler looks for an enum literal named `X`
across all enum-typed modifier attributes on the target type and sets
the attribute to that literal:

```
[name] attribute name { type: primitives.String }
[documentation] attribute documentation { type: primitives.String }
[modifier] attribute abstract_ { type: primitives.boolean }
```

These three resolve against the `role` attribute on `Attribute` (typed
`AttributeRole`, with literals `Plain`, `Name`, `Documentation`,
`Modifier`). The first letter of the modifier identifier is folded
upward for lookup, so `[name]` and `[Name]` both resolve to the `Name`
literal.

To disambiguate when multiple enum-typed attributes have a literal of
the same name, the modifier may be qualified by the enum type:

```
[OperationKind.Action] attribute kind { type: OperationKind }
```

A modifier whose identifier matches no eligible attribute is a fatal
compile error listing the modifier-eligible slot names on the type.

---

## 8. Authoring a metamodel

A metamodel is a Simon file written `@language Simon`. It declares a
`package` containing `objectType`, `recordType`, `enumType`, and
`primitiveType` elements, each with their own features. The full
meta-metamodel lives in
`example-languages/src/main/resources/com/abstratt/simon/examples/simon.simon`;
the essentials follow.

### 8.1 Packages

A `package` is the root composite of any metamodel:

```
package UI {
    objectTypes { … }
    recordTypes { … }
    enumTypes   { … }
}
```

The `package` declaration accepts these built-in slots:

- `name` — derived from the object name in source (`UI` above).
- `builtIns` — a `[multivalued]` list of named auxiliary source
  resources to be bundled with the package. When this package is in
  scope, each named resource is registered with the compiler's source
  provider, so an `@import 'name'` in a consumer file resolves to the
  bundled content. Example:
  `package Simon (builtIns: ['primitives'])`.

### 8.2 Object types

```
[abstract] objectType Named {
    attributes {
        [name] attribute name { type: primitives.String }
        [documentation] [optional] attribute documentation { type: primitives.String }
    }
}

[root] objectType Application {
    superTypes: BaseNamed
    containments {
        [multivalued] containment screens { type: Screen }
    }
}
```

- `[abstract]` — the type cannot be instantiated directly. Use only
  as a supertype.
- `[root]` — instances of this type may appear at file scope (as root
  objects). Without `[root]` a type is "internal" and can only appear
  nested inside a containment.
- `superTypes:` — a reference (or list of references) to other object
  types in scope. Inherited features cumulate.
- `attributes { … }` — primitive- or record-typed fields. Each is an
  `attribute` declaration; see §8.5.
- `containments { … }` — child elements owned by instances of this
  type. Each is a `containment` declaration.
- `references { … }` — links to elements owned elsewhere. Each is a
  `reference` declaration.

### 8.3 Record types

```
recordType Color {
    attributes {
        attribute red   { type: primitives.int }
        attribute green { type: primitives.int }
        attribute blue  { type: primitives.int }
    }
}
```

Record types are composite values without identity: instances are
written inline as `#(…)` and not referenceable by name.

### 8.4 Enum types

```
enumType PrimitiveKind {
    literals {
        enumLiteral Integer
        enumLiteral Decimal
        enumLiteral Boolean
        enumLiteral String
        enumLiteral Other
    }
}
```

Literals are bare names; an enum-typed slot value is just the literal
name (`kind: Integer`).

### 8.5 Features (attributes, containments, references)

Each feature has a name, a type, and a set of modifiers inherited
from the meta-metamodel's `Feature`:

| Modifier | Effect |
|----------|--------|
| `[required]` / `[optional]` | Lower bound is 1 / 0. `[required]` is the default for single-valued features. |
| `[multivalued]` | Upper bound is unbounded. Default upper bound is 1. |

`attribute` features admit an additional role modifier — `[name]`,
`[documentation]`, or `[modifier]` — that sets the attribute's `role`
to the corresponding enum literal. See §7.2.

`reference` features accept an `opposite` slot naming the inverse
reference on the target type; see §6.3.

The `type` slot of a feature is a *reference* (link), not an attribute
value, so it goes in the brace block rather than the parens:

```
[multivalued] containment screens (documentation: 'the screens') { type: Screen }
```

(Slot vs. link is determined by whether the feature is an attribute or
a reference — `documentation` is an attribute, `type` is a reference.)

---

## 9. Names and resolution

### 9.1 Type lookup

The first identifier in an object header is the type. Resolution:

1. Upper-case the first letter of the source identifier (so
   `objectType` and `ObjectType` both look up `ObjectType`).
2. Search each language in scope, in declaration order, for a
   classifier with that name.
3. Qualified type names (`UI.Container`) restrict the search to the
   named package.

If no language has a matching classifier, the compiler reports
`Unknown language element`. If the matching classifier is non-
instantiable, the compiler reports `Language element not instantiable`.

The case-folding only affects the first character — `UI` is not the
same as `Ui`, and `UI.application` does not match `UI.Application`.

### 9.2 Element-name lookup

Inside a `featureName: target` link, the target is resolved to a
named element. Resolution tries, in order:

1. The model being compiled, by name, considering the languages in
   scope.
2. Models being compiled in parallel (cross-file references in the
   same compilation unit).
3. Imported model resources (`@import`).

Resolution is deferred to the end of compilation, so name order
within or across files does not matter.

### 9.3 Built-in resources

A metamodel package may declare auxiliary source resources via
`builtIns: ['name', …]`. Each name is resolved against the compiler's
configured `SourceProvider` at metamodel-build time; the contents are
attached to the package and made available to any file that uses the
metamodel. This is how `@import 'primitives'` resolves to the prelude
of common primitive types when `@language Simon` is in scope.

---

## 10. Naming convention and case folding

Simon's lookup rules quietly encode a casing convention. The grammar
itself accepts any case, but the case-folding applied at lookup time
is asymmetric — it folds the first letter *upward* in two specific
positions, and not the other way. That asymmetry only pays off if the
metamodel side commits to a specific shape. That shape *is* the
convention.

### 10.1 Metamodel side (canonical names)

When you declare a metamodel — in Java, Kotlin, or Simon source — you
are expected to spell its constituents like this:

- **Classifier names** (object types, record types, enum types,
  primitive types): `TitleCase` (`Application`, `IComponent`,
  `PrimitiveKind`).
- **Enum literal names**: `TitleCase` (`Vertical`, `Integer`, `Name`).
- **Feature names** (slot keys, containment/reference feature names):
  `camelCase` (`objectTypes`, `attributes`, `screenName`).
- **Boolean modifier attribute names**: lowercase or `camelCase`
  (`abstract`, `root`, `optional`, `multivalued`).

A metamodel that ignores these — say, classifiers in all-lowercase or
modifier attributes in `TitleCase` — will appear to parse, but the
case-folding helpers below stop helping. `[abstract]` won't match an
attribute named `Abstract`, and a classifier named `objecttype`
won't be reachable from a source file written as `objectType X` (the
fold would produce `ObjectType`, not `objecttype`).

The example metamodels under `example-languages/` follow this
convention because their Java- and Kotlin-defined twins do, and the
Simon-source twins mirror them line for line.

### 10.2 Source side (case-folding shorthand)

At two specific positions in a `.simon` file the lookup folds the
first letter of the identifier upward before matching:

1. **Type keyword position** — the first identifier in an object
   header. The first letter is upper-cased before lookup
   (`StringUtils.capitalize`). So `objectType Foo` and
   `ObjectType Foo` both resolve to the metamodel's `ObjectType`
   classifier; the rest of the identifier is matched literally, so
   `OBJECTTYPE` or `objecttype` would *not* resolve.

2. **Enum-modifier reference position** — the identifier inside
   `[...]`, when it doesn't match a boolean attribute name. The
   first letter is upper-cased and looked up among the enum literals
   on the type's enum-typed modifier attributes. So `[name]` and
   `[Name]` both resolve to the `Name` enum literal.

The stylistic effect is that meta-type keywords (`objectType`,
`recordType`, `attribute`, `containment`, `reference`) and role
modifiers (`[name]`, `[documentation]`, `[modifier]`) read like
lowercase reserved words, the way `class` or `interface` look in
Java, even though the underlying classifiers and literals are
`TitleCase` in the metamodel. The convention in the `.simon` files
of this repo is to use this lowercase-first-letter form in the
keyword position consistently:

```
[root] objectType Package {
    superTypes: Named
    attributes {
        [multivalued] attribute builtIns { type: primitives.String }
    }
    containments {
        [multivalued] containment objectTypes { type: ObjectType }
    }
}
```

### 10.3 Positions that keep `TitleCase`

Three positions in any `.simon` file keep the `TitleCase` form
literally, because the lookup at those positions does *not* fold:

- **Instance names** (`Package`, `Named` above) — the names of the
  declared elements themselves. The case is preserved.
- **Type references** (`type: ObjectType`, `superTypes: Named`,
  `type: primitives.String`) — these are matched **literally** with
  no case folding, so they must spell the classifier as the metamodel
  declared it. `type: objectType` would *not* resolve.
- **Enum literal definitions and value references**
  (`enumLiteral Integer`, `kind: Integer`) — matched literally.

Boolean modifier identifiers (`[abstract]`, `[root]`, `[optional]`,
`[multivalued]`) are also matched literally — they must spell the
boolean attribute name exactly. `[Abstract]` would *not* resolve.

---

## 11. Comments

Three comment forms are recognized:

| Form | Syntax | Treatment |
|------|--------|-----------|
| Line | `// …` | Stripped by the lexer; invisible. |
| Block | `/* … */` | Stripped by the lexer; invisible. |
| Model | `(* … *)` | Retained — attached as `documentation` to the surrounding model element. |

Line and block comments are normal compiler-only comments. Model
comments are part of the model: they may appear immediately before or
inside an object, a component block, or at file scope, and the
compiler attaches each comment's text (trimmed of the `(*` / `*)`
delimiters) to the nearest following named element's `documentation`
attribute where one exists.

---

## 12. Identifiers

```
IDENT : [A-Za-z] [A-Za-z0-9_]* ;
```

Identifiers start with a letter, then admit letters, digits, and
underscore. A trailing underscore is the established escape for names
that would collide with reserved words on the host platform — e.g.
`abstract_`, `public_`. The underscore is stripped from the
model-side name (`abstract`, `public`).

Qualified identifiers use `.` as a separator: `UI.Container`,
`primitives.String`.
