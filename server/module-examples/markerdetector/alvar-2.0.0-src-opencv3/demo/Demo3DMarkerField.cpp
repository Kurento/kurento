/**************************************************************************************
* Experimental
*
* Example how to use ALVAR's dynamical markefield tracking with osgViewer.
* 
* This sample deduces the relative poses and positions of markers that have been
* laid in any 3D configuration. Then the program uses the deduced marker configuration
* for tracking and overlays an OSG model to the scene.
* 
* Model is a basic OSG model (download the models from the OpenSceneGraph's webpage)
**************************************************************************************/



// OSG Includes
#include <osgDB/ReadFile>
#include <osgViewer/Viewer>
#include <osgViewer/CompositeViewer>
#include <osgGA/TrackballManipulator>
#include <osgViewer/ViewerEventHandlers>
#include <osg/MatrixTransform>
#include <osg/Depth>
#include <osg/BlendFunc>
#include <osg/BlendColor>
#include <osg/PositionAttitudeTransform>
#include <osgUtil/Optimizer>
#include <osg/Material>
#include <osg/CullFace>
#include <osg/PolygonMode>
#include <osg/LineWidth>
#include <osg/Point>

// ALVAR Includes
#include <Camera.h>
#include <Pose.h>
#include <MarkerDetector.h>
#include <CaptureFactory.h>
#include <MultiMarkerBundle.h>
#include <MultiMarkerInitializer.h>

// Own includes
#include "Shared.h"



osg::ref_ptr<osgViewer::Viewer> viewer;
osg::ref_ptr<osg::Node> model_node;
osg::ref_ptr<osg::Switch> model_switch;
osg::ref_ptr<osg::Group> root_group;
osg::ref_ptr<osg::MatrixTransform> model_transform;
ViewWithBackGroundImage *viewBG;

const int nof_markers = 12;
const double marker_size = 4.0;
int pose_marker = 0;
int curr_meas = 0;
bool optimize_done = false;
int every_20th = 0;
alvar::MarkerDetector<alvar::MarkerData> marker_detector;
alvar::MultiMarkerInitializer *multi_marker_init=NULL;
alvar::MultiMarkerBundle *multi_marker_bundle=NULL;
alvar::Camera cam;

alvar::Capture *capture = 0;
IplImage *frame = 0;
IplImage *gray  = 0;
IplImage *rgb   = 0;
int _width=0, _height=0;
int _origin=0;

bool stop_running  = false;
bool reset		   = false;
bool visualize	   = true;



/*
	The keyboard handler.
*/
class PickHandler : public osgGA::GUIEventHandler 
{
public:

	PickHandler() {}
	~PickHandler() {}

    bool handle(const osgGA::GUIEventAdapter& ea, osgGA::GUIActionAdapter& aa)
    {
        osgViewer::View* view = dynamic_cast<osgViewer::View*>(&aa);
        if (!view) 
			return false;

        switch(ea.getEventType())
        {
			case(osgGA::GUIEventAdapter::KEYDOWN):
			{
				switch(ea.getKey())
				{
					case 'r': case 'R':
						{
							std::cout << "Resetting multi marker" << std::endl;
							multi_marker_init->Reset();
							multi_marker_init->MeasurementsReset();
							multi_marker_bundle->Reset();
							multi_marker_bundle->MeasurementsReset();
							optimize_done = false;
							visualize	  = true;
							curr_meas  = 0;
							every_20th = 0;
						}
						break;
					case 'q': case 'Q':
						{
							stop_running = true;
							std::cout << "Stopping..." << std::endl;
						} 
						break;
				}
			}
		}			
        return true;
	}
};



