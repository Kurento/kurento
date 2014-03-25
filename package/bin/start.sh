#!/bin/sh

DIRNAME=`dirname "$0"`
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

# Setup KMF_MEDIA_CONNECTOR_HOME
RESOLVED_KMF_MEDIA_CONNECTOR_HOME=`cd "$DIRNAME/.."; pwd`
if [ "x$KMF_MEDIA_CONNECTOR_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    KMF_MEDIA_CONNECTOR_HOME=$RESOLVED_KMF_MEDIA_CONNECTOR_HOME
fi
export KMF_MEDIA_CONNECTOR_HOME

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

# determine the default log dir, if not set
if [ "x$KMF_MEDIA_CONNECTOR_LOG_DIR" = "x" ]; then
   KMF_MEDIA_CONNECTOR_LOG_DIR="$KMF_MEDIA_CONNECTOR_HOME/logs"
fi
# determine the default configuration dir, if not set
if [ "x$KMF_MEDIA_CONNECTOR_CONFIG_DIR" = "x" ]; then
   KMF_MEDIA_CONNECTOR_CONFIG_DIR="$KMF_MEDIA_CONNECTOR_HOME/config"
fi

# Display our environment
echo "========================================================================="
echo ""
echo "  KMF Media Connector Bootstrap Environment"
echo ""
echo "  KMF_MEDIA_CONNECTOR_HOME: $KMF_MEDIA_CONNECTOR_HOME"
echo ""
echo "  JAVA: $JAVA"
echo ""
echo "  JAVA_OPTS: $JAVA_OPTS"
echo ""
echo "========================================================================="
echo ""
if [ -z "$PIDFILE" ]; then
	PIDFILE=$KMF_MEDIA_CONNECTOR_HOME/kmf-media-connector.pid
fi

while true; do
   if [ "x$LAUNCH_KMF_IN_BACKGROUND" = "x" ]; then
      # Execute the JVM in the foreground
      eval \"$JAVA\"  $JAVA_OPTS \
         -jar \"$KMF_MEDIA_CONNECTOR_HOME/lib/kmf-media-connector.jar\"
      KMF_STATUS=$?
   else
      # Execute the JVM in the background
      eval \"$JAVA\"  $JAVA_OPTS \
         -jar \"$KMF_MEDIA_CONNECTOR_HOME/lib/kmf-media-connector.jar\" \
         "&"
      PID=$!

      if [ "x$PIDFILE" != "x" ]; then
        echo $PID > $PIDFILE
      fi

echo "========================================================================="
echo ""
      echo "$PID"
echo "$PIDFILE"
echo ""
echo "========================================================================="

      # Wait until the background process exits
      WAIT_STATUS=128
      while [ "$WAIT_STATUS" -ge 128 ]; do
         wait $PID 2>/dev/null
         WAIT_STATUS=$?
         if [ "$WAIT_STATUS" -gt 128 ]; then
            SIGNAL=`expr $WAIT_STATUS - 128`
            SIGNAL_NAME=`kill -l $SIGNAL`
            echo -n "*** KMF Media Connector process ($PID) received $SIGNAL_NAME signal ***" >&2
         fi
      done
      if [ "$WAIT_STATUS" -lt 127 ]; then
         KMF_STATUS=$WAIT_STATUS
      else
         KMF_STATUS=0
      fi
      if [ "$KMF_STATUS" -ne 10 ]; then
            # Wait for a complete shudown
            wait $PID 2>/dev/null
      fi
      if [ "x$PIDFILE" != "x" ]; then
            grep "$PID" $PIDFILE && rm $PIDFILE
      fi
   fi
   if [ "$KMF_STATUS" -eq 10 ]; then
      echo -n "Restarting Kmf Media Connector..."
   else
      exit $KMF_STATUS
   fi
done
