# The Simon Language

Simon is a textual notation for declarative models. A Simon source file
is read against a *metamodel* — a definition of the language elements
the file is allowed to use — and produces a tree of model objects.

The same notation describes both **models** (instances of a language)
and **metamodels** (definitions of a language) — the only difference is
which language is in scope. The bootstrap metamodel for declaring new
metamodels is `Simon`, which is itself self-described in `simon.simon`.

This document covers the surface syntax and the runtime semantics of
each construct. Examples are drawn from the example metamodels under
`example-languages/src/main/resources/com/abstratt/simon/examples/`.

---

## 1. File structure

```
@language UI
@import 'primitives'

(* a model comment *)

Application myApp {
    screens {
        Screen login {}
        Screen home  {}
    }
}
```

Every source file has three regions, in order:

1. **Declarations** — one or more `@language` and `@import` directives
   that establish what metamodels are in scope and what other sources
   the compiler can resolve names against.
2. **Root objects** — zero or more object declarations at the top
   level. Each root object becomes an entry in the result.
3. **Comments** may appear anywhere a top-level item or component is
   expected.

### 1.1 `@language NAME`

Names a metamodel that this file's objects are typed against. Multiple
`@language` declarations are allowed; their type spaces are merged for
name resolution. `NAME` is the package name of the metamodel (e.g.
`UI`, `IM`, `Simon`), matched case-sensitively against the metamodel's
declared name.

A file with no `@language` declaration cannot define any root objects;
the compiler reports `No languages defined`.

### 1.2 `@import 'sourceName'`

Pulls in another source by name, parsed against the same languages
already in scope, so cross-file references resolve. The string is a
logical source identifier — the compiler resolves it against the
configured `SourceProvider` (typically classpath or filesystem lookup),
not a path literal.

Imports are transitive: an imported file's own `@import` directives are
followed in turn.

---

## 2. Lexical elements

### 2.1 Identifiers

```
IDENT : [A-Za-z] [A-Za-z0-9_]* ;
```

Identifiers start with a letter, then admit letters, digits, and
underscore. A trailing underscore is the established escape for names
that would collide with reserved words on the host platform — e.g.
`abstract_`, `public_`. The underscore is stripped from the model-side
name (`abstract`, `public`).

Qualified identifiers use `.` as a separator: `UI.Container`,
`primitives.String`.

### 2.2 Literals

| Kind | Form | Example |
|------|------|---------|
| String | `'…'` | `'hello'` |
| Number | digits, optional `.` | `42`, `3.14` |
| Boolean | `true` / `false` | `true` |
| Enum literal | bare identifier | `Vertical` |
| Record | `#(…)` | `#(red = 255 green = 0 blue = 0)` |
| List | `[…]` | `['a', 'b', 'c']` |

String literals use single quotes; embedded double quotes are taken
literally. There is no escape syntax — strings cannot contain a single
quote or a line break.

List literals appear only as slot values for multivalued attributes.
An empty list `[]` is legal and means "no values."

Record literals carry the same `key: value` slots used elsewhere; see
§4.

### 2.3 Comments

| Form | Syntax | Treatment |
|------|--------|-----------|
| Line | `// …` | Stripped by the lexer; invisible. |
| Block | `/* … */` | Stripped by the lexer; invisible. |
| Model | `(* … *)` | Retained — attached as `documentation` to the surrounding model element. |

Model comments may appear immediately before or inside an object, a
component block, or at file scope. The compiler attaches each model
comment's text (trimmed of the `(*` / `*)` delimiters) to the nearest
following named element's `documentation` attribute, where one exists.

---

## 3. Objects

The object form is the core declarative construct. It describes one
model element — its type, optional name, optional flat slots, and
optional nested components.

```
[modifier]* TypeName objectName? (slot1: v1 slot2: v2 …)? { components }?
```

Example (using the UI metamodel):

```
[root] Application myApp (documentation: 'demo') {
    screens {
        Screen login {}
    }
}
```

Each part:

