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
KJRS_HOME=$(cd $DIRNAME/..;pwd)
KJRS_BINARY=$KJRS_HOME/lib/kjrserver.jar
if [ ! -f $KJRS_BINARY ]; then
    # System installation
    [ -f /etc/default/kjrserver ] && . /etc/default/kjrserver
    [ -f /etc/sysconfig/kjrserver ] && . /etc/sysconfig/kjrserver
    KJRS_HOME=/var/lib/kurento
    KJRS_BINARY=$KJRS_HOME/kjrserver.jar
    KJRS_CONFIG="/etc/kurento/kjrserver.conf.json"
    KJRS_LOG_CONFIG=/etc/kurento/kjrserver-log4j.properties
    KJRS_LOG_FILE=/var/log/kurento/kjrserver.log
else
    # Home based installation
    KJRS_CONFIG=$KJRS_HOME/config/kjrserver.conf.json
    KJRS_LOG_CONFIG=$KJRS_HOME/config/kjrserver-log4j.properties
    KJRS_LOG_FILE=$KJRS_HOME/logs/kjrserver.log
    mkdir -p $KJRS_HOME/logs
fi

# logging.config ==> Springboot logging config
# log4j.configuration ==> log4j default config. Do not remove to avoid exception for all login taking place before Springboot has started
KJRS_OPTS="$KJRS_OPTS -DconfigFilePath=$KJRS_CONFIG"
KJRS_OPTS="$KJRS_OPTS -Dkjrserver.log.file=$KJRS_LOG_FILE"
KJRS_OPTS="$KJRS_OPTS -Dlogging.config=$KJRS_LOG_CONFIG"
KJRS_OPTS="$KJRS_OPTS -Dlog4j.configuration=$KJRS_LOG_CONFIG"

[ -f $KJRS_CONFIG ] || { echo "Unable to find configuration file: $KJRS_CONFIG"; exit 1; }

# Display our environment
echo "========================================================================="
echo ""
echo "  Kurento JSON-RPC Test Server Bootstrap Environment"
echo ""
echo "  KJRS_BINARY: $KJRS_BINARY"
echo ""
echo "  JAVA: $JAVA"
echo ""
echo "  JAVA_OPTS: $JAVA_OPTS"
echo ""
echo "  KJRS_OPTS: $KJRS_OPTS"
echo ""
echo "========================================================================="
echo ""

cd $KJRS_HOME
exec $JAVA $JAVA_OPTS $KJRS_OPTS -jar $KJRS_BINARY 