/*
	Initialize video capture.
*/
bool InitVideoCapture(size_t use_cap) 
{
    // Enumerate possible capture devices and check the command line argument which one to use
	std::cout << "Enumerated Capture Devices:" << std::endl;
	alvar::CaptureFactory::CaptureDeviceVector vec = alvar::CaptureFactory::instance()->enumerateDevices();

	if (vec.size() < 1) 
	{
		std::cout << "  none found" << std::endl;
		return false;
	}
	if (use_cap >= vec.size()) 
	{
		std::cout << "  required index not found - using 0" << std::endl;
		use_cap=0;
	}
	for (size_t i=0; i<vec.size(); i++) 
	{
        if (use_cap == i) 
			std::cout << "* "; 
		else 
			std::cout << "  ";
		std::cout << i << ": " << vec[i].uniqueName();
        if (vec[i].description().length() > 0) 
			std::cout << ", " << vec[i].description();
		std::cout << std::endl;
	}
    std::cout << std::endl;
	capture = alvar::CaptureFactory::instance()->createCapture(vec[use_cap]);

	if (capture) 
	{
		std::stringstream filename;
		filename << "camera_settings_" << vec[use_cap].uniqueName() << ".xml";
		capture->start();
		if (capture->loadSettings(filename.str())) 
			std::cout << "read: " << filename.str() << std::endl;

		int i;
		IplImage *dummy;
		for (i=0; i<10; i++) {
			dummy = capture->captureImage();
			if (dummy) 
				break;
// todo: why is a sleep necessary?
            alvar::sleep(100);
		}
		if (i == 10) 
			return false;
		_width  = dummy->width;
		_height = dummy->height;
		_origin = dummy->origin;
		return true;
	}
	return false;
}



/*
	Allocate memory for one RGB image and one grayscale image.
*/
bool InitImages(int w, int h, int origin)
{
	if (w==0||h==0) 
		return false;
	CvSize size = cvSize(w, h);
	if (!rgb)
	{
		rgb  = cvCreateImage(size, 8, 3);
		rgb->origin = origin;
	}
	if (!gray)
	{
		gray = cvCreateImage(size, 8, 1);
		gray->origin = origin;
	}
	return true;
}