- **Modifiers** — zero or more bracketed identifiers; see §6.
- **Type** — a (possibly qualified) identifier resolving to an
  `objectType`, `recordType`, `enumType`, or `primitiveType` in some
  language currently in scope. The type must be *instantiable* (i.e.
  not declared `[abstract]`); otherwise the compiler reports
  `Language element not instantiable`.
- **Name** — an optional identifier. Required when the type has a
  `[name]`-marked attribute and the surrounding context expects a
  named element. Sets the element's name slot.
- **Slots** — a parenthesized list of `key: value` (or `key = value`)
  pairs; see §4.
- **Components** — a brace block of further nested objects organized
  by containment feature; see §5.

A naked `Foo` is a valid object with no name, no slots, and no
components — useful where defaults suffice.

---

## 4. Slots

A *slot* assigns a value to an attribute (or, for record types, a
containment). Slots live inside the optional parens after the object
name:

```
Reference parent (opposite: 'children') { type: Container }
Item myItem  (tags: ['red', 'green', 'blue'])
```

The grammar is `featureName keyValueSep slotValue`, where the
separator is `:` or `=` (chosen for readability — both behave
identically). `featureName` is the attribute name on the surrounding
type.

### 4.1 Single-valued slots

For a non-multivalued attribute, the slot value is a single literal of
the appropriate kind:

```
Attribute name (documentation: 'the name of this thing')
Color (red = 100 blue = 50)
```

A list literal supplied here is a `TypeError`.

### 4.2 Multivalued slots

For a `[multivalued]` primitive attribute, the slot value must be a
list literal. Each element is parsed as the attribute's element type:

```
Package Foo (builtIns: ['primitives', 'extras'])
```

A single literal supplied to a multivalued slot is a `TypeError`.
Omitting the slot is equivalent to an empty list.

### 4.3 Record-typed slots

Record types are composite attribute values without identity. They are
written inline using the `#(…)` literal:

```
Button (backgroundColor = #(red = 100 green = 0 blue = 50))
```

The `#(…)` body uses the same slot grammar as object headers; only
attributes are admitted there.

---

## 5. Components and links

The brace block after an object header carries *containments*
(child objects owned by this one) and *references* (links to other
objects, owned elsewhere). Both are expressed inside a feature-named
block:

```
Application myApp {
    screens {              // a containment block
        Screen login {}
        Screen home  {}
    }
}

Reference parent (opposite: 'children') { type: Container }
                                          //  ^ this brace block holds links
```

### 5.1 Containments

A containment block names a *contained* feature on the surrounding
type and lists child objects nested directly under it. Each child is
itself an object declaration following the rules in §3. Children are
added to the feature in source order; if the feature is single-valued
the block must hold at most one child.

The containment block's feature name is matched case-sensitively
against the feature names declared on the surrounding type.

### 5.2 Links

A `featureName: target` line inside a brace block names a *reference*
feature and links it to an existing named object (resolved by name
within the languages in scope). The target can be:

- A simple name (`screen1`) — resolved within the same package.
- A qualified name (`UI.Application`) — for cross-package references.
- A path through a containment chain (`myApp.screen2`) — for
  resolving by location in the model.

Link targets are resolved at the end of compilation, so forward
references and cross-file references work in either order.

### 5.3 Opposite references

A reference may declare its opposite — the inverse navigable
relationship — via the `opposite` slot:

```
Reference application (opposite: 'screens') { type: Application }
```

When two references are paired by opposite name, setting one
automatically maintains the other.

---

## 6. Modifiers

A modifier `[X]` decorates an object header to set a *flag* on the
resulting element. There are two flavors, both resolved against the
target type's attributes:

### 6.1 Boolean modifiers

`[X]` where the surrounding type has a `boolean` attribute named `X`
sets that attribute to `true`. Useful flags carry their truth-state in
their name:

```
[abstract] objectType Named { ... }      // sets Named.abstract = true
[root] objectType Package { ... }        // sets Package.root = true
[optional] attribute documentation { … } // sets documentation.optional = true
[multivalued] containment children { … } // sets children.multivalued = true
```

