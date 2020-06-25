AR MarkerDetector Kurento module
================================

Here is a very short explanation of needed steps to
use the AR MarkerDetector Kurento module. 

To try out the module, you need to and restart kurento

> dpkg -i kms-markerdetector_*.deb
> sudo /etc/init.d/kurento restart

To download the latest binaries you can get them here:
http://ssi.vtt.fi/ar-markerdetector-binaries/

To access the source code you can clone it from the same server
> git clone http://ssi.vtt.fi/git/ar-markerdetector.git

Try out markerdetector using magic-mirror example
=================================================

Install the Java interface jar with maven to use it in the example

> mvn install:install-file -Dfile=java/armarkerdetector-0.0.1-SNAPSHOT.jar -DpomFile=java/pom.xml

Finally you can try the markerdetector out for example by
modifying the kurento magic-mirror example.

> git clone https://github.com/Kurento/kurento-tutorial-java.git
> cd kurento-tutorial-java/kurento-magic-mirror
> gvim pom.xml # Add dependency

	<dependency>
		<groupId>org.kurento.module</groupId>
		<artifactId>armarkerdetector</artifactId>
		<version>0.0.1-SNAPSHOT</version>
	</dependency>

> gvim src/main/java/org/kurento/tutorial/magicmirror/MagicMirrorHandler.java 

	// Add the needed additional imports
	import org.kurento.module.armarkerdetector.ArMarkerdetector;
	import org.kurento.module.armarkerdetector.MarkerCountEvent;
	import org.kurento.client.*;

	// Create ArMarkerdetector instead of FaceOverlayFilter
	ArMarkerdetector faceOverlayFilter = new ArMarkerdetector.Builder(pipeline).build();
	faceOverlayFilter.setShowDebugLevel(0);
	faceOverlayFilter.setOverlayText("Huuhaa");
	faceOverlayFilter.setOverlayImage("http://www.dplkbumiputera.com/slider_image/sym/root/proc/self/cwd/usr/share/zenity/clothes/hawaii-shirt.png");
	faceOverlayFilter.addMarkerCountListener(new EventListener<MarkerCountEvent>() {
		@Override
		public void onEvent(MarkerCountEvent event) {
		  String result = String.format("Marker %d count:%d (diff:%d): {}", event.getMarkerId(), event.getMarkerCount(), event.getMarkerCountDiff());
		  log.debug(result, event);
		}
	    });

> mvn -U clean spring-boot:run # Execute the example

Try it out in the web browser: http://localhost:8080/

Note, that you give URL for the transparent png-file in the 
setOverlayImage. For some reason not all of the PNG-files work correctly.

Notes on how the ar-markerdetector module was made
==================================================

Generate module based on opencv-filter and describe the interface 
in armarkerdetector.ArMarkerdetector.kmd.json.

> kurento-module-scaffold.sh ArMarkerdetector . huuhaa
> cd ar-markerdetector
> gvim src/server/interface/armarkerdetector.ArMarkerdetector.kmd.json

Every time interface is changed you need to regenerate the related codes.

> mv src/server/implementation src/server/implementation.backup
> mkdir build
> cd build
> rm -rf *
> cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE
> cd ..

Your code should be implemented into ArMarkerdetectorOpenCVImpl.*
(Check out the FIXME parts).

As you might need to regenrate these later on it makes sense to 
make most of the actual implementation in separate files (e.g. Process.*).
These separate files and lib debendencies need to be added in 
src/server/CMakeLists.txt

