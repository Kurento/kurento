{
  "metadata" : {
  	"publicIp" : "127.0.0.1"
  },
  "mediaServer" : {
    "net" : {
      <#if transport=="rabbitmq">
      "rabbitmq": {
        "address" : "${rabbitAddress}",
        "port" : ${rabbitPort},
        "username" : "guest",
        "password" : "guest",
        "vhost" : "/"
      }
      </#if>
      <#if transport=="ws">
      "websocket": {
        "port": ${wsPort},
        "path": "${wsPath}",
        "threads": 10
        <#if registrar??>
        ,"registrar": {
          "address": "${registrar}",
          "localAddress": "localhost"
        }
        </#if>
      }
      </#if>
    }
  },
  "modules": {
    "kurento": {
      "SdpEndpoint" : {
        "sdpPattern" : "/etc/kurento/sdp_pattern.txt"
      }
    }
  }
}