The match is **case-sensitive** on the attribute name.

### 6.2 Enum-typed modifiers

`[X]` may also resolve against an enum-typed attribute (one whose type
is an `enumType`). The compiler looks for an enum literal named `X`
across all enum-typed modifier attributes on the target type, and
sets the attribute to that literal:

```
[name] attribute name { type: primitives.String }
[documentation] attribute documentation { type: primitives.String }
[modifier] attribute abstract_ { type: primitives.boolean }
```

These resolve against the `role` attribute on `Attribute` (typed
`AttributeRole`, with literals `Plain`, `Name`, `Documentation`,
`Modifier`). The match is **case-insensitive on the first letter** —
`[name]` and `[Name]` both resolve to the `Name` literal. The
remainder is matched case-sensitively, so `[NAME]` does not resolve.

To disambiguate when multiple enum-typed attributes have a literal of
the same name, the modifier may be qualified by the enum type:

```
[OperationKind.Action] attribute kind { type: OperationKind }
```

A modifier whose identifier matches no eligible attribute is a fatal
compile error listing the modifier-eligible slot names on the type.

---

## 7. Defining a metamodel

Metamodels are themselves Simon files, written `@language Simon`. They
declare `package` objects containing `objectType`, `recordType`,
`enumType`, and `primitiveType` elements. The full meta-metamodel is
in `example-languages/.../simon.simon`; the essential constructs are
below.

### 7.1 Packages

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
  bundled content. Example: `Package Simon (builtIns: ['primitives'])`.

### 7.2 Object types

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
  `attribute` declaration; see §7.5.
- `containments { … }` — child elements owned by instances of this
  type. Each is a `containment` declaration.
- `references { … }` — links to elements owned elsewhere. Each is a
  `reference` declaration.

### 7.3 Record types

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

### 7.4 Enum types

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

### 7.5 Features (attributes, containments, references)

Each feature has a name, a type, and a set of modifiers. Modifier
attributes inherited from the meta-metamodel's `Feature`:

| Modifier | Effect |
|----------|--------|
| `[required]` / `[optional]` | Lower bound is 1 / 0. (`[required]` is the default for single-valued features.) |
| `[multivalued]` | Upper bound is unbounded. (Default upper bound is 1.) |

`attribute` features admit an additional role modifier — `[name]`,
`[documentation]`, or `[modifier]` — that sets the attribute's `role`
to the corresponding enum literal. See §6.2.

`reference` features accept an `opposite` slot naming the inverse
reference on the target type; see §5.3.

The `type` slot of a feature is a *reference* (link), not a slot
value, so it goes in the brace block rather than the parens:

```
[multivalued] containment screens (documentation: 'the screens') { type: Screen }
```

(Slot vs. link is determined by whether the feature is an attribute or
a reference — `documentation` is an attribute, `type` is a reference.)

---

## 8. Names and resolution

### 8.1 Type lookup

The first identifier in an object header is the type. Resolution:

1. Build a candidate name by upper-casing the first letter of the
   source identifier (so `objectType` and `ObjectType` both look up
   `ObjectType`).
2. Search each language in scope, in declaration order, for a
   classifier with that name.
3. Qualified type names (`UI.Container`) restrict the search to the
   named package.

If no language has a matching classifier, the compiler reports
`Unknown language element`. If the matching classifier is non-
instantiable, the compiler reports `Language element not instantiable`.

The case-folding only affects the first character — `UI` is not the
same as `Ui`, and `UI.application` does not match `UI.Application`.

### 8.2 Element-name lookup

Inside a `featureName: target` link, the target is resolved to a
named element. Resolution tries, in order:

1. The model being compiled, by name, considering the languages in
   scope.
2. Models being compiled in parallel (cross-file references in the
   same compilation unit).
3. Imported model resources (`@import`).

Resolution is deferred to the end of compilation, so name order
within or across files does not matter.

### 8.3 Built-in resources