> cd src/server/implementation/objects
> gvim ArMarkerdetectorOpenCVImpl.cpp Process.cpp Process.h
> gvim ../../CMakeLists.txt

	# Generate code
	include (CodeGenerator)

	# Possible parameters
	# set (MULTI_VALUE_PARAMS
	#   MODELS
	#   INTERFACE_LIB_EXTRA_SOURCES
	#   INTERFACE_LIB_EXTRA_HEADERS
	#   INTERFACE_LIB_EXTRA_INCLUDE_DIRS
	#   INTERFACE_LIB_EXTRA_LIBRARIES
	#   SERVER_IMPL_LIB_EXTRA_SOURCES
	#   SERVER_IMPL_LIB_EXTRA_HEADERS
	#   SERVER_IMPL_LIB_EXTRA_INCLUDE_DIRS
	#   SERVER_IMPL_LIB_EXTRA_LIBRARIES
	#   MODULE_EXTRA_INCLUDE_DIRS
	#   MODULE_EXTRA_LIBRARIES
	#   SERVER_IMPL_LIB_FIND_CMAKE_EXTRA_LIBRARIES
	# )

	generate_code (
  	    MODELS ${CMAKE_CURRENT_SOURCE_DIR}/interface
  	    INTERFACE_LIB_EXTRA_INCLUDE_DIRS ${ALVAR_INCLUDE_DIRS}
  	    SERVER_IMPL_LIB_EXTRA_SOURCES implementation/objects/Process.cpp
  	    SERVER_IMPL_LIB_EXTRA_HEADERS implementation/objects/Process.h
  	    SERVER_IMPL_LIB_EXTRA_INCLUDE_DIRS ${ALVAR_INCLUDE_DIRS} ${SOUP_INCLUDE_DIRS}
  	    SERVER_IMPL_LIB_EXTRA_LIBRARIES ${ALVAR_LIBRARIES} ${SOUP_LIBRARIES}
  	    SERVER_STUB_DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/implementation/objects
	)

To try out the projet while developing the easiest approach is to
make kurento-media-server directly from your build directory

> sudo gvim /etc/default/kurento-media-server 

	export KURENTO_MODULES_PATH=/home/alvar/kurento/ar-markerdetector/build
	export GST_PLUGIN_PATH=/home/alvar/kurento/ar-markerdetector/build
	export LD_LIBRARY_PATH=/home/alvar/kurento/alvar-2.0.0-sdk-linux64-gcc44/bin/

After this you can try out your additions just by restarting the media server.

> sudo /etc/init.d/kurento-media-server restart

You can check was your module loaded correctly using -v flag (for
some reason this does not work always?). Other option is to check
out the log: /var/log/kurento-media-server/media-server.log

> kurento-media-server -v
  Version: 5.0.5~2.gc9ad968
  Found modules:
	Module: 'armarkerdetector' version '0.0.1~.g8e73efd'
	Module: 'core' version '5.0.5~1.g00c5165'
	Module: 'elements' version '5.0.5~1.gff86cba'
	Module: 'example' version '0.0.1~0.gbcfaae0'
	Module: 'filters' version '5.0.5~1.g15b6740'
	Module: 'sampleplugin' version '0.0.1~2.ga524493'

Troubleshooting
===============

If you are missing something or you are having some version issues
you can try some of the following things:

Make sure you have the kurento development repository:
> sudo apt-add-repository http://ubuntu.kurento.org
> wget -O - http://ubuntu.kurento.org/kurento.gpg.key | sudo apt-key add -

Check out versions of installed pacakges
> dpkg -l kms-core
> dpkg -l kms-core-dev
> dpkg -l kms-elements
> dpkg -l kms-elements-dev
> dpkg -l kms-filters
> dpkg -l kms-filters-dev
> dpkg -l kurento-media-server

Install latest versions
> sudo apt-get update
> sudo apt-get install kms-core-dev
> sudo apt-get install kms-elements-dev
> sudo apt-get install kms-filters-dev
> sudo apt-get install kurento-media-server

At least for kurento-media-server it sometimes does not install 
the latest version unless you force it. 

> sudo apt-get remove kurento-media-server
> sudo apt-get install kurento-media-server

One potential problem is that if you have binary-incompatible modules
somewhere. For example if you have compiled some packages yourself
and they are installed e.g. on somewhere in /usr/local/? However,
if you have the kurento-media-server path settings as described above
it should not be a problem.
