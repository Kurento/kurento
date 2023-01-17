/**************************************************************************************
* Experimental
*
* Example how to use ALVAR's markerfield (multimarker) with osgViewer,
* including SfM (Structure from motion) algorithm for visual tracking.
* 
* This sample detects marker field (predefined, or command-line parameter) from the view. 
* When the field is detected, system adds the model on top of it. If the field cannot be 
* (after the first detection) the system switches itself to the 
* visual based tracking and tries to keep the model in correct position. As soon the
* field is detected again the system is back on normal state.
* 
* Models are basic OSG models (download the models from the OpenSceneGraph's webpage)
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
#include <SfM.h>
#include <CaptureFactory.h>

// Own includes
#include "Shared.h"



osg::ref_ptr<osgViewer::CompositeViewer> cviewer;
osg::ref_ptr<osg::Node> model_node;
osg::ref_ptr<osg::Node> camera_node;
osg::ref_ptr<osg::Group> origin_node;
osg::ref_ptr<osg::MatrixTransform> camera_transform;
ViewWithBackGroundImage *v1;

alvar::Capture *capture = 0;
alvar::SimpleSfM sfm;
IplImage *frame = 0;
IplImage *gray  = 0;
IplImage *rgb   = 0;
int _width=0, _height=0;
int _origin=0;
std::map<std::string, int> view_map;

bool stop_running  = false;
bool reset		   = false;
bool visualise	   = true;
bool do_sfm		   = false;



class Visualisator
{

private:
	int _n_features;

	osg::Group		*_root;
	osg::Geode      *_geode;
	osg::Geometry   *_geometry;
	osg::Vec3Array  *_array;
	osg::DrawArrays *_da;

	osg::Geometry   *_geometry2;
	osg::Vec3Array  *_array2;
	osg::DrawArrays *_da2;

	osg::StateSet	*_state;
	osg::Material	*_mater;
	osg::StateSet	*_state2;
	osg::Material	*_mater2;


public:
	Visualisator(int n_features)
		: _n_features(n_features)
	{
		_root	   = new osg::Group;
		_geode     = new osg::Geode;
		_geometry  = new osg::Geometry; 
		_geometry2 = new osg::Geometry; 		

		_da2	   = new osg::DrawArrays(osg::PrimitiveSet::LINES, 0, _n_features*2);
		_da		   = new osg::DrawArrays(osg::PrimitiveSet::POINTS, 0, _n_features);
		_array	   = new osg::Vec3Array(_n_features);
		_array2    = new osg::Vec3Array(_n_features*2); 

		_geometry->setVertexArray(_array);
		_geometry->addPrimitiveSet(_da);
		_geometry2->setVertexArray(_array2);
		_geometry2->addPrimitiveSet(_da2);
		_geode->addDrawable(_geometry);
		_geode->addDrawable(_geometry2);

		_state = new osg::StateSet();
		_mater = new osg::Material();

		_state->setMode(GL_BLEND,osg::StateAttribute::ON|osg::StateAttribute::OVERRIDE);
		_mater->setEmission(osg::Material::FRONT_AND_BACK, osg::Vec4(0, 0, 1, 1));	
		_mater->setAlpha(osg::Material::FRONT_AND_BACK, 0.1);
		_state->setAttributeAndModes(_mater,osg::StateAttribute::OVERRIDE |osg::StateAttribute::ON);
		_geometry2->setStateSet(_state);

		_state2 = new osg::StateSet();
		_mater2 = new osg::Material();
		_mater2->setEmission(osg::Material::FRONT_AND_BACK, osg::Vec4(1, 0, 0, 1));
		_state2->setAttribute(_mater2);
		_geometry->setStateSet(_state2);

		_geode->getOrCreateStateSet()->setMode(GL_LIGHTING, osg::StateAttribute::OFF); 

		osg::Point *point=new osg::Point;
		point->setSize(2);
		_root->getOrCreateStateSet()->setAttribute(point); 

		_root->addChild(_geode);
	}

	void Update3DPoints(alvar::Pose &pose, std::map<int,alvar::SimpleSfM::Feature> &fm, bool draw_lines)
	{
		for(int i = 0; i < _n_features ;++i)
		{
			if (!draw_lines) 
				_array->at(i) = osg::Vec3(0,0,0);
			if (draw_lines) 
			{
				_array2->at(2*i+0) = osg::Vec3(0,0,0);
				_array2->at(2*i+1) = osg::Vec3(0,0,0);
			}
		}

		double Od[3];
		CvMat Om = cvMat(3, 1, CV_64F, Od);
		pose.GetTranslation(&Om);

		int ind = 0;
		std::map<int,alvar::SimpleSfM::Feature>::iterator it;
		for (it = fm.begin(); it != fm.end(); it++)
		{
			alvar::SimpleSfM::Feature *f = &(it->second);
			if (f->has_p3d)
			{
				if (!draw_lines) 
					_array->at(ind+0) = osg::Vec3(f->p3d.x, f->p3d.y, f->p3d.z);
				if (draw_lines && f->has_p2d)
				{
					_array2->at(2*ind+0) = osg::Vec3(Od[0],Od[1],Od[2]);
					_array2->at(2*ind+1) = osg::Vec3(f->p3d.x, f->p3d.y, f->p3d.z);
				}
				ind++;
			}
		}
		_geometry->setVertexArray(_array);
		_geometry2->setVertexArray(_array2);
	}

	osg::Node *GetRoot() 
	{
		return _root;
	}	
};

const int N_FEATURES = 200;
Visualisator visualisator(N_FEATURES*2);



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
					case 'q':
						{
							stop_running = true;
							std::cout << "Stopping..." << std::endl;
						} 
						break;
					case 'v':
						{
							visualise = !visualise;
							std::cout << "Visualisation " << (visualise?"ON":"OFF") << std::endl;
						} 
						break;
					case 's':
						{
							do_sfm = !do_sfm;
							std::cout << "SfM Tracker " << (do_sfm?"ON":"OFF") << std::endl;
						} 
						break;
					case 'r':
						{
							reset = true;
							std::cout << "Resetting..." << std::endl;
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
	std::cout<<"Enumerated Capture Devices:"<<std::endl;
	alvar::CaptureFactory::CaptureDeviceVector vec = alvar::CaptureFactory::instance()->enumerateDevices();

	if (vec.size() < 1) 
	{
		std::cout<<"  none found"<<std::endl;
		return false;
	}
	if (use_cap >= vec.size()) 
	{
		std::cout<<"  required index not found - using 0"<<std::endl;
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
	Initialize OSG, create a CompositeViewer with two subwindows (side-by-side).
*/
bool InitOSG(int w, int h, bool flip_image, std::string model_file, std::string camera_file)
{
	int offx = 10;
	int offy = 30;

	cviewer = new osgViewer::CompositeViewer;
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
    traits->width  = w*2;
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

	origin_node = new osg::Group;
	camera_transform = new osg::MatrixTransform;
	model_node = osgDB::readNodeFile(model_file.c_str());
	camera_node = osgDB::readNodeFile("axes.osg");
	camera_transform->addChild(camera_node);
	
	origin_node->addChild(osgDB::readNodeFile("grid_xy.osg"));
	origin_node->addChild(camera_transform);
	origin_node->addChild(visualisator.GetRoot());

	osg::StateSet *state_set = new osg::StateSet;
	state_set->setMode(GL_LIGHTING, osg::StateAttribute::OFF | osg::StateAttribute::OVERRIDE);
	model_node->setStateSet(state_set);
	camera_node->setStateSet(state_set);
	origin_node->setStateSet(state_set);

	double scale = 10.0;
	osg::Matrixd smat(scale, 0, 0, 0, 0, scale, 0, 0, 0, 0, scale, 0, 0, 0, 0, 1);
	osg::MatrixTransform *scalet = new osg::MatrixTransform(smat);
	scalet->addChild(model_node);
	origin_node->addChild(scalet);
	
	// Video+augmentation
	osgViewer::View* view1 = new osgViewer::View;
    view1->setName("View one");
	view_map[view1->getName()] = 0;
    view1->getCamera()->setName("Cam one");
    view1->getCamera()->setViewport(new osg::Viewport(0, 0, w, h));
    view1->getCamera()->setGraphicsContext(gc.get());
	v1 = new ViewWithBackGroundImage(view1, w, h, flip_image, scalet);
	view1->addEventHandler(new PickHandler());

	// VR view
	osgViewer::View* view2 = new osgViewer::View;
	view2->setName("View two");
	view_map[view2->getName()] = 1;
	view2->getCamera()->setGraphicsContext(gc.get());
	view2->setSceneData(origin_node);
    view2->getCamera()->setName("Cam two");
	view2->getCamera()->setViewport(new osg::Viewport(w, 0, w, h));
	view2->setCameraManipulator(new osgGA::TrackballManipulator);

	cviewer->addView(view1);
	cviewer->addView(view2);
	cviewer->setThreadingModel(osgViewer::Viewer::SingleThreaded);
	
	alvar::Camera cam;
	cam.SetCalib(camera_file.c_str(), w, h);
	double p[16];
	cam.GetOpenglProjectionMatrix(p, w, h);
	view1->getCamera()->setProjectionMatrix(osg::Matrix(p));
	view2->getCamera()->setProjectionMatrix(osg::Matrix(p));

	return true;
}



