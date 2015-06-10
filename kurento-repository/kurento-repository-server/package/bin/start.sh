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
KREPO_HOME=$(cd $DIRNAME/..;pwd)
KREPO_BINARY=$KREPO_HOME/lib/kurento-repo.jar
if [ ! -f $KREPO_BINARY ]; then
    # System installation
    [ -f /etc/default/kurento-repo ] && . /etc/default/kurento-repo
    [ -f /etc/sysconfig/kurento-repo ] && . /etc/sysconfig/kurento-repo
    KREPO_HOME=/var/lib/kurento
    KREPO_BINARY=$KREPO_HOME/kurento-repo.jar
    KREPO_CONFIG="/etc/kurento/kurento-repo.conf.json"
    KREPO_LOG_CONFIG=/etc/kurento/kurento-repo-log4j.properties
    KREPO_LOG_FILE=/var/log/kurento/kurento-repo.log
else
    # Home based installation
    KREPO_CONFIG=$KREPO_HOME/config/kurento-repo.conf.json
    KREPO_LOG_CONFIG=$KREPO_HOME/config/kurento-repo-log4j.properties
    KREPO_LOG_FILE=$KREPO_HOME/logs/kurento-repo.log
    mkdir -p $KREPO_HOME/logs
fi

# logging.config ==> Springboot logging config
# log4j.configuration ==> log4j default config. Do not remove to avoid exception for all login taking place before Springboot has started
KREPO_OPTS="$KREPO_OPTS -DconfigFilePath=$KREPO_CONFIG"
KREPO_OPTS="$KREPO_OPTS -Dkurento-repo.log.file=$KREPO_LOG_FILE"
KREPO_OPTS="$KREPO_OPTS -Dlogging.config=$KREPO_LOG_CONFIG"
KREPO_OPTS="$KREPO_OPTS -Dlog4j.configuration=$KREPO_LOG_CONFIG"

[ -f $KREPO_CONFIG ] || { echo "Unable to find configuration file: $KREPO_CONFIG"; exit 1; }

# Display our environment
echo "========================================================================="
echo ""
echo "  Kurento Repository Bootstrap Environment"
echo ""
echo "  KREPO_BINARY: $KREPO_BINARY"
echo ""
echo "  JAVA: $JAVA"
echo ""
echo "  JAVA_OPTS: $JAVA_OPTS"
echo ""
echo "  KREPO_OPTS: $KREPO_OPTS"
echo ""
echo "========================================================================="
echo ""

cd $KREPO_HOME
exec $JAVA $JAVA_OPTS $KREPO_OPTS -jar $KREPO_BINARY 
