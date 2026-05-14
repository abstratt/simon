# Credit: https://vaadin.com/blog/host-your-javadoc-s-online-in-github
set -x

generate_docs() {
  mvn clean
  mvn javadoc:javadoc javadoc:aggregate
  mvn com.github.ferstl:depgraph-maven-plugin:aggregate  "-Dincludes=com.abstratt.simon:*" "-Dexcludes=com.abstratt.simon:simon-test*"
  dot -Tpng target/dependency-graph.dot -o target/reports/apidocs/dependencies.png
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
    cd target/reports/apidocs/
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
maybe_push "$@"

