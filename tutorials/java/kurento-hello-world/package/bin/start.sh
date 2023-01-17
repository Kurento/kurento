#!/bin/sh

DIRNAME=$(dirname "$0")
GREP="grep"

DEMO_PORT=${demo.port}

JAVA_OPTS="-Dserver.port=$DEMO_PORT -Dapp.server.url=http://127.0.0.1:$DEMO_PORT/"
JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"

# OS specific support (must be 'true' or 'false').
cygwin=false;
darwin=false;
linux=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;

    Darwin*)
        darwin=true
        ;;

    Linux)
        linux=true
        ;;
esac

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

if [ "$PRESERVE_JAVA_OPTS" != "true" ]; then
    # Check for -d32/-d64 in JAVA_OPTS
    JVM_D64_OPTION=`echo $JAVA_OPTS | $GREP "\-d64"`
    JVM_D32_OPTION=`echo $JAVA_OPTS | $GREP "\-d32"`

    # Check If server or client is specified
    SERVER_SET=`echo $JAVA_OPTS | $GREP "\-server"`
    CLIENT_SET=`echo $JAVA_OPTS | $GREP "\-client"`

    if [ "x$JVM_D32_OPTION" != "x" ]; then
        JVM_OPTVERSION="-d32"
    elif [ "x$JVM_D64_OPTION" != "x" ]; then
        JVM_OPTVERSION="-d64"
    elif $darwin && [ "x$SERVER_SET" = "x" ]; then
        # Use 32-bit on Mac, unless server has been specified or the user opts are incompatible
        "$JAVA" -d32 $JAVA_OPTS -version > /dev/null 2>&1 && PREPEND_JAVA_OPTS="-d32" && JVM_OPTVERSION="-d32"
    fi

    CLIENT_VM=false
    if [ "x$CLIENT_SET" != "x" ]; then
        CLIENT_VM=true
    elif [ "x$SERVER_SET" = "x" ]; then
        if $darwin && [ "$JVM_OPTVERSION" = "-d32" ]; then
            # Prefer client for Macs, since they are primarily used for development
            CLIENT_VM=true
            PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -client"
        else
            PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -server"
        fi
    fi

    if [ $CLIENT_VM = false ]; then
        NO_COMPRESSED_OOPS=`echo $JAVA_OPTS | $GREP "\-XX:\-UseCompressedOops"`
        if [ "x$NO_COMPRESSED_OOPS" = "x" ]; then
            "$JAVA" $JVM_OPTVERSION -server -XX:+UseCompressedOops -version >/dev/null 2>&1 && PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -XX:+UseCompressedOops"
        fi

        NO_TIERED_COMPILATION=`echo $JAVA_OPTS | $GREP "\-XX:\-TieredCompilation"`
        if [ "x$NO_TIERED_COMPILATION" = "x" ]; then
            "$JAVA" $JVM_OPTVERSION -server -XX:+TieredCompilation -version >/dev/null 2>&1 && PREPEND_JAVA_OPTS="$PREPEND_JAVA_OPTS -XX:+TieredCompilation"
        fi
    fi

    JAVA_OPTS="$PREPEND_JAVA_OPTS $JAVA_OPTS"
fi

# Find out installation type
DEMO_HOME=$(cd $DIRNAME/..;pwd)
DEMO_BINARY=$DEMO_HOME/lib/${project.artifactId}.jar
# DEMO_CONFIG=$DEMO_HOME/config/${project.artifactId}.conf.json
if [ ! -f $DEMO_BINARY ]; then
    DEMO_HOME=/var/lib/kurento
    DEMO_BINARY=$DEMO_HOME/${project.artifactId}.jar
    #    DEMO_CONFIG="/etc/kurento/${project.artifactId}.conf.json"
    DEMO_OPTS="-DconfigFilePath=$DEMO_CONFIG"
fi

[ -f $DEMO_BINARY ] || { echo "Unable to find ${project.artifactId} binary file"; exit 1; }
#[ -f $DEMO_CONFIG ] || { echo "Unable to find configuration file: $DEMO_CONFIG"; exit 1; }

# Display our environment
echo "========================================================================="
echo ""
echo "  ${project.artifactId} Bootstrap Environment"
echo ""
echo "  DEMO_BINARY: $DEMO_BINARY"
echo ""
echo "  JAVA: $JAVA"
echo ""
echo "  JAVA_OPTS: $JAVA_OPTS"
echo ""
echo "========================================================================="
echo ""

cd $DEMO_HOME
exec $JAVA $JAVA_OPTS $DEMO_OPTS -jar $DEMO_BINARY
