# Simon Architecture

This document describes the **codebase** — its modules, the contracts
they share, and how a `.simon` source becomes a model. For the **language
surface** (the notation you write in a `.simon` file), see
[`language.md`](language.md). This doc assumes you have skimmed that one.

It is organized top-down: the big picture first, then the module map,
then the three contracts that hold everything together, then a trace of
what happens during compilation, then the self-describing bootstrap.
The last and longest section — [How to extend Simon](#6-how-to-extend-simon) —
is the practical payoff: recipes for the changes people actually make.

For a critique of where this design is weak, see
[`architecture-issues.md`](architecture-issues.md).

---

## 1. Big picture

Simon is a framework for building DSLs that are **driven by a
metamodel**. You describe the shape of a language (its element kinds,
their attributes, how they nest and cross-reference) once, as a
metamodel; Simon gives you a parser, a compiler, and a model builder for
free. Every Simon-based DSL shares the *same* surface grammar — the
metamodel is what makes one file a UI definition and another an
information model.

The core is a single generic pipeline with **three pluggable swap
points**:

```
              ┌──────────────────────────────────────────────┐
   .simon  ─► │  SimonCompiler  (parse → build → resolve)      │ ─►  model
   source     └──────────────────────────────────────────────┘     objects
                   ▲                  ▲                  ▲
                   │                  │                  │
            MetamodelSource      Simon.g4 grammar       Backend
            (where the           (how syntax is         (what the model
             metamodel           parsed — fixed,         is built as —
             comes from)         shared by all DSLs)     Ecore today)
```

- **MetamodelSource** decides *where the metamodel comes from* — annotated
  Java, an Ecore `EPackage`, or a `.simon` metamodel file.
- **Backend** decides *what the compiler builds* — today, EMF/Ecore
  `EObject`s; in principle any object graph.
- The **grammar** is fixed and language-agnostic: all DSLs are parsed by
  the same ANTLR grammar, so "adding a language" never means touching the
  parser.

Simon aims to be polyglot — the metamodel can be authored in different
host languages (there is currently support for Java, Kotlin, and Simon itself), and
multiple output/wire formats / runtime are possible, though the reference implementation is Ecore.

---

## 2. Module map

The repo is a Maven reactor (`pom.xml`). Modules grouped by role, with
dependency direction pointing toward the abstractions they build on:

The first two groups are both **core abstractions** — dependency-light,
implementation-free contracts. They differ only in topic: the first defines
what model elements *are* (the type vocabulary), the second defines what
the compiler *does* (the process). `MetamodelSource` and `Backend` are
also the framework's main swap points (see [§1](#1-big-picture)).

### Core abstractions — type vocabulary (minimal deps, no binding)

| Module | What it is |
| --- | --- |
| `metamodel` | The abstract type system: `Metamodel` and its nested interfaces (`Type`, `ObjectType`, `Slot`, …). Zero dependencies. The vocabulary everything else speaks. |
| `annotation-dsl` | The `@Meta.*` annotations (`Meta.java`) used to declare a metamodel directly on Java types. Depends only on `metamodel` (for `PrimitiveKind`). |
| `gen-utils` | `Traversal` — a generic model-navigation primitive (`hop`, `then`, `search`, `bubbleUp`), tied to no particular model implementation. Orthogonal utility. |

### Core abstractions — pipeline contracts (minimal deps, no binding)

Equally abstract — each depends only on `metamodel`. These are the swap
points: implementing them is how you target a new metamodel input or a
new model output.

| Module | What it is |
| --- | --- |
| `compiler-source` | The `MetamodelSource<T>` contract (where the metamodel comes from) plus `SourceProvider` / `ContentProvider` (where source *text* comes from) and their chains. |
| `compiler-backend` | The `Backend<O, S, M>` contract (how model objects are built) and its per-operation handler interfaces. |
| `compiler` | The `SimonCompiler<T>` orchestration contract, plus `Result<T>` and `Problem`. Implementation-agnostic. |

### Metamodel binding to Ecore

| Module | What it is |
| --- | --- |
| `metamodel-ecore` | Implements the abstract `Metamodel` interfaces on top of Ecore (`EcoreType`, `EcoreObjectType`, `EcoreSlotted`, …). Two helpers worth knowing: `MetaEcoreHelper` operates at the **metamodel** level (reads/writes `@Meta` markers stored as Ecore annotations); `EcoreHelper` operates at the **instance** level (get/set names, values, documentation on `EObject`s). |

### Pipeline implementation (ANTLR)

| Module | What it is |
| --- | --- |
| `antlr-parser` | The shared ANTLR4 grammar `Simon.g4` and the generated lexer/parser. One grammar for every DSL. |
| `antlr-compiler` | The concrete `SimonCompiler` — `SimonCompilerAntlrFactory` / `SimonCompilerAntlrImpl`, and `SimonBuilder`, the parse-tree listener that drives the backend. |

### Concrete metamodel sources (implement `MetamodelSource`)

| Module | Consumes |
| --- | --- |
| `compiler-source-ecore` | An existing Ecore `EPackage` (`EPackageMetamodelSource`) or a loaded resource (`ResourceMetamodelSource`). |
| `compiler-source-annotated-java` | `@Meta`-annotated Java types (`AnnotatedJavaMetamodelSource`), via `AnnotatedJava2EcoreMapper`. |
| `compiler-source-simon` | `.simon` metamodel files (`SimonFileMetamodelSource`), via `Simon2EcoreMapper`. See [§5](#5-the-bootstrap--self-description). |

### Concrete backend (implements `Backend`)

| Module | Builds |
| --- | --- |
| `compiler-backend-ecore` | EMF `EObject`s. `EMFModelBackendFactory` → `EcoreModelBuilder`. The only backend today. |

### Examples and tests

| Module | What it is |
| --- | --- |
| `example-languages` | Reference metamodels. The languages are defined in both forms — annotated Java (`UI.java`, `IM.java`, `Simon.java`, …) and `.simon` (`ui.simon`, `im-metamodel.simon`, `simon.simon`, …); a few `.simon` files (`primitives`, `im-primitives`) are built-in primitive definitions with no Java twin. |
| `example-languages-kotlin` | Partial Kotlin twins (polyglot demonstration). |
| `test-fixtures` | `TestHelper` — pre-built metamodels (`UI_PACKAGE`, `IM_PACKAGE`, `SIMON_PACKAGES`) and `compileProject(...)` helpers shared by tests. |
| `tests` | End-to-end tests. `AbstractCompilerTests` is run twice — once against Java-defined metamodels, once against Simon-defined ones (see [§5](#5-the-bootstrap--self-description)). Sample programs live in `tests/src/test/resources/` (`ui-sample.simon`, `im-sample.simon`). |

### Scaffolding / not what it looks like

- **`annotation-processor`** is a compile-time `javax.annotation.processing`
  processor (`SimonDSLProcessor`) that today only writes trace `.txt`
  files for each `@Meta`-annotated element. **It does not build the
  metamodel.** The real Java→Ecore mapping is `AnnotatedJava2EcoreMapper`,
  run **at runtime** by `AnnotatedJavaMetamodelSource`. Do not wire new
  metamodel logic into the processor.
- `pom.xml` carries commented-out modules (`kotlin-compiler-plugin`,
  `lsp-server`, `tests-kt`) — aspirational, not built.

The module dependency diagram is also published as
[`dependencies.png`](https://abstratt.github.io/simon/dependencies.png).

---

## 3. The three core contracts

Almost everything reduces to three pluggable interfaces plus the type
system they share. If you understand these, you understand the framework.
This section is the orientation; the interfaces themselves are the
exhaustive, authoritative reference for their methods.

### 3.0 The type system — `Metamodel`

`metamodel/.../Metamodel.java` defines what a metamodel *is*, as a nest
of interfaces. The distinctions that matter:

- **Objects vs. values.** Object types (`ObjectType`) have identity and
  can own and reference other objects; value types — records
  (`RecordType`), primitives (`Primitive`), enumerations (`Enumerated`) —
  do not.
- **Atomic vs. structured.** Structured types (objects and records) are
  made of *slots*; atomic types are not.
- **Three kinds of feature.** A `Feature` is a named, typed property: a
  `Slot` holds a basic value, a `Composition` is an owned child (the
  containment tree), a `Reference` is a link to a non-owned object
  (a cross-tree edge).

The whole framework is generic over *implementations* of these
interfaces. In this repo the only implementation is the Ecore-backed one
in `metamodel-ecore` (`EcoreType` and friends), but nothing in the
pipeline depends on that implementation directly — only on the
`Metamodel` interfaces.

### 3.1 Where the metamodel comes from — `MetamodelSource<T extends Type>`

A `MetamodelSource` (in `compiler-source`) answers "what type is named
`Button` in language `UI`?" and exposes any built-in source text the
metamodel needs (e.g. `primitives`). `T` is the concrete `Type`
implementation it serves up — in this repo always an `EcoreType<…>`. The
three implementations differ only in *what they read to populate
themselves* — see the [concrete sources](#concrete-metamodel-sources-implement-metamodelsource)
in the module map.

`SourceProvider` (also in `compiler-source`) is the orthogonal concern of
locating *source text* by name — from the classpath, a URI, in memory, or
a chain of these (`SourceProviderChain`).

### 3.2 What gets built — `Backend<O, S, M>`

A `Backend` (in `compiler-backend`) is a factory of small operation
handlers, each performing one model-construction task the compiler
requests while walking the parse tree — instantiate an element, set a
slot, attach a child, link a reference, and so on. The generics: `O` and
`S` constrain which metamodel types the backend accepts; `M` is **the
model object type the backend produces**. The Ecore binding is
`Backend<EcoreObjectType, EcoreSlotted<?>, EObject>` — see
`EcoreModelBuilder` and `EMFModelBackendFactory` in
`compiler-backend-ecore`.

### 3.3 The orchestrator — `SimonCompiler<T>`

A `SimonCompiler` (in `compiler`) compiles a set of entry-point sources
into a list of `Result<T>`, where `T` is the same model type `M` the
backend produces. `SimonCompiler.Factory.create` is the wiring point:
hand it a `MetamodelSource.Factory` and a `Backend`, get a compiler. Each
`Result` carries the root model objects and any `Problem`s (each with a
severity and a category).

---

## 4. Compilation data flow (two passes)

The concrete compiler is `SimonCompilerAntlrImpl` (in `antlr-compiler`).
`compile(entryPoints, sources)`:

1. **Sets up** — builds the `MetamodelSource` and merges the metamodel's
   `builtInSources()` ahead of the user's `SourceProvider`.
2. **Parses (pass 1)** — lexes/parses each unit with the ANTLR grammar
   and walks the tree with **`SimonBuilder`**, which drives the backend
   as it goes: instantiating objects, setting slot values, and attaching
   children. References are recorded as *deferred* resolution requests.
   `@import`s discovered while parsing are followed transitively.
3. **Resolves (pass 2)** — `builder.resolve()` settles the deferred
   requests via `Backend.nameResolution()` and `Backend.linking()`, so a
   reference can point forward or across imports, not only to elements
   already built.
4. **Assembles** — attaches collected `Problem`s to each `Result<T>`.

The mapping from each grammar rule to its `SimonBuilder` action lives in
that class. `Simon.g4` also defines a small `query`/`expression`
sub-grammar for attribute-valued lookups, separate from the `program`
entry rule.

---

## 5. The bootstrap / self-description

Simon describes itself. There is a metamodel named **`Simon`** whose
element kinds are "object type", "record type", "attribute",
"containment", "reference", and so on — i.e. the `Simon` metamodel is a
metamodel *for writing metamodels*. It exists in both forms, like every
other example language: as annotated Java (`Simon.java`) and as a `.simon`
file (`simon.simon`, which is itself written in the `Simon` language).

This is what makes `compiler-source-simon` work. To turn a `.simon`
metamodel file (say `ui.simon`) into something the compiler can use as a
metamodel, `SimonFileMetamodelSource`:

1. bootstraps with the annotated-Java `Simon` metamodel as its own
   metamodel source,
2. **compiles** the `.simon` metamodel file with that — producing
   `EObject`s that are instances of the `Simon` metamodel (an object
   model *describing* the UI language),
3. hands those instances to `Simon2EcoreMapper`, which turns them into a
   real Ecore `EPackage`.

The payoff: the metamodel you get from compiling `ui.simon` behaves like
the one `AnnotatedJava2EcoreMapper.map(UI.class)` produces from `UI.java`.
The test suite checks this — `AbstractCompilerTests` is subclassed to run
the *same* tests against both the Java-defined and Simon-defined
metamodels (see `TestHelper`,
which builds `UI_PACKAGE` / `IM_PACKAGE` from Java and `SIMON_PACKAGES`
from `.simon` files via `SimonFileMetamodelSource.Factory.withBootstrapClass`).

---

## 6. How to extend Simon

The five changes people actually make. Each names the interface to
implement, the closest existing code to copy, and where it gets wired in.

### Add a new example language

Pick a host form — both produce an equivalent Ecore `EPackage`:

- **Annotated Java:** write a `@Meta.Package` interface like
  `example-languages/.../UI.java`. Use `@Meta.ObjectType` /
  `@Meta.RecordType` / `@Meta.PrimitiveType` for kinds, `@Meta.Composite`
  (with `root = true` for entry points), `@Meta.Contained` for
  compositions, `@Meta.Reference` for references, `@Meta.Attribute` for
  slots, `@Meta.Name` / `@Meta.Documentation` / `@Meta.Modifier` for
  special roles. (See the vocabulary in [§3.0](#30-the-type-system--metamodel).)
- **`.simon` file:** write a metamodel in the `Simon` language like
  `example-languages/.../ui.simon`. Refer to
  [`language.md`](language.md) for the notation.

Then register it for tests in `TestHelper` (add a `*_PACKAGE` constant
and/or an entry to the `SIMON_PACKAGES` entry-point list) and add a
sample program under `tests/src/test/resources/`.

### Add a new metamodel source

When the metamodel lives somewhere not yet supported (a database, a
remote schema, a different file format). Implement
`MetamodelSource<T>` plus its `Factory`. The contract is generic over any
`Metamodel.Type`; in practice you use `T = EcoreType<…>` because that is
the only `Type` implementation in the repo *and* what the Ecore backend
consumes — so a source feeding the existing backend must produce
`EcoreType`. (A different `T` only makes sense paired with a backend that
consumes it.) Model it on `EPackageMetamodelSource` (simplest),
`AnnotatedJavaMetamodelSource`, or `SimonFileMetamodelSource` (most
involved — it bootstraps a sub-compile). Compose several sources with
`MetamodelSourceChain`. Wire your factory into
`SimonCompiler.Factory.create` in place of the existing one.

### Add a new backend

When you want to build something other than Ecore `EObject`s (POJOs,
JSON, a graph DB). Implement `Backend<O, S, M>` and `Backend.Factory`,
choosing your model type `M`; the compiler becomes `SimonCompiler<M>` and
results are `Result<M>`. Implement each operation handler (§3.2). Model it
on `EcoreModelBuilder` / `EMFModelBackendFactory` in
`compiler-backend-ecore`. Wire your factory into
`SimonCompiler.Factory.create`.

### Add a grammar construct

When the *surface syntax* itself needs to change (rare — the grammar is
shared by every DSL, so this is language-agnostic). Edit
`antlr-parser/.../Simon.g4`, regenerate, then handle the new rule in
`SimonBuilder` by calling the appropriate `Backend` operation (see
[§4](#4-compilation-data-flow-two-passes)). Do not add language-specific
constructs here — those belong in a metamodel.

### Add a `@Meta` annotation / metamodel capability

When the metamodel needs to express something new (a new role marker, a
new kind). Add the annotation to `annotation-dsl/.../Meta.java`, teach
`metamodel-ecore`'s `MetaEcoreHelper` how to read/write the corresponding
Ecore marker, and update **both** mappers so the capability is available
from either authoring form: `AnnotatedJava2EcoreMapper`
(`compiler-source-annotated-java`) and `Simon2EcoreMapper`
(`compiler-source-simon`). **Do not** extend `annotation-processor` — it
is trace-only scaffolding (see [§2](#scaffolding--not-what-it-looks-like)).

---

## 7. Cross-cutting notes and gotchas

- **Primitives are wrapped, not bare** *(Ecore implementation)*. A
  primitive value is an `EObject` with a `__value__` feature, not a raw
  Ecore datatype — so the metamodel stays uniform. Names, documentation,
  and modifiers also live in marker-annotated features; the constants are
  in `MetaEcoreHelper`.
- **Casing is a convention, not a free-for-all.** The grammar accepts any
  case, but lookup folds the first letter — so the runtime encodes a
  naming convention even where the grammar is permissive. See
  [`agent-notes/feedback_docs_grammar_vs_convention.md`](agent-notes/feedback_docs_grammar_vs_convention.md).
- **No feature/reference redefinition.** Simon does not (yet) support
  redefining or narrowing inherited features — don't design around it.
- **`Traversal` is the navigation primitive.** Generic model walking
  (`gen-utils`) is done with `Traversal` (`hop` / `then` / `search` /
  `bubbleUp`), tied to no particular model implementation.

For project conventions (commit messages, doc structure, publishing),
see [`AGENTS.md`](../AGENTS.md) and [`agent-notes/`](agent-notes/).

---

## Appendix: module → key package → central type

| Module | Key package | Central type(s) |
| --- | --- | --- |
| `metamodel` | `com.abstratt.simon.metamodel` | `Metamodel` |
| `annotation-dsl` | `com.abstratt.simon.metamodel.dsl` | `Meta` |
| `gen-utils` | `com.abstratt.simon.genutils` | `Traversal` |
| `metamodel-ecore` | `com.abstratt.simon.metamodel.ecore` | `EcoreMetamodel`, `MetaEcoreHelper`, `EcoreHelper` |
| `compiler-source` | `com.abstratt.simon.compiler.source` | `MetamodelSource`, `SourceProvider` |
| `compiler-backend` | `com.abstratt.simon.compiler.backend` | `Backend` |
| `compiler` | `com.abstratt.simon.compiler` | `SimonCompiler`, `Result`, `Problem` |
| `antlr-parser` | `com.abstratt.simon.parser.antlr` | `Simon.g4` |
| `antlr-compiler` | `com.abstratt.simon.compiler.antlr` | `SimonCompilerAntlrImpl`, `SimonBuilder` |
| `compiler-source-ecore` | `…compiler.source.ecore` | `EPackageMetamodelSource`, `ResourceMetamodelSource` |
| `compiler-source-annotated-java` | `…compiler.source.annotated` | `AnnotatedJavaMetamodelSource`, `AnnotatedJava2EcoreMapper` |
| `compiler-source-simon` | `…compiler.source.simon` | `SimonFileMetamodelSource`, `Simon2EcoreMapper` |
| `compiler-backend-ecore` | `…compiler.backend.ecore` | `EMFModelBackendFactory`, `EcoreModelBuilder` |
| `example-languages` | `com.abstratt.simon.examples` | `UI`, `IM`, `Simon`, … |
| `test-fixtures` | `com.abstratt.simon.tests.fixtures` | `TestHelper` |
| `tests` | `com.abstratt.simon.tests` | `AbstractCompilerTests` |
