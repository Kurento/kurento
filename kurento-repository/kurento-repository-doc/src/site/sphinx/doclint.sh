#!/bin/bash
# Script to disable doclint for javadoc when the java version is greater than 1.7.

if hash java; then
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    _java="$JAVA_HOME/bin/java"
else
    echo ""
fi

if [[ "$_java" ]]; then
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    if [[ "$version" > "1.7" ]]; then
        echo -Xdoclint:none
    else         
        echo ""
    fi
fi