A metamodel package may declare auxiliary source resources via
`builtIns: ['name', …]`. Each name is resolved against the compiler's
configured `SourceProvider` at metamodel-build time; the contents are
attached to the package and made available to any file that uses the
metamodel. This is how `@import 'primitives'` resolves to the
prelude of common primitive types when `@language Simon` is in scope.

---

## 9. Names and case folding

Simon itself imposes **no** naming convention. The case of every
identifier you write in a `.simon` file — type keyword, feature name,
enum literal, etc. — is just the case the metamodel author chose when
declaring that element. What you see in the example metamodels under
`example-languages/` reflects how those metamodels were defined: the
Java- and Kotlin-defined twins follow Java/Kotlin naming conventions,
and the Simon-source twins mirror them line for line.

That convention, as it filters into the source you read, looks like:

- **Classifier names** are `TitleCase` (e.g. `Application`,
  `IComponent`, `PrimitiveKind`) — the Java/Kotlin class-name
  convention.
- **Feature names** are `camelCase` (e.g. `objectTypes`, `attributes`,
  `screenName`) — the Java method-name convention.
- **Enum literal names** are `TitleCase` (e.g. `Vertical`, `Integer`,
  `Name`) — the Java enum-constant convention used in the metamodels
  (Kotlin's convention is more often `SCREAMING_SNAKE_CASE`, but the
  examples follow the Java shape).
- **Boolean modifier attributes** are `camelCase` (e.g. `abstract`,
  `root`, `optional`, `multivalued`) — like any boolean field name.

A different metamodel could choose other conventions and Simon source
would faithfully use them. Identifiers are matched against the
metamodel **case-sensitively**, with two ergonomic case-folding
helpers:

1. **Type keyword position** — the first letter of the type
   identifier in an object header is upper-cased before lookup
   (`StringUtils.capitalize`). So if the metamodel declares
   `ObjectType`, source files can refer to it as either `ObjectType`
   or `objectType` and get the same classifier. The rest of the
   identifier is matched literally (`OBJECTTYPE` would *not* resolve).

2. **Enum-modifier reference position** — the identifier inside
   `[...]` is first matched literally against any boolean-attribute
   name, then (if no boolean matched) the first letter is upper-cased
   and looked up among the enum literals on the type's enum-typed
   modifier attributes. So `[name]` and `[Name]` both resolve to the
   `Name` enum literal.

Both helpers exist so the meta-type-keyword position can read
naturally (lowercase, like `class` or `interface` in Java) even
though the underlying classifier is named with a `TitleCase`
identifier inherited from Java/Kotlin. That stylistic lowercase you
see in `.simon` files (`objectType Foo`, `[modifier] attribute bar`)
is not a separate naming rule — it's the case-folding shorthand for
the same classifier names the metamodel declares.

Type *references* (`type: X`, `superTypes: Y`) are matched literally
with no case folding, so they must spell the classifier as the
metamodel declared it (`type: ObjectType`, not `type: objectType`).

---

## 10. End-to-end example

A small UI model exercising every construct:

```
@language UI
@import 'colors'

(* The customer-facing demo app. *)
[root] Application myApp {
    screens {
        Screen login {
            children {
                Button submit (label: 'Sign in' backgroundColor = #(red = 0 green = 120 blue = 215))
                Link forgotPassword (label: 'Forgot password?') {
                    targetScreen: myApp.recovery
                }
            }
        }
        Screen recovery {}
    }
}
```

What that says, semantically:

- `Application myApp` is a root composite (`[root]` on `Application`
  in the UI metamodel), with `screens` as a multivalued containment.
- Each `Screen` contains a list of UI components (multivalued
  `children` from the `Container` supertype).
- `Button submit (…)` sets the `label` attribute (string) and
  `backgroundColor` attribute (a record literal of type `Color`).
- `Link forgotPassword (…) { … }` carries one attribute slot and one
  reference link; `targetScreen: myApp.recovery` is resolved by
  name path within the model.

The compiler produces a tree of model objects, one root per `@language
UI` file, validated against the UI metamodel.
