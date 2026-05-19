# Credit: https://vaadin.com/blog/host-your-javadoc-s-online-in-github
set -euo pipefail
set -x

APIDOCS_DIR=target/reports/apidocs

generate_docs() {
  mvn clean
  mvn javadoc:javadoc javadoc:aggregate
  mvn com.github.ferstl:depgraph-maven-plugin:aggregate  "-Dincludes=com.abstratt.simon:*" "-Dexcludes=com.abstratt.simon:simon-test*"
  dot -Tpng target/dependency-graph.dot -o "$APIDOCS_DIR/dependencies.png"
  # Bundle the prose docs alongside Javadoc so they render at
  # https://abstratt.github.io/simon/docs/<file>. GitHub Pages' default
  # Jekyll renders markdown automatically.
  cp -R docs "$APIDOCS_DIR/docs"
}

verify_artifacts() {
  # Guard against silent upstream failures: if any of these are missing the
  # publish step would otherwise force-push whatever the current working
  # directory contains to gh-pages, blowing away the docs site.
  test -d "$APIDOCS_DIR" || { echo "Aborting: $APIDOCS_DIR missing — javadoc:aggregate did not produce output." >&2; exit 1; }
  test -f "$APIDOCS_DIR/index.html" || { echo "Aborting: $APIDOCS_DIR/index.html missing — Javadoc generation incomplete." >&2; exit 1; }
  test -f "$APIDOCS_DIR/dependencies.png" || { echo "Aborting: $APIDOCS_DIR/dependencies.png missing — is graphviz 'dot' installed?" >&2; exit 1; }
  test -f "$APIDOCS_DIR/docs/language.md" || { echo "Aborting: $APIDOCS_DIR/docs/language.md missing — docs/ did not copy." >&2; exit 1; }
}

maybe_push() {
  PUSH=true
  for arg in "$@"; do
    case $arg in
      --no-push) PUSH=false ;;
    esac
  done

  if [ "$PUSH" = true ]; then
    # Run publish in a subshell so cwd changes, the throwaway `.git`, the
    # `javadoc` remote, and any upstream-tracking writes can't leak into
    # this repo if anything here goes sideways.
    (
      cd "$APIDOCS_DIR"
      # Defensive: if the cd silently landed us in the wrong place (e.g.
      # the parent repo root), refuse to run git commands here. Without
      # this, a `git init` + force-push would clobber the docs site.
      test -f index.html || { echo "Aborting: $PWD does not look like the Javadoc output directory." >&2; exit 1; }
      git init
      git remote add javadoc git@github.com:abstratt/simon.git
      git fetch --depth=1 javadoc gh-pages
      git add --all
      git commit -m sync
      git merge --allow-unrelated-histories --no-edit -s ours remotes/javadoc/gh-pages
      git push javadoc master:gh-pages --force
    )
  fi
}

generate_docs
verify_artifacts
maybe_push "$@"