/*
	Initialize ALVAR: Set calibration file, set multimarker file.
*/
bool InitALVAR(int w, int h, std::string calib_file, std::string multi_marker_file)
{
	sfm.GetCamera()->SetCalib(calib_file.c_str(), w, h);
	sfm.Clear();
	sfm.AddMultiMarker(multi_marker_file.c_str(), alvar::FILE_FORMAT_XML);
	sfm.SetResetPoint();
	//sfm.SetMultiMarkerFile(multi_marker_file);
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

		static bool do_sfm_old = do_sfm;
		if (do_sfm != do_sfm_old) 
			reset = true;
		do_sfm_old = do_sfm;
		if (reset)
		{
			sfm.SetScale(10.0);
			sfm.Reset();
			reset = false;
		}

		bool track_ok = false;
		if (do_sfm)
			track_ok = sfm.Update(gray, false, true, 7.f, 22.5f);
		else
			track_ok = sfm.UpdateRotationsOnly(gray);

		if (track_ok)
		{
			// Draw cameras, points & features
			alvar::Pose p = *(sfm.GetPose());
			double gl_mat[16];
			p.GetMatrixGL(gl_mat);
			osg::Matrixd inv_mat = osg::Matrixd::inverse(osg::Matrixd(gl_mat));
			v1->SetModelMatrix(gl_mat);	
			camera_transform->setMatrix(inv_mat);
			alvar::Pose pose_inv = p; pose_inv.Invert();
			if (do_sfm)
			{
				visualisator.Update3DPoints(pose_inv, sfm.container_triangulated, false);
				visualisator.Update3DPoints(pose_inv, sfm.container, true);
			}
		}

		if (visualise) 
			sfm.Draw(rgb);
		v1->DrawImage(rgb);
		if (cviewer->done())
			break;
		cviewer->frame();
		if (stop_running) 
			break;
	}
}