/*
	Initialize OSG, create a Viewer
*/
bool InitOSG(int w, int h, bool flip_image, std::string model_file, std::string camera_file)
{
	int offx = 10;
	int offy = 30;

	viewer = new osgViewer::Viewer;
	osg::GraphicsContext::WindowingSystemInterface* wsi = osg::GraphicsContext::getWindowingSystemInterface();
    if (!wsi)
    {
        osg::notify(osg::NOTICE) << "Error, no WindowSystemInterface available, cannot create windows." << std::endl;
        return false;
    }

    unsigned int width, height;
    wsi->getScreenResolution(osg::GraphicsContext::ScreenIdentifier(0), width, height);

    osg::ref_ptr<osg::GraphicsContext::Traits> traits = new osg::GraphicsContext::Traits;
    traits->x	   = offx;
    traits->y	   = offy;
    traits->width  = w;
    traits->height = h;
    traits->windowDecoration = true;
    traits->doubleBuffer	 = true;
    traits->sharedContext	 = 0;

    osg::ref_ptr<osg::GraphicsContext> gc = osg::GraphicsContext::createGraphicsContext(traits.get());
    if (gc.valid())
    {
        osg::notify(osg::INFO) << "  GraphicsWindow has been created successfully." << std::endl;
        // need to ensure that the window is cleared make sure that the complete window is set the correct colour
        // rather than just the parts of the window that are under the camera's viewports
        gc->setClearColor(osg::Vec4f(0.2f,0.2f,0.6f,1.0f));
        gc->setClearMask(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
    else
    {
        osg::notify(osg::NOTICE) << "  GraphicsWindow has not been created successfully." << std::endl;
    }

	model_node = osgDB::readNodeFile(model_file.c_str());
	model_transform = new osg::MatrixTransform;
	model_transform->addChild(model_node.get());
	model_switch = new osg::Switch;
	model_switch->addChild(model_transform.get());
	model_switch->setAllChildrenOff();

	root_group = new osg::Group;
	root_group->addChild(model_switch.get());

	osg::StateSet *state_set = new osg::StateSet;
	state_set->setMode(GL_LIGHTING, osg::StateAttribute::OFF | osg::StateAttribute::OVERRIDE);
	model_node->setStateSet(state_set);
	root_group->setStateSet(state_set);
	
    viewer->getCamera()->setViewport(new osg::Viewport(0, 0, w, h));
    viewer->getCamera()->setGraphicsContext(gc.get());
	viewer->addEventHandler(new PickHandler());
	viewer->setThreadingModel(osgViewer::Viewer::SingleThreaded);

	viewBG = new ViewWithBackGroundImage(viewer, w, h, flip_image, root_group);

	cam.SetCalib(camera_file.c_str(), w, h);
	double p[16];
	cam.GetOpenglProjectionMatrix(p, w, h);
	viewer->getCamera()->setProjectionMatrix(osg::Matrix(p));

	return true;
}



/*
	Clean-up: free memory allocated for the RGB and grayscale images, stop capture.
*/
void CleanUp()
{
	if (gray) 
		cvReleaseImage(&gray);
	if (rgb)  
		cvReleaseImage(&rgb);
	if (capture) 
	{			
		//capture->saveSettings(filename.str());
		capture->stop();
		delete capture;
		capture = 0;
	}
}



/*
	Get pose from marker field.
*/
double GetMultiMarkerPose(IplImage *image, alvar::Camera *cam, alvar::Pose &pose)
{
    static bool init = true;
	bool add_measurement = curr_meas<20;
	bool optimize = !add_measurement && !optimize_done;

    if (init) 
	{
        std::cout << "Using manual multimarker approach with MultiMarkerInitializer and" << std::endl;
        std::cout << "MultiMarkerBundle. Point the camera towards the markers 0-" << nof_markers-1 << std::endl;
		std::cout << " (marker " << pose_marker << " is required, others are optional). " << std::endl;
        std::cout << "20 frames will be acquired." << std::endl;
        init = false;
		std::vector<int> id_vector;
		id_vector.push_back(pose_marker);
        for(int i = 0; i < nof_markers; ++i)
			if( i!=pose_marker )
				id_vector.push_back(i);
        // We make the initialization for MultiMarkerBundle using MultiMarkerInitializer
        // Each marker needs to be visible in at least two images and at most 32 image are used.
		multi_marker_init = new alvar::MultiMarkerInitializer(id_vector, 2, 32);
        pose.Reset();
        multi_marker_init->PointCloudAdd(id_vector[0], marker_size, pose);
        multi_marker_bundle = new alvar::MultiMarkerBundle(id_vector);
    }

    double error = -1;
    if (!optimize_done) 
	{
        if (marker_detector.Detect(image, cam, true, visualize, 0.0)) 
		{
            if (!visualize)
                error = multi_marker_init->Update(marker_detector.markers, cam, pose, image);
            else
                error = multi_marker_init->Update(marker_detector.markers, cam, pose);
        }
    } 
	else 
	{
        if (marker_detector.Detect(image, cam, true, visualize, 0.0))
		{
            if (!visualize)
                error = multi_marker_bundle->Update(marker_detector.markers, cam, pose, image);
            else 
                error = multi_marker_bundle->Update(marker_detector.markers, cam, pose);
            if ((multi_marker_bundle->SetTrackMarkers(marker_detector, cam, pose, image) > 0) &&
                (marker_detector.DetectAdditional(image, cam, !visualize) > 0))
            {
                if (!visualize)
                    error = multi_marker_bundle->Update(marker_detector.markers, cam, pose, image);
                else
                    error = multi_marker_bundle->Update(marker_detector.markers, cam, pose);
            }
        }
    }

    if (add_measurement && every_20th>=20) 
	{
        if (marker_detector.markers->size() >= 2) 
		{
            std::cout << "Adding measurement... (" << curr_meas+1 << "/20) " << std::endl;
            multi_marker_init->MeasurementsAdd(marker_detector.markers);
            add_measurement = false;
			curr_meas++;
        }
		every_20th = 0;
    }
	every_20th++;

    if (optimize) 
	{
        std::cout << "Initializing optimization..." << std::endl;
        if (!multi_marker_init->Initialize(cam)) {
            std::cout << "Initialization failed, this config needs more measurements." << std::endl;

        } 
		else 
		{
            // Reset the bundle adjuster.
            multi_marker_bundle->Reset();
            multi_marker_bundle->MeasurementsReset();
            // Copy all measurements into the bundle adjuster.
            for (int i = 0; i < multi_marker_init->getMeasurementCount(); ++i) 
			{
				alvar::Pose pose;
                multi_marker_init->getMeasurementPose(i, cam, pose);
				const std::vector<alvar::MultiMarkerInitializer::MarkerMeasurement> markers 
                    = multi_marker_init->getMeasurementMarkers(i);
                multi_marker_bundle->MeasurementsAdd(&markers, pose);
            }
            // Initialize the bundle adjuster with initial marker poses.
            multi_marker_bundle->PointCloudCopy(multi_marker_init);
            std::cout << "Optimizing..." << std::endl;
            std::cout << "(this may take more than a minute, please wait...)" << std::endl;
            if (multi_marker_bundle->Optimize(cam, 0.01, 20)) 
			{
                std::cout << "Optimizing done" << std::endl;
                optimize_done = true;
				visualize = false;
            } 
			else 
			{
                std::cout << "Optimizing FAILED!" << std::endl;
            }
        }
        optimize = false;
    }
    return error;
}



/*
	Process each captured video frame.
*/
void Process()
{
	reset = true;
	while (true)
	{
		frame = capture->captureImage();
		if (frame == 0) 
			continue; // TODO: For some reason CvCam gives NULL sometimes?
		if(frame->origin==1) 
		{ 
			cvFlip(frame); 
			frame->origin=0;
		}
		if (frame->nChannels == 1) 
		{
			gray->imageData = frame->imageData;
			cvCvtColor(frame, rgb, CV_BayerGB2RGB); // TODO: Now this assumes Bayer
		} 
		else 
		{
			rgb->imageData = frame->imageData;
			cvCvtColor(frame, gray, CV_RGB2GRAY);
		}
		gray->origin = frame->origin;

		if (reset)
		{
			reset = false;
		}

		alvar::Pose pose;
		double error = GetMultiMarkerPose(frame, &cam, pose);
		bool track_ok = (error>=0.0 && error<5.0);

		if (track_ok)
		{
			// Draw cameras, points & features
			double gl_mat[16];
			pose.GetMatrixGL(gl_mat);
			model_transform->setMatrix(osg::Matrix(gl_mat));
			model_switch->setAllChildrenOn();
		}
		else
		{
			model_switch->setAllChildrenOff();
//			std::cout << "\rTrack Failed     ";
		}

		viewBG->DrawImage(rgb);
		if (viewer->done())
			break;
		viewer->frame();
		if (stop_running) 
			break;
	}
}



int main(int argc, char **argv)
{	
    // Output usage message
    std::cout << "Osg3DMarkerField example" << std::endl;
	std::cout << "==============" << std::endl;
    std::cout << std::endl;
    std::cout << "Description:" << std::endl;
    std::cout << "  This is an example of how to use the 'MultiMarkerBundle' class " << std::endl;
	std::cout << "  to automatically deduce and optimize 'Multimarker' setups." << std::endl;
    std::cout << "  The program deduces the relative poses and positions of markers that have been" << std::endl;
	std::cout << "  laid in any 3D configuration. Then the program uses the deduced marker" << std::endl;
	std::cout << "  configuration for tracking and overlays an OSG model to the scene." << std::endl;
    std::cout << std::endl;
    std::cout << "Possible command line parameters:" << std::endl;
    std::cout << "  <the number of the capture device to use>" << std::endl;
    std::cout << "  <camera calibration file>" <<  std::endl;
    std::cout << "  <pose marker id (0-11)>" << std::endl;
    std::cout << std::endl;
	std::cout << "Keyboard Shortcuts (these work only when AR window is selected):" << std::endl;
    std::cout << "  q: quit" << std::endl;
    std::cout << "  r: reset" << std::endl;
    std::cout << std::endl;

	size_t use_cap = 0;
	std::string calib_file = "calib.xml";
	if (argc > 1) 
		use_cap = atoi(argv[1]);
	if (argc > 2) 
		calib_file = argv[2];
	if (argc > 3) 
	{
		pose_marker = atoi(argv[3]);
		if( pose_marker<0 || pose_marker>=nof_markers )
			pose_marker = 0;
	}

	if (InitVideoCapture(use_cap) && 
		InitImages(_width, _height, _origin) && 
		InitOSG(_width, _height, true, "axes.osg", calib_file)) 
	{
		Process();
		CleanUp();
		return 0;
	}
	CleanUp();
	return -1;
}
