# Credit: https://vaadin.com/blog/host-your-javadoc-s-online-in-github
set -x 
mvn clean
mvn javadoc:javadoc javadoc:aggregate
mvn com.github.ferstl:depgraph-maven-plugin:aggregate  "-Dincludes=com.abstratt.simon:*" "-Dexcludes=com.abstratt.simon:simon-test*"
dot -Tpng target/dependency-graph.dot -o target/reports/apidocs/dependencies.png
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

