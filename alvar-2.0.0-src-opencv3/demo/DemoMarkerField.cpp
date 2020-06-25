/**************************************************************************************
* Simple example how to use ALVAR's markerfield (multimarker) with osgViewer
* 
* This sample detects marker field (predefined) from the view. When the field
* is detected, system adds the model on top of it. If the field cannot be detected
* (after the first detection) the system switches it self to the 
* visual based tracking and tries to keep the model in correct position. As soon the
* field is detected again the system is back on normal state. Note, the visual based
* tracking here can handle only pan-tilt action. No depth or rotate. See ALVAR manual 
* for further details and options.
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
#include "MultiMarker.h"    // Multimarker system (marker field)
#include "TrackerStat.h"    // Visual based tracking system

#include "Shared.h"

osg::ref_ptr<osgViewer::Viewer>		viewer; // The OSG viewer
osg::ref_ptr<osg::Group>			arRoot; // The main group for scene
osg::ref_ptr<osg::Image>			videoImage; // camera video frame image in OSG image format
osg::ref_ptr<osg::MatrixTransform>	mtForMarkerField; // Matrix transformation for marker field

osg::ref_ptr<osg::Node>				modelNode=NULL; //This contains the model

int videoXRes=0; // Video capture width (x)
int videoYRes=0; // video capture height (y)
float video_X_FOV; // Video horizontal field of view
float video_Y_FOV; // Video vertical field of view
alvar::Capture*	capture = NULL; // ALVAR Capture

alvar::Camera camera; // ALVAR camera (do not confuse to the OSG)
alvar::MarkerDetector<alvar::MarkerData> markerDetector; // Marker detector
alvar::TrackerStat trackerStat; // Visual tracker
alvar::MultiMarker *multiMarker;// MultiMarker
std::vector<int> markerIdVector;// vector that contains marker field marker ids

// Size of the markers in the marker field
// The size is an "abstract value" for library, but using normal, logical values (mm, cm, m, inch) will help 
// understanding the model scaling and positioning in human point of view.
#define CORNER_MARKER_SIZE	4 // as in centimeters
#define CENTRE_MARKER_SIZE  8 // as in centimeters
#define MARKER_COUNT		5 // marker count in the field

CVideoBG videoBG;

/*
	The main rendering function.
*/
void renderer()
{
	alvar::Pose pose;
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
	trackerStat.Track(image);

	// Detect the markers
	if (markerDetector.Detect(image, &camera, false, false)) {
		// if ok, we have field in sight
		// Update the data
		multiMarker->Update(markerDetector.markers, &camera, pose);

		// get the field's matrix
		double temp_mat[16];
		pose.GetMatrixGL(temp_mat);
		mtForMarkerField->setMatrix(osg::Matrix(temp_mat));
		trackerStat.Reset();
	}else
	{
		// The field is not on sight, so let's try to use the model in
		// right position by using tracking system
		double trackerStat_dx_Angle = -(video_X_FOV * trackerStat.xd  / float(videoXRes));
		double trackerStat_dy_Angle = -(trackerStat.yd * video_Y_FOV / float(videoYRes));
		float z = 0.0;

		osg::Matrix m(mtForMarkerField->getMatrix());
				
		m.postMult(osg::Matrix::rotate(trackerStat_dy_Angle, osg::Vec3(1,0,0),
									   trackerStat_dx_Angle, osg::Vec3(0,1,0),
									   0.0, osg::Vec3(0,0,1)));

		mtForMarkerField->setMatrix(m);
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

		// Get the video fov for the tracking system
		video_X_FOV = camera.GetFovX();
		video_Y_FOV = camera.GetFovY();


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

		// Create model transformation for the markerfield and add it to the scene
		mtForMarkerField = new osg::MatrixTransform;
		//At start we scale the model zero (invisible)
		mtForMarkerField->preMult(osg::Matrix::scale(osg::Vec3(0,0,0)));
		arRoot->addChild(mtForMarkerField.get());

		modelNode = osgDB::readNodeFile("axes.osg");
	    
		// If loading ok, add models under the matrixtransformation nodes.
		if(modelNode)
			mtForMarkerField->addChild(modelNode.get());

		//Initialize the multimarker system

		for(int i = 0; i < MARKER_COUNT; ++i)
			markerIdVector.push_back(i);

		// We make the initialization for MultiMarkerBundle using a fixed marker field (can be printed from MultiMarker.ppt)
		
		markerDetector.SetMarkerSize(CORNER_MARKER_SIZE);
		markerDetector.SetMarkerSizeForId(0, CENTRE_MARKER_SIZE);
		
		multiMarker = new alvar::MultiMarker(markerIdVector);
		
		alvar::Pose pose;
		pose.Reset();
		multiMarker->PointCloudAdd(0, CENTRE_MARKER_SIZE, pose);
		
		pose.SetTranslation(-10, 6, 0);
		multiMarker->PointCloudAdd(1, CORNER_MARKER_SIZE, pose);
		
		pose.SetTranslation(10, 6, 0);
		multiMarker->PointCloudAdd(2, CORNER_MARKER_SIZE, pose);
		
		pose.SetTranslation(-10, -6, 0);
		multiMarker->PointCloudAdd(3, CORNER_MARKER_SIZE, pose);
		
		pose.SetTranslation(+10, -6, 0);
		multiMarker->PointCloudAdd(4, CORNER_MARKER_SIZE, pose);
	

		// Set scene data 
		viewer->setSceneData(arRoot.get());

		trackerStat.Reset();
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

