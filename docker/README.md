# How to use this Dockerfile

You can build a docker image based on this Dockerfile. This image will contain only an Stream oriented kurento instance, exposing port 8888. This requires that you have docker installed on your machine.

If you just want to have an Stream oriented kurento running as quickly as possible jump to section The Fastest Way.

If you want to know what is behind the scenes of our container you can go ahead and read the build and run sections.

## The Fastest Way

### Run a container from an image you just built

If you have downloaded the [Stream oriented kurento's](https://github.com/kurento/kurento-docker/) code simply navigate to the docker directory and run

    sudo docker build -t kurento/kurento-media-server .
    sudo docker run kurento/kurento-media-server

This will build a new docker image and store it locally as kurento/kurento-media-server. Then, it starts the created image in the frontend.

### Run a container pulling an image from the cloud (recommended)

If you do not have or want to download the Stream oriented kurento repository, you can run kurento-media-server directly:

    sudo docker run kurento/kurento-media-server

This way is equivalent to the previous one, except that it pulls the image from the Docker Registry instead of building your own. Keep in mind though that everything is run locally.

> **Note**
> If you do not want to have to use `sudo` in this or in the next section follow [these instructions](http://askubuntu.com/questions/477551/how-can-i-use-docker-without-sudo).

The parameter `-t kurento` gives the image a name. This name could be anything, or even include an organization like `-t org/fiware-kurento`. This name is later used to run the container based on the image.

If you want to know more about images and the building process you can find it in [Docker's documentation](https://docs.docker.com/userguide/dockerimages/).

### Run the container

The following line will run the container exposing port `8888`, giving it a name -in this case `kurento`:

	  sudo docker run -d --name kurento -p 8888:8888 kurento/kurento-media-server

As a result of this command, there is a kurento-media-server listening on port 8888 on localhost. Try to see if it works now with

curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Host: 127.0.0.1:8888" -H "Origin: 127.0.0.1" http://127.0.0.1:8888 | grep -q "Server: WebSocket++"

Kurento Media Server is run by default with debug level 5 (```GST_DEBUG=Kurento*:5```).

## Get help about kurento media server

    docker run --rm kurento/kurento-media-server --help
