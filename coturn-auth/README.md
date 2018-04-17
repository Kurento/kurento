# coturn
Dockerized coturn with authentication

# run

`docker run -ti --rm --net=host -e LISTENING_PORT=3478 -e REALM=kurento.org -e USER=user -e PASSWORD=s3cr3t --name kurento-coturn kurento-coturn`

# build

`docker build -t kurento-coturn .`
