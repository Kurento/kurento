#!/bin/bash
#
# Script to change links due to read-the-docs limitation in 2nd level sections

# arg1: search pattern
# arg2: replace string
# arg3: file in which replace
function changelink {
   grep "$1" $3 &> /dev/null
   if [ $? -eq 0 ]; then
     echo "Replacing \"$1\" by \"$2\" in \"$3\""
     sed -i "s,$1,$2," $3
   fi
}

# ./build/html/mastering_kurento.html
changelink "#kurento-architecture" "mastering/kurento_architecture.html" "./build/html/mastering_kurento.html"
changelink "#kurento-api-reference" "mastering/kurento_API.html" "./build/html/mastering_kurento.html"
changelink "#kurento-protocol" "mastering/kurento_protocol.html" "./build/html/mastering_kurento.html"
changelink "#advanced-installation-guide" "mastering/advanced_installation_guide.html" "./build/html/mastering_kurento.html"
changelink "#kurento-development" "mastering/kurento_development.html" "./build/html/mastering_kurento.html"
changelink "#kurento-modules" "mastering/kurento_modules.html" "./build/html/mastering_kurento.html"
changelink "#media-concepts" "mastering/media_concepts.html" "./build/html/mastering_kurento.html"

# ./build/html/mastering/*.html
files=(kurento_architecture kurento_API kurento_protocol advanced_installation_guide kurento_development kurento_modules media_concepts)
for i in ${files[@]}
do
   changelink "mastering_kurento.html#kurento-architecture" "mastering/kurento_architecture.html" "./build/html/mastering/${i}.html"
   changelink "mastering_kurento.html#kurento-api-reference" "mastering/kurento_API.html" "./build/html/mastering/${i}.html"
   changelink "mastering_kurento.html#kurento-protocol" "mastering/kurento_protocol.html" "./build/html/mastering/${i}.html"
   changelink "mastering_kurento.html#advanced-installation-guide" "mastering/advanced_installation_guide.html" "./build/html/mastering/${i}.html"
   changelink "mastering_kurento.html#kurento-development" "mastering/kurento_development.html" "./build/html/mastering/${i}.html"
   changelink "mastering_kurento.html#kurento-modules" "mastering/kurento_modules.html" "./build/html/mastering/${i}.html"
   changelink "mastering_kurento.html#media-concepts" "mastering/media_concepts.html" "./build/html/mastering/${i}.html"
done
