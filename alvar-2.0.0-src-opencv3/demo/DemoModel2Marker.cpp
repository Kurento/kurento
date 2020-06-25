/**************************************************************************************
* Simple example how to use ALVAR with osgViewer
* 
* this sample detects markers from the video view. If markers 5 and/or 10 is visible, 
* models are added to to view in top of the markers. 
* 
* Models are basic OSG models (download the models from the OpenSceneGraph's webpage)
*
* The sample utilizes the convenience system from ALVAR to capture the video and in this
* case we use OpenCV's Highgui approach. However, the video capturing is totally independent
* option/feature in ALVAR framework i.e the main ALVAR library is not depended on this in any way.
* You more than welcome to use any other video capture library in your own systems. 
**************************************************************************************/

//OSG includes
#include <osgViewer/Viewer>
#include <osgViewer/ViewerEventHandlers>
#include <osgDB/ReadFile>
#include <osg/Texture2D> 
#include <osg/Projection>
#include <osg/MatrixTransform>
#include <osg/Image>
#include <osg/Group>
#include <osg/Geometry>
#include <osg/Geode>
#include <osg/Switch>

//ALVAR includes
#include "CaptureFactory.h" // Video capturing
#include "MarkerDetector.h" // Marker detector

#include "Shared.h"

osg::ref_ptr<osgViewer::Viewer>		viewer; // The OSG viewer
osg::ref_ptr<osg::Group>			arRoot; // The main group for scene
osg::ref_ptr<osg::Image>			videoImage; // camera video frame image in OSG image format
osg::ref_ptr<osg::MatrixTransform>	mtForMarkerFive; // Matrix transformation for marker 5 (id)
osg::ref_ptr<osg::MatrixTransform>	mtForMarkerTen;  // Matrix transformation for marker 10 (id)
osg::ref_ptr<osg::Node>				modelForMarkerFive=NULL; //This contains the model for marker 5
osg::ref_ptr<osg::Node>				modelForMarkerTen=NULL;  //This contains the model for marker 10
osg::ref_ptr<osg::Switch>			modelSwitch; //Switch to toggle model visibility 

int videoXRes=0; // Video capture width (x)
int videoYRes=0; // video capture height (y)

alvar::Capture*	capture = NULL; // ALVAR Capture

alvar::Camera camera; // ALVAR camera (do not confuse to the OSG)
alvar::MarkerDetector<alvar::MarkerData> markerDetector; // Marker detector

// Size of the marker
// The size is an "abstract value" for library, but using normal, logical values (mm, cm, m, inch) will help 
// understanding the model scaling and positioning in human point of view.
#define MARKER_SIZE	5 //as in centimeters

CVideoBG videoBG;

/*
The main rendering function.
*/
void renderer()
{
	// Capture the image
	IplImage *image = capture->captureImage();

	// Check if we need to change image origin and is so, flip the image.
	bool flip_image = (image->origin?true:false);
	if (flip_image) {
		cvFlip(image);
		image->origin = !image->origin;
	}

	// Detect all the markers from the frame
	markerDetector.Detect(image, &camera, false, false);
	
	// Loop throught the list of detected markers
	for (size_t i=0; i<markerDetector.markers->size(); i++) {

		// Get the ID of the marker
		int id = (*(markerDetector.markers))[i].GetId();

		// Get the marker's pose (transformation matrix)
		double temp_mat[16];
		alvar::Pose p = (*(markerDetector.markers))[i].pose;
		p.GetMatrixGL(temp_mat);

	
		if( id == 5){ //Marker 5 is visible
			//Switch the 5 on
			modelSwitch->setChildValue(mtForMarkerFive, 1);
			// Update the matrix transformation
			mtForMarkerFive->setMatrix(osg::Matrix(temp_mat));
		}
		else if(id == 10){ //Marker 10 is visible
			//Switch the 10 on
			modelSwitch->setChildValue(mtForMarkerTen, 1);
			// Update the matrix transformation
			mtForMarkerTen->setMatrix(osg::Matrix(temp_mat));
		}
	}

	// In case we flipped the image, it's time to flip it back 
	if (flip_image) {
		cvFlip(image);
		image->origin = !image->origin;
	}

	// "copy" the raw image data from IplImage to the Osg::Image
	videoImage->setImage(image->width, image->height, 1, 4, GL_BGR, GL_UNSIGNED_BYTE, (unsigned char*)image->imageData, osg::Image::NO_DELETE);
	if(videoImage.valid()){
		// Set the latest frame to the view as an background texture
		videoBG.SetBGImage(videoImage.get());
	}
	// Draw the frame
	viewer->frame();
	
	//Swiths all the models of until next frame
	modelSwitch->setAllChildrenOff();
}

