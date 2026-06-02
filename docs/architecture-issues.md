# Simon Architecture — Known Issues & Critique

A grounded critique of the codebase architecture, ordered by impact.
Companion to [`architecture.md`](architecture.md) (which describes how
things *are*); this file records where the design is weak. Findings were
verified against the source, not inferred from the docs. Each is labelled
as a genuine flaw, a code-level smell, cruft, or a defensible trade-off —
some entries are intentional trade-offs, not mistakes.

The throughline: **the system is architected for a polyglot, multi-backend
future that hasn't arrived, while the present reality is Ecore end-to-end
with stringly-typed glue.** That mismatch is the root of most of what
follows.

---

## Architectural flaws (high impact)

### 1. Speculative generality — a pluggable pipeline with one implementation of everything

The framework is built around three generic abstractions —
`MetamodelSource<T extends Type>`, `Backend<O, S, M>`, and the `Metamodel`
type system — yet there is exactly **one** `Type` implementation
(`EcoreType`), **one** `Backend` (`EcoreModelBuilder`), and every source
yields `EcoreType`. The `O`/`S`/`M`/`T` generics impose real cognitive
cost on every signature and have never been validated by a second
implementation.

The boundary is not fake — the pipeline (`compiler`, `antlr-compiler`)
genuinely has no Ecore dependency (verified: no `metamodel-ecore`/EMF
dependency or imports). So it is a *clean but unproven* abstraction, which
is the textbook YAGNI risk: the seams are probably in the wrong places and
that won't surface until a real second backend forces them to move. The
abstraction tax is paid in full; the pluggability benefit is hypothetical.

### 2. The "abstract" layer is shaped around Ecore, and Ecore leaks through it

Several core decisions are Ecore artifacts dressed as framework concepts:

- **Wrapped primitives.** A primitive value is an `EObject` with a
  `__value__` feature rather than a plain value — a modeling workaround
  that propagates throughout the system.
- **Stringly-typed conventions.** Metamodel roles and markers are string
  constants in `MetaEcoreHelper` (`"name"`, `"documentation"`,
  `"modifier"`, `"__value__"`, `"kind"`, `"rootComposite"`,
  `"simon.annotations"`), looked up by name against EMF features and
  EAnnotations. No compile-time safety, and a real collision hazard: a
  user DSL with a feature genuinely named `name` or `kind` invites
  trouble.

Tellingly, `EcoreModelBuilder` (the only `Backend`) is the only file in
the entire pipeline that needs `@SuppressWarnings("unchecked")` and
`(EClass)` casts — the abstraction does not actually insulate it.

### 3. Dual (triple) source of truth via self-definition

The `Simon` metamodel exists as both `Simon.java` and `simon.simon`; every
example language has a Java twin and a `.simon` twin (plus partial Kotlin).
They must be kept in agreement **manually**, and the agreement is checked
only **behaviorally** — the same `AbstractCompilerTests` run against both
forms. Adding one metamodel feature means editing it in two or three
places, with drift caught only if a test happens to exercise it. Elegant
as a self-hosting demonstration; a maintenance liability as an
architecture.

### 4. Hidden mutable state — `ThreadLocal` backend, monolithic stateful builder

`EcoreModelBuilder` holds per-compilation state in
`private final ThreadLocal<Resource> currentResource`
(`EcoreModelBuilder.java:25`), set and cleared around `runOperation`. That
couples `instantiation()` to a prior `runOperation()` call through an
invisible channel, leaks if cleanup is skipped, and is hostile to async or
parallel execution.

`SimonBuilder` is a single large listener doing parsing + model
construction + deferred-resolution bookkeeping (manual scope deques,
resolution-request lists). Hard to test in isolation and the natural home
for subtle two-pass bugs.

---

## Code-level smells

### 5. One concept, three names

The `Backend` is the type `Backend`, the parameter `configurationProvider`
(`SimonCompiler.java:94` and the ANTLR factory), and the field
`modelHandling`. `configurationProvider` is actively misleading — it
builds the model, it does not provide configuration. This trips up every
reader tracing the wiring.

### 6. `compiler-source` bundles two orthogonal concerns

`MetamodelSource` (the type provider) and `SourceProvider` /
`ContentProvider` (source-text location) share one module, despite being
unrelated — the architecture doc itself calls them "orthogonal."

### 7. Two error channels

Errors flow through both a collected-`Problem` model (with
`Severity`/`Category`) and thrown exceptions (`CompilerException`,
`MetamodelException`, `AbortCompilationException`). Workable, but the line
between "collect and continue" and "throw and abort" is not obviously
principled.

---

## Cruft that misleads readers

### 8. `annotation-processor` is dead weight that runs on every build

`SimonDSLProcessor` only writes trace `.txt` files, yet it is registered
via `@AutoService` and emits a `MANDATORY_WARNING` on init. Pure noise — a
new contributor reasonably assumes it builds the metamodel (it does not;
`AnnotatedJava2EcoreMapper` does, at runtime).

### 9. A dead query sub-grammar

`Simon.g4` carries a whole `query` / `expression` / comparison sub-grammar
that no hand-written code references (`query()` / `ExpressionContext` /
`comparisonOperator` are unused). Dead language surface shipped in the
grammar.

### 10. Commented-out modules in `pom.xml`

`kotlin-compiler-plugin`, `lsp-server`, and `tests-kt` sit commented out in
the reactor — aspirational stubs that blur what actually exists.

---

## Defensible trade-offs (not clearly flaws)

- **~18 Maven modules, many with a single source file.** Over-modular for
  the project's size and it multiplies build/release overhead, but it does
  hard-enforce the dependency boundaries — a deliberate choice.
- **The generics themselves.** Heavy, but they do not cause widespread
  unsafe casts (only the Ecore backend needs suppressions). The ceremony
  is the cost; it is not actively unsafe.
