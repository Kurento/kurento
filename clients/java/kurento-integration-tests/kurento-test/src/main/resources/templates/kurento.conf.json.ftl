{
  "metadata" : {
  	"publicIp" : "127.0.0.1"
  },
  "mediaServer" : {
    "net" : {
      "websocket": {
        "port": ${wsPort},
        "path": "${wsPath}",
        "threads": 10
        <#if registrar??>
        ,"registrar": {
           "address": "${registrar}",
           "localAddress": "${registrarLocalAddress}"
        }
        </#if>
      }
    }
  }
}
