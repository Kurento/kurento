/**************************************************************************************
* Simple example how to use ALVAR with osgViewer
* 
* This sample demonstrates the marker hiding feature. Sample is based on Moder2marker
* sample but now we also hide the marker 5.
* Hiding is based on using textures on top of the markers. For this purpose we implement
* simple class to hold the texture for marker. NOTE! This implementation is very simple 
* and can be used as is only in this sample (hard coded marker size...).
*
* More information can be found from the ALVAR manual
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
#include <osg/BlendFunc>

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
IplImage *markerHiderImage = NULL;
osg::ref_ptr<osg::Image>			texImage; 

class CTextureOnMarker
{
public:
	CTextureOnMarker()
	{
		m_nXsize = 64;
		m_nYsize = 64;

		texture = new osg::Texture2D();
		texture->setResizeNonPowerOfTwoHint(false);

		group			= new osg::Group();
		rectGeode		= new osg::Geode();
		rectGeometry	= new osg::Geometry();

		group->addChild(rectGeode.get());
		rectGeode->addDrawable(rectGeometry.get());

		rectVertices = new osg::Vec3Array(4);
		(*rectVertices)[0].set(-3.f,-3.f,0.f); 
		(*rectVertices)[1].set(-3.f, 3.f,0.f); 
		(*rectVertices)[2].set( 3.f, 3.f,0.f);
		(*rectVertices)[3].set( 3.f,-3.f,0.f);

	
		rectGeometry->setVertexArray( rectVertices );
		rectGeometry->addPrimitiveSet(new osg::DrawArrays(osg::PrimitiveSet::QUADS,0,4));

		texCoords = new osg::Vec2Array(4);
		(*texCoords)[0].set(0.0f,0.0f); 
		(*texCoords)[1].set(0.f,1.0f); 
		(*texCoords)[2].set(1.f,1.f); 
		(*texCoords)[3].set(1.f,0.f); 

		rectGeometry->setTexCoordArray(0,texCoords.get());

		osg::StateSet* stateset = rectGeometry->getOrCreateStateSet();
		stateset->setTextureAttributeAndModes(0, texture.get(), osg::StateAttribute::ON);
		stateset->setMode(GL_DEPTH_TEST, osg::StateAttribute::OFF);

		stateset->setMode(GL_CULL_FACE, osg::StateAttribute::OFF);
		stateset->setMode(GL_LIGHTING, osg::StateAttribute::OFF);
		stateset->setMode(GL_ALPHA_TEST, osg::StateAttribute::ON);

		osg::BlendFunc    *blendFunc = new osg::BlendFunc();

		blendFunc->setSource(osg::BlendFunc::SRC_ALPHA);
		blendFunc->setDestination(osg::BlendFunc::ONE_MINUS_SRC_ALPHA);
		stateset->setAttributeAndModes(blendFunc,osg::StateAttribute::ON);

		stateset->setRenderingHint(osg::StateSet::TRANSPARENT_BIN);
		stateset->setMode(GL_LIGHTING, osg::StateAttribute::OFF);
		stateset->setRenderBinDetails(-1, "RenderBin");

	}
	osg::Group * GetDrawable(){	return group.get(); }
	void SetBGImage(osg::Image *image){texture->setImage(image);};
private:
	
	int m_nXsize;
	int m_nYsize;
	osg::ref_ptr<osg::Group>			group;
	osg::ref_ptr<osg::Geode>			rectGeode;
	osg::ref_ptr<osg::Geometry>			rectGeometry;
	osg::Vec3Array						*rectVertices;
	osg::ref_ptr<osg::Texture2D>		texture;
	osg::ref_ptr<osg::Vec2Array>		texCoords;
};

CTextureOnMarker texOnMarker;
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

			//Generate the texture for hiding the marker
			const double margin = 3.0;
			alvar::BuildHideTexture(image, markerHiderImage, &camera, temp_mat, alvar::PointDouble(-margin, -margin), alvar::PointDouble(margin, margin));
		
			modelSwitch->setChildValue(mtForMarkerFive, 1);
			// Update the matrix transformation
			mtForMarkerFive->setMatrix(osg::Matrix(temp_mat));
			
			//Set the marker texture to the osg::Image
			texImage->setImage(markerHiderImage->width, markerHiderImage->height, 1, 4, GL_BGRA, GL_UNSIGNED_BYTE, (unsigned char*)markerHiderImage->imageData, osg::Image::NO_DELETE);
			//..and update the texture system
			texOnMarker.SetBGImage(texImage.get());
			
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

		//Create the osg::Image for the texture (marker hiding)
		texImage = new osg::Image;
		
		//IplImage for the texture generation.
		markerHiderImage=cvCreateImage(cvSize(64, 64), 8, 4);

		// construct the viewer
		viewer = new osgViewer::Viewer(arguments);
		// Let's use window size of the video (approximate).
		viewer->setUpViewInWindow (200, 200, videoXRes, videoYRes);
		// Viewport is the same
		viewer->getCamera()->setViewport(0,0,videoXRes,videoYRes);
		viewer->setLightingMode(osg::View::HEADLIGHT);

		// Attach our own event handler to the system so we can catch the resizing events
		viewer->addEventHandler(new CSimpleWndSizeHandler(videoXRes,videoYRes ));
		
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

		// add the texture under the marker transformation node
		mtForMarkerFive->addChild(texOnMarker.GetDrawable());

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

	if(markerHiderImage)
		cvReleaseImage(&markerHiderImage);
	
	return 0; // bye bye. Happy coding!
}

