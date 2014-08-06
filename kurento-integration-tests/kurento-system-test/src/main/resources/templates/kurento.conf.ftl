[Server]
sdpPattern=pattern.sdp
service=${transport}

[HttpEPServer]
#serverAddress=${serverAddress}

# Announced IP Address may be helpful under situations such as the server needs
# to provide URLs to clients whose host name is different from the one the
# server is listening in. If this option is not provided, http server will try
# to look for any available address in your system.
# announcedAddress=${serverAddress}

serverPort=${httpEndpointPort}

[WebRtcEndPoint]
#stunServerAddress = xxx.xxx.xxx.xxx
#stunServerPort = xx
#pemCertificate = file

[Thrift]
serverAddress=${serverAddress}
serverPort=${serverPort}

[RabbitMQ]
serverAddress = ${serverAddress}
serverPort = ${serverPort}
username = "guest"
password = "guest"
vhost = "/"