int main(int argc, char **argv)
{	
    // Output usage message
    std::cout << "OSGSfM example" << std::endl;
	std::cout << "==============" << std::endl;
    std::cout << std::endl;
    std::cout << "Description:" << std::endl;
    std::cout << "  This is an example of how to use the 'SimpleSfM' class. 'SimpleSfM' extends" << std::endl;
	std::cout << "  the multi marker tracking. Use 's' to toggle between two possible options:" << std::endl;
    std::cout << "  1) The first option assumes that when the markers are not visible the camera" << std::endl;
	std::cout << "  is only rotating. Image features are used to update rotation parameters." << std::endl;
    std::cout << "  2) The second option uses structure from motion (SfM) technique to estimate all" << std::endl;
    std::cout << "  the camera parameters during tracking. The camera needs to moved in 3D for" << std::endl;
    std::cout << "  this option to work properly." << std::endl;
	std::cout << std::endl;
    std::cout << "  To be able to use this software, one must calibrate the camera using" << std::endl;
	std::cout << "  'SampleCamCalib' included in ALVAR. Additionally the multi marker has to be" << std::endl;
	std::cout << "  defined either manually or using 'SampleMultiMarkerBundle'." << std::endl;
	std::cout << "  The default marker setup 'multimarker.xml' can be printed from 'ALVAR.pdf'." << std::endl;
	std::cout << "  For the best user experience you need to finetune your camera for fast" << std::endl;
	std::cout << "  framerate without motion blur (fast exposure compensated with gain)." << std::endl;
    std::cout << std::endl;
    std::cout << "Possible command line parameters:" << std::endl;
    std::cout << "  <the number of the capture device to use>" << std::endl;
    std::cout << "  <camera calibration file>" <<  std::endl;
    std::cout << "  <multi marker calibration file>" << std::endl;
    std::cout << std::endl;
	std::cout << "Keyboard Shortcuts (these work only when AR window is selected):" << std::endl;
    std::cout << "  v: visualization on/off" << std::endl;
	std::cout << "  r: reset tracker" << std::endl;
	std::cout << "  s: switch between tracker types (rotation only/structure from motion)" << std::endl;
    std::cout << "  q: quit" << std::endl;
    std::cout << std::endl;

	size_t use_cap = 0;
	std::string calib_file = "calib.xml";
	std::string mmarker_file = "multimarker.xml";
	if (argc > 1) 
		use_cap = atoi(argv[1]);
	if (argc > 2) 
		calib_file = argv[2];
	if (argc > 3) 
		mmarker_file = argv[3];

	if (InitVideoCapture(use_cap) && 
		InitImages(_width, _height, _origin) && 
		InitOSG(_width, _height, true, "axes.osg", calib_file) &&
		InitALVAR(_width, _height, calib_file, mmarker_file)) 
	{
		Process();
		CleanUp();
		return 0;
	}
	CleanUp();
	return -1;
}
