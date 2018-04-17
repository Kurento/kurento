# coturn
Dockerized coturn with authentication

# run

`docker run -ti --rm --net=host -e LISTENING_PORT=3478 -e REALM=kurento.org -e USER=user -e PASSWORD=s3cr3t --name kurento-coturn kurento/coturn-auth`

# build

`docker build -t kurento/coturn-auth .`

# create an user

`docker exec -ti coturn turnadmin -a -b /var/local/turndb -u user -r kurento.org -p s3cr3t`

# delete an user

`docker exec -ti coturn turnadmin -d -b /var/local/turndb -u user -r kurento.org`

# testing

Check out [this](https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/) with:

var | value
--- | --- 
STUN or TURN URI | turn:PUBLIC_IP:PORT
TURN username | user 
TURN password | s3cr3t
