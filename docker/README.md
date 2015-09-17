# How to use this Dockerfile

You can build a docker image based on this Dockerfile. This image will contain only an Stream oriented kurento instance, exposing port 8888. This requires that you have docker installed on your machine.

If you just want to have an Stream oriented kurento running as quickly as possible jump to section The Fastest Way.

If you want to know what is behind the scenes of our container you can go ahead and read the build and run sections.

## The Fastest Way

Docker Compose allows you to link a Stream oriented kurento container to a coturn container in a few minutes. You must install Docker Compose for this method to work.

### Run a container from an image you just built

If you have downloaded the [Stream oriented kurento's](https://github.com/kurento/kurento-docker/) code simply navigate to the docker directory and run

    sudo docker-compose up

You are using this [docker-compose.yml](docker-compose.yml) file. This will instruct to build a new image for Stream oriented kurento and run it linking it to a coturn container.

### Run a container pulling an image from the cloud (recommended)

If you do not have or want to download the Stream oriented kurento repository, you can create a file called `docker-compose.yml` in a directory of your choice and fill it with the following content:

    coturn:
      image: fiware/coturn:4.4.3

    kurento:
      image: fiware/kurento
      links:
        - coturn
      ports:
        - "3478:3478"

Then run

    sudo docker-compose up

This way is equivalent to the previous one, except that it pulls the image from the Docker Registry instead of building your own. Keep in mind though that everything is run locally.

## Build the image

This is an alternative approach to the one presented in the previous section. You do not need to go through these steps if you have used docker-compose. The end result will be the same, but this way you have a bit more of control of what's happening.

You only need to do this once in your system:

    sudo docker build -t kurento .

> **Note**
> If you do not want to have to use `sudo` in this or in the next section follow [these instructions](http://askubuntu.com/questions/477551/how-can-i-use-docker-without-sudo).

The parameter `-t kurento` gives the image a name. This name could be anything, or even include an organization like `-t org/fiware-kurento`. This name is later used to run the container based on the image.

If you want to know more about images and the building process you can find it in [Docker's documentation](https://docs.docker.com/userguide/dockerimages/).

### Run the container

The following line will run the container exposing port `8888`, give it a name -in this case `kurento1`, link it to a coturn docker, and present a bash prompt. This uses the image built in the previous section.

	  sudo docker run -d --name kurento1 --link coturn:coturn -p 3728:3728 kurento

If you did not build the image yourself and want to use the one on Docker Hub use the following command:

	  sudo docker run -d --name orion1 --link mongodb:mongodb -p 1026:1026 fiware/orion -dbhost mongodb

> **Note**
> Keep in mind that if you use this last command you get access to the tags and specific versions of Orion. For example, you may use `fiware/orion:0.22` instead of `fiware/orion` in the above command if you need that particular version. If you do not specify a version you are pulling from `latest` by default.

As a result of this command, there is a context broker listening on port 1026 on localhost. Try to see if it works now with

	curl localhost:1026/version



A few points to consider:

* The name `orion1` can be anything and doesn't have to be related to the name given to the docker image in the previous section.
* `--link mongodb:mongodb` assumes there is a docker container running a MongoDB image in your system, whose name is `mongodb`. In case you need one type `sudo docker run --name mongodb -d mongo:2.6`.
* In `-p 1026:1026` the first value represents the port to listen in on localhost. If you wanted to run a second context broker on your machine you should change this value to something else, for example `-p 1027:1026`.
* Anything after the name of the container image (in this case `orion`) is interpreted as a parameter for the Orion Context Broker. In this case we are telling the broker where the MongoDB host is, represented by the name of our other MongoDB container. Take a look at the [documentation](https://github.com/telefonicaid/fiware-orion) for other command-line options.








Run the media server and expose the websocket on local port 8888:

    docker run --rm -d -p 8888:8888 -t gortazar/kurento-media-server

Kurento Media Server is run by default with debug level 5 (```GST_DEBUG=Kurento*:5```) and stderr redirected to stdout (```2>&1```) so you can use docker logs to inspect output.

Note that ```/usr/bin/kurento-media-server``` is defined as the ```ENTRYPOINT```, and the ```2>&1``` redirection is provided as a ```CMD``` argument. So if you provide any argument to the image, it will override the ```2>&1``` default redirection and you will have to provide it yourself.

## Get help about kurento media server

    docker run --rm -t gortazar/kurento-media-server --help

## Testing the image

Download and run the [hello world](http://www.kurento.org/docs/current/tutorials/java/tutorial-1-helloworld.html) kurento tutorial

    wget http://builds.kurento.org/release/6.1.0/kurento-hello-world-6.1.0.zip
    unzip kurento-hello-world-6.1.0.zip -d kurento-hello-world
    cd kurento-hello-world/lib
    java -jar -Dkms.ws.uri=ws://localhost:8888/kurento

The tutorial starts a web application that is available by default at http://localhost:8080. Click the start button and allow sharing camera and microphone with the application. On the left you'll see what the camera is capturing, this is sent to the media server. On the right you should see the same video as it is sent back by the media server. In this tutorial the video is returned as is. You can check for more tutorials at http://www.kurento.org/docs/current/tutorials.html.
