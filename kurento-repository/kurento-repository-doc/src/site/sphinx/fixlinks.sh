#!/bin/bash
#
# Script to change links due to read-the-docs limitation in 2nd level sections

# arg1: search pattern
# arg2: replace string
# arg3: file in which replace
function changelink {
   grep "$1" $3 &> /dev/null
   if [ $? -eq 0 ]; then
     # echo "Replacing \"$1\" by \"$2\" in \"$3\""
     sed -i "s,$1,$2," $3
   fi
}

# ./$(BUILDDIR)/html/javadoc.html
changelink "#kurento-repository-internal-javadoc" "langdoc/javadoc/internal/index.html" "./../../../target/site/html/javadoc.html"
changelink "#kurento-repository-server-javadoc" "langdoc/javadoc/server/index.html" "./../../../target/site/html/javadoc.html"
changelink "#kurento-repository-client-javadoc" "langdoc/javadoc/client/index.html" "./../../../target/site/html/javadoc.html"

# ./build/html/index.html
changelink "javadoc.html#kurento-repository-internal-javadoc" "langdoc/javadoc/internal/index.html" "./../../../target/site/html/index.html"
changelink "javadoc.html#kurento-repository-server-javadoc" "langdoc/javadoc/server/index.html" "./../../../target/site/html/index.html"
changelink "javadoc.html#kurento-repository-client-javadoc" "langdoc/javadoc/client/index.html" "./../../../target/site/html/index.html"