// main
int main(int argc, char** argv)
{
	osg::ArgumentParser arguments(&argc, argv);

	// Let's use the convenience system from ALVAR for capturing.
	// We initialize Highgui through the CaptureFactory (see manual for other options like AVI)
    alvar::CaptureFactory *factory = alvar::CaptureFactory::instance();
    alvar::CaptureFactory::CaptureDeviceVector devices = factory->enumerateDevices("highgui");

    // Check to ensure that a device was found
    if (devices.size() > 0) {
        capture = factory->createCapture(devices.front());
    }

	// Capture is central feature, so if we fail, we get out of here.
    if (capture && capture->start()) {
	   
		// Let's capture one frame to get video resolution
		IplImage *tempImg = capture->captureImage();
		videoXRes = tempImg->width;
		videoYRes = tempImg->height;

		// Calibration. See manual and ALVAR internal samples how to calibrate your camera
		// Calibration will make the marker detecting and marker pose calculation more accurate.
		if (! camera.SetCalib("calib.xml", videoXRes, videoYRes)) {
			camera.SetRes(videoXRes, videoYRes);
		}

		//Create the osg::Image for the video
		videoImage = new osg::Image;

		// construct the viewer
		viewer = new osgViewer::Viewer(arguments);
		// Let's use window size of the video (approximate).
		viewer->setUpViewInWindow (200, 200, videoXRes, videoYRes);
		// Viewport is the same
		viewer->getCamera()->setViewport(0,0,videoXRes,videoYRes);
		viewer->setLightingMode(osg::View::HEADLIGHT);

		// Attach our own event handler to the system so we can catch the resizing events
		viewer->addEventHandler(new CSimpleWndSizeHandler(videoXRes,videoYRes));
		
		// Set projection matrix as ALVAR recommends (based on the camera calibration)
		double p[16];
		camera.GetOpenglProjectionMatrix(p,videoXRes,videoYRes);
		viewer->getCamera()->setProjectionMatrix(osg::Matrix(p));

		// Create main root for everything
		arRoot = new osg::Group;
		arRoot->setName("ALVAR stuff (c) VTT");
		
		// Init the video background class and add it to the graph
		videoBG.Init(videoXRes,videoYRes,(tempImg->origin?true:false));
		arRoot->addChild(videoBG.GetOSGGroup());

		// Create model switch and add it the to graph
		modelSwitch = new osg::Switch;
		arRoot->addChild(modelSwitch.get());

		// Create model transformation for the markers and add them under the switch
		mtForMarkerFive = new osg::MatrixTransform;
		mtForMarkerTen = new osg::MatrixTransform;
		modelSwitch->addChild(mtForMarkerFive.get());
		modelSwitch->addChild(mtForMarkerTen.get());

		// All models off
		modelSwitch->setAllChildrenOff();

		// load the data (models).
		modelForMarkerFive = osgDB::readNodeFile("grid.osg");
		modelForMarkerTen = osgDB::readNodeFile("axes.osg");
	    
		// If loading ok, add models under the matrixtransformation nodes.
		if(modelForMarkerFive)
			mtForMarkerFive->addChild(modelForMarkerFive.get());

		if(modelForMarkerTen)
			mtForMarkerTen->addChild(modelForMarkerTen.get());
	    
		// Tell the ALVAR the markers' size (same to all)
		// You can also specify different size for each individual markers
		markerDetector.SetMarkerSize(MARKER_SIZE);

		// Set scene data 
		viewer->setSceneData(arRoot.get());

		// And start the main loop
		while(!viewer->done()){
			//Call the rendering function over and over again.
			renderer();
		}
	}
	// Time to close the system
	if(capture){
		capture->stop();
		delete capture;
	}
	
	return 0; // bye bye. Happy coding!
}

