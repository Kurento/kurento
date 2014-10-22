AR MarkerDetector Kurento module
================================

Here is a very short explanation of needed steps to
use the AR MarkerDetector Kurento module. 

To try out the module, you need to have libalvar200.so
somewhere on the lib path.

> tar xvfz alvar-2.0.0-sdk-linux64-gcc44
> cd alvar-2.0.0-sdk-linux64-gcc44/bin
> sudo cp libalvar200.so /usr/lib

Then you need to install the module and restart kurento

> dpkg -i ar-markerdetector-dev_0.0.1~rc1_amd64.deb 
> sudo /etc/init.d/kurento restart

To access the source code you can clone it from here

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

// Add the import to the example
import org.kurento.module.armarkerdetector.ArMarkerdetector;

// Create ArMarkerdetector instead of FaceOverlayFilter
	ArMarkerdetector faceOverlayFilter = new ArMarkerdetector.Builder(pipeline).build();
	faceOverlayFilter.setShowDebugLevel(0);
	faceOverlayFilter.setOverlayText("Huuhaa");
	faceOverlayFilter.setOverlayImage("http://www.dplkbumiputera.com/slider_image/sym/root/proc/self/cwd/usr/share/zenity/clothes/hawaii-shirt.png");
	faceOverlayFilter.addmarkerEventListener(new EventListener<markerEventEvent>() {
		@Override
		public void onEvent(markerEventEvent event) {
			log.debug("MarkerEvent: {}", event);
		}
	    });

	//FaceOverlayFilter faceOverlayFilter = new FaceOverlayFilter.Builder(
	//		pipeline).build();
	//faceOverlayFilter.setOverlayedImage(
	//		"http://files.kurento.org/imgs/mario-wings.png", -0.35F,
	//		-1.2F, 1.6F, 1.6F);

> mvn compile exec:java # Execute the example

Try it out in the web browser: http://localhost:8080/

Note, that you give URL for the transparent png-file in the 
setOverlayImage. For some reason not all of the PNG-files work correctly

Notes on how the ar-markerdetector module was made
==================================================

Generate module based on opencv-filter

> kurento-module-scaffold.sh ArMarkerdetector . huuhaa
> mkdir build
> cd build
> cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE
> cd ..

You describe the interface in armarkerdetector.ArMarkerdetector.kmd.json.
Every time this is changed you need to regenerate the related codes.

> gvim src/server/interface/armarkerdetector.ArMarkerdetector.kmd.json
> mv src/server/implementation src/server/implementation.backup
> cd build
> rm -rf *
> cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE
> cd ..

Your code should be implemented into ArMarkerdetectorOpenCVImpl.*
As you might need to regenrate these later on it makes sense to 
make most of the actual implementation in separate files (e.g. Process.*).
These separate files and lib debendencies need to be added in src/server/.

> cd src/server/implementation/objects
>

To try out the projet while developing the easiest approach is to
make kurento-media-server directly from your build directory



