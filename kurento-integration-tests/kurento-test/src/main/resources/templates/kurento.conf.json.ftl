{
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