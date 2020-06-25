
/**
	CSimpleWndSizeHandler

	Very simple event handler to catch window resize events

	If you are using some GUI or windowing library (windows, wxWidgets,...)
	you usually do not need this. Other libraries usually provides their
	own OnSize() etc. methods to catch window resizing events. You do, however, need
	to make correct perspective in these cases also 
	(see latter implementation in "case osgGA::GUIEventAdapter::RESIZE:")
*/
class CSimpleWndSizeHandler:public osgViewer::WindowSizeHandler
{
public:
	CSimpleWndSizeHandler(float vX, float vY):
		vidX(vX),
		vidY(vY)
	{};
private:

	bool handle(const osgGA::GUIEventAdapter &ea, osgGA::GUIActionAdapter &aa){
		if (ea.getHandled()) return false;
		switch(ea.getEventType()){
		case osgGA::GUIEventAdapter::RESIZE:
			{
				osgViewer::View* view = dynamic_cast<osgViewer::View*>(&aa);
				if (!view) return false;

				view->getCamera()->setViewport(0,0, ea.getWindowWidth(),ea.getWindowHeight());
				double fovy = 0;
				double aspRatio = 0;
				double foo1, foo2;
				view->getCamera()->getProjectionMatrix().getPerspective(fovy, aspRatio, foo1, foo2);
				view->getCamera()->getProjectionMatrix().makePerspective(fovy, vidX/vidY, foo1, foo2);

			};return true;
		};
		return false;
	};

private:
	float vidX;
	float vidY;
};


/**
	CVideoBG

	Class to handle backgroung image as an texture, including the ortho projection
*/
class CVideoBG
{
public:
	void Init(int x, int y, int flip){
		static osg::ref_ptr<osg::Geometry>  bgGeometry = NULL;
		static osg::ref_ptr<osg::Vec3Array> bgVertices = NULL;
		static osg::ref_ptr<osg::Vec2Array> bgTexCoords = NULL;
		static osg::ref_ptr<osg::Vec4Array> bgColors = NULL;
		static osg::ref_ptr<osg::Vec3Array> bgNormals = NULL;
		static osg::ref_ptr<osg::DrawElementsUInt> bgIndices = NULL;

		mMainGroup = new osg::Group;

		bgGeode = new osg::Geode();
		//bgImage = new osg::Image();
		bgTexture = new osg::Texture2D();
		bgTexture->setResizeNonPowerOfTwoHint(false);

		bgGeometry = new osg::Geometry();

		bgGeode->addDrawable(bgGeometry.get());

		bgVertices = new osg::Vec3Array(4);
		(*bgVertices)[0] = osg::Vec3(0,0,0);
		(*bgVertices)[1] = osg::Vec3(x,0,0);
		(*bgVertices)[2] = osg::Vec3(x,y,0);
		(*bgVertices)[3] = osg::Vec3(0,y,0);
		bgGeometry->setVertexArray(bgVertices.get());

		bgTexCoords = new osg::Vec2Array(4);
        if (flip) {
		    (*bgTexCoords)[0].set(0.0f, 0.0f);
		    (*bgTexCoords)[1].set(1.0f, 0.0f);
		    (*bgTexCoords)[2].set(1.0f, 1.0f);
		    (*bgTexCoords)[3].set(0.0f, 1.0f);
        }
        else {
            (*bgTexCoords)[0].set(0.0f, 1.0f);
		    (*bgTexCoords)[1].set(1.0f, 1.0f);
		    (*bgTexCoords)[2].set(1.0f, 0.0f);
		    (*bgTexCoords)[3].set(0.0f, 0.0f);
        }
		bgGeometry->setTexCoordArray(0, bgTexCoords.get());

		bgNormals = new osg::Vec3Array(1);
		(*bgNormals)[0].set(0.0f, -1.0f, 0.0f);
		bgGeometry->setNormalArray(bgNormals.get());
		bgGeometry->setNormalBinding(osg::Geometry::BIND_OVERALL);

		bgColors = new osg::Vec4Array(1);
		(*bgColors)[0].set(1.0f, 1.0f, 1.0f, 1.0f);
		bgGeometry->setColorArray(bgColors.get());
		bgGeometry->setColorBinding(osg::Geometry::BIND_OVERALL);

		bgIndices = new osg::DrawElementsUInt(osg::PrimitiveSet::QUADS, 0);
		bgIndices->push_back(0);
		bgIndices->push_back(1);
		bgIndices->push_back(2);
		bgIndices->push_back(3);
		bgGeometry->addPrimitiveSet(bgIndices.get());

		osg::StateSet* bgStateset = bgGeometry->getOrCreateStateSet();
		bgStateset->setTextureAttributeAndModes(0, bgTexture.get(), osg::StateAttribute::ON);
		bgStateset->setMode(GL_DEPTH_TEST, osg::StateAttribute::OFF);
		bgStateset->setRenderingHint(osg::StateSet::TRANSPARENT_BIN);
		bgStateset->setMode(GL_LIGHTING, osg::StateAttribute::OFF);
		bgStateset->setRenderBinDetails(-1, "RenderBin");

		bgMatrixTransform = new osg::MatrixTransform;
		bgMatrixTransform->setReferenceFrame(osg::Transform::ABSOLUTE_RF);
		bgMatrixTransform->setMatrix(osg::Matrix::identity());
		bgMatrixTransform->addChild(bgGeode.get());

		bgProjection = new osg::Projection;
		bgProjection->setMatrix(osg::Matrix::ortho2D(0, x, 0, y));
		bgProjection->addChild(bgMatrixTransform.get());
		mMainGroup->addChild(bgProjection.get());
	}

