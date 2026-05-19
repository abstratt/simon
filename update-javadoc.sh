# Credit: https://vaadin.com/blog/host-your-javadoc-s-online-in-github
set -euo pipefail
set -x

APIDOCS_DIR=target/reports/apidocs

generate_docs() {
  mvn clean
  mvn javadoc:javadoc javadoc:aggregate
  mvn com.github.ferstl:depgraph-maven-plugin:aggregate  "-Dincludes=com.abstratt.simon:*" "-Dexcludes=com.abstratt.simon:simon-test*"
  dot -Tpng target/dependency-graph.dot -o "$APIDOCS_DIR/dependencies.png"
}

verify_artifacts() {
  # Guard against silent upstream failures: if any of these are missing the
  # publish step would otherwise force-push whatever the current working
  # directory contains to gh-pages, blowing away the docs site.
  test -d "$APIDOCS_DIR" || { echo "Aborting: $APIDOCS_DIR missing — javadoc:aggregate did not produce output." >&2; exit 1; }
  test -f "$APIDOCS_DIR/index.html" || { echo "Aborting: $APIDOCS_DIR/index.html missing — Javadoc generation incomplete." >&2; exit 1; }
  test -f "$APIDOCS_DIR/dependencies.png" || { echo "Aborting: $APIDOCS_DIR/dependencies.png missing — is graphviz 'dot' installed?" >&2; exit 1; }
}

maybe_push() {
  PUSH=true
  for arg in "$@"; do
    case $arg in
      --no-push) PUSH=false ;;
    esac
  done

  if [ "$PUSH" = true ]; then
    # now push documentation to gh-pages branch
    cd "$APIDOCS_DIR"
    git init
    git remote add javadoc git@github.com:abstratt/simon.git
    git fetch --depth=1 javadoc gh-pages
    git add --all
    git commit -m sync
    git merge --allow-unrelated-histories --no-edit -s ours remotes/javadoc/gh-pages
    git push --set-upstream javadoc master:gh-pages --force
    pwd
    cd -
  fi
}

generate_docs
verify_artifacts
maybe_push "$@"
