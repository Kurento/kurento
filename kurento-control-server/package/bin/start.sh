#!/bin/sh

DIRNAME=$(dirname "$0")
GREP="grep"

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
KMC_HOME=$(cd $DIRNAME/..;pwd)
KMC_BINARY=$KMC_HOME/lib/kmf-media-connector.jar
KMC_CONFIG=$KMC_HOME/config/application.properties
if [ ! -f $KMC_BINARY ]; then
    KMC_HOME=/var/lib/kurento
    KMC_BINARY=$KMC_HOME/kmf-media-connector.jar
    KMC_CONFIG="/etc/kurento/media-connector.properties"
    KMC_OPTS="--spring.config.location=$KMC_CONFIG"
fi

[ -f $KMC_BINARY ] || { echo "Unable to find KMC binary file"; exit 1; }
[ -f $KMC_CONFIG ] || { echo "Unable to find configuration file: $KMC_CONFIG"; exit 1; }

# Display our environment
echo "========================================================================="
echo ""
echo "  KMF Media Connector Bootstrap Environment"
echo ""
echo "  KMC_BINARY: $KMC_BINARY"
echo ""
echo "  JAVA: $JAVA"
echo ""
echo "  JAVA_OPTS: $JAVA_OPTS"
echo ""
echo "========================================================================="
echo ""

cd $KMC_HOME
exec $JAVA $JAVA_OPTS -jar $KMC_BINARY $KMC_OPTS