	void SetBGImage(osg::Image *image){bgTexture->setImage(image);};
	osg::Group * GetOSGGroup(){return mMainGroup.get();};

private:

	osg::ref_ptr<osg::Group> mMainGroup;
	osg::ref_ptr<osg::Projection> bgProjection;
	osg::ref_ptr<osg::MatrixTransform> bgMatrixTransform;
	osg::ref_ptr<osg::Geode> bgGeode;
	osg::ref_ptr<osg::Texture2D> bgTexture;
	//osg::ref_ptr<osg::Image> bgImage;
	osg::ref_ptr<osg::Group> mapRoot;
};


/**
	ViewWithBackGroundImage

	Class to handle backgroung image as an texture, including the ortho projection
*/
class ViewWithBackGroundImage
{

private:

	osg::ref_ptr<osg::Geometry>			bgGeometry;
	osg::ref_ptr<osg::Vec3Array>		bgVertices;
	osg::ref_ptr<osg::Vec2Array>		bgTexCoords;
	osg::ref_ptr<osg::Vec4Array>		bgColors;
	osg::ref_ptr<osg::Vec3Array>		bgNormals;
	osg::ref_ptr<osg::DrawElementsUInt> bgIndices;
	osg::ref_ptr<osg::Image>			bgImage;
	osg::ref_ptr<osg::Texture2D>		bgTexture;
	osg::ref_ptr<osg::Geode>			bgGeode;
	osg::ref_ptr<osg::Projection>		bgProjection;
	osg::ref_ptr<osg::MatrixTransform>	bgMatrixTransform;

	osgViewer::View *view;
	osg::Group		*root;
	IplImage		*_image;

	int width;
	int height;
public:

	~ViewWithBackGroundImage()
	{
		if(_image) cvReleaseImage(&_image);
	}

	ViewWithBackGroundImage(osgViewer::View* v, int w, int h, bool flip=false, osg::ref_ptr<osg::Node> node=0)
	{
		view   = v;
		_image = 0;
		width  = w;
		height = h;

		bgGeode = new osg::Geode();
		bgImage = new osg::Image();
		bgTexture = new osg::Texture2D();
		bgTexture->setResizeNonPowerOfTwoHint(false);
		bgGeometry = new osg::Geometry();
		bgGeode->addDrawable(bgGeometry.get());

		bgVertices = new osg::Vec3Array(4);
		(*bgVertices)[0] = osg::Vec3(0,0,0);
		(*bgVertices)[1] = osg::Vec3(width,0,0);
		(*bgVertices)[2] = osg::Vec3(width, height,0);
		(*bgVertices)[3] = osg::Vec3(0,height,0);
		bgGeometry->setVertexArray(bgVertices.get());

		bgTexCoords = new osg::Vec2Array(4);
		if(!flip)
		{
			(*bgTexCoords)[0].set(0.0f, 0.0f);
			(*bgTexCoords)[1].set(1.0f, 0.0f);
			(*bgTexCoords)[2].set(1.0f, 1.0f);
			(*bgTexCoords)[3].set(0.0f, 1.0f);
		}
		else
		{
			(*bgTexCoords)[3].set(0.0f, 0.0f);
			(*bgTexCoords)[2].set(1.0f, 0.0f);
			(*bgTexCoords)[1].set(1.0f, 1.0f);
			(*bgTexCoords)[0].set(0.0f, 1.0f);
		}
		bgGeometry->setTexCoordArray(0, bgTexCoords.get());

		bgNormals = new osg::Vec3Array(1);
		(*bgNormals)[0].set(0.0f, -1.0f, 0.0f);
		bgGeometry->setNormalArray(bgNormals.get());
		bgGeometry->setNormalBinding(osg::Geometry::BIND_OVERALL);

		bgColors = new osg::Vec4Array(1);
		(*bgColors)[0].set(1.0f, 1.0f, 1.0f, 1.0f);
		bgGeometry->setColorArray(bgColors.get());
		bgGeometry->setColorBinding(osg::Geometry::BIND_OVERALL);

		bgIndices = new osg::DrawElementsUInt(osg::PrimitiveSet::QUADS, 0);
		bgIndices->push_back(0);
		bgIndices->push_back(1);
		bgIndices->push_back(2);
		bgIndices->push_back(3);
		bgGeometry->addPrimitiveSet(bgIndices.get());

		osg::StateSet* bgStateset = bgGeometry->getOrCreateStateSet();
		bgStateset->setTextureAttributeAndModes(0, bgTexture.get(), osg::StateAttribute::ON);
		bgStateset->setMode(GL_DEPTH_TEST, osg::StateAttribute::OFF);
		bgStateset->setRenderingHint(osg::StateSet::TRANSPARENT_BIN);
		bgStateset->setMode(GL_LIGHTING, osg::StateAttribute::OFF);
		bgStateset->setRenderBinDetails(-1, "RenderBin");

		bgMatrixTransform = new osg::MatrixTransform;
		bgMatrixTransform->setReferenceFrame(osg::Transform::ABSOLUTE_RF);
		bgMatrixTransform->setMatrix(osg::Matrix::identity());
		bgMatrixTransform->addChild(bgGeode.get());

		bgProjection = new osg::Projection;
		bgProjection->setMatrix(osg::Matrix::ortho2D(0, width, 0, height));
		bgProjection->addChild(bgMatrixTransform.get());

		root = new osg::Group();
		root->addChild(bgProjection.get());
		if(node) root->addChild(node);
		view->setSceneData(root);
	}

	void AddNode(osg::ref_ptr<osg::Node> node)
	{
		root->addChild(node);
	}

	void DrawImage()
	{
		if(!_image) return;
		if(_image->nChannels == 3)
			bgImage->setImage(_image->width, _image->height, 1, 4, GL_BGR, GL_UNSIGNED_BYTE, (unsigned char*)_image->imageData, osg::Image::NO_DELETE);
		else if(_image->nChannels == 1)
			bgImage->setImage(_image->width, _image->height, 1, 4, GL_LUMINANCE, GL_UNSIGNED_BYTE, (unsigned char*)_image->imageData, osg::Image::NO_DELETE);
		bgTexture->setImage(bgImage.get());
	}

	void DrawImage(IplImage* image)
	{
		_image = image;
		DrawImage();
	}

	void SetModelMatrix(double gl_mat[16])
	{
		view->getCamera()->setViewMatrix(osg::Matrix(gl_mat));
	}	
};
