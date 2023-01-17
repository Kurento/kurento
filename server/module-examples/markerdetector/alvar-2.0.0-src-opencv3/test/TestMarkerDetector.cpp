/*
 * This file is part of ALVAR, A Library for Virtual and Augmented Reality.
 *
 * Copyright 2007-2012 VTT Technical Research Centre of Finland
 *
 * Contact: VTT Augmented Reality Team <alvar.info@vtt.fi>
 *          <http://www.vtt.fi/multimedia/alvar.html>
 *
 * ALVAR is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ALVAR; if not, see
 * <http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html>.
 */

#include <cxxtest/TestSuite.h>
#include <fstream>
#include "cxcore.h"
#include "highgui.h"
#include "MarkerDetector.h"
#ifdef WIN32
    #include <windows.h>
    #include <GL/gl.h>
    #include <GL/glu.h>
    #include <glut.h>
#else
    #include <GL/gl.h>
    #include <GL/glu.h>
    #include <GL/glut.h>
#endif
using namespace alvar;
using namespace std;

static GLubyte pixels[16][16] = {
	{ 0, 0,   0,   0,   0,   0,   0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0,   0,   0,   0,   0,   0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0, 255, 255, 255,   0, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0,   0,   0, 255, 255,   0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0, 255,   0, 255,   0, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0, 255, 255,   0, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0, 255, 255,   0, 255, 255, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0,   0,   0,   0,   0,   0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0,   0,   0,   0,   0,   0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0,   0,   0,   0,   0,   0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0,   0,   0,   0,   0,   0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0,   0,   0,   0,   0,   0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0,   0,   0,   0,   0,   0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0,   0,   0,   0,   0,   0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0,   0,   0,   0,   0,   0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
	{ 0, 0,   0,   0,   0,   0,   0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
};

class TestMarkerDetector : public CxxTest::TestSuite 
{
protected:
	int win;
	int x_res, y_res, render_mul;

	GLuint texName;
	MarkerDetector<MarkerData> marker_detector;

	// TODO: We should have no pose.Mirror(false, true, true) in here...!!!
	void PrintMatrixDiff(char *title, double *gl1, double *gl2) {
		cout<<title<<" ["<<endl;
		cout<<fixed<<setprecision(4);
		cout<<"\t"<<(gl1[0])<<"\t"<<(gl1[4])<<"\t"<<(gl1[8])<<"\t"<<(gl1[12])<<" | ";
		cout<<"\t"<<(gl2[0]-gl1[0])<<"\t"<<(gl2[4]-gl1[4])<<"\t"<<(gl2[8]-gl1[8])<<"\t"<<(gl1[12]-gl2[12])<<endl;
		cout<<"\t"<<(gl1[1])<<"\t"<<(gl1[5])<<"\t"<<(gl1[9])<<"\t"<<(gl1[13])<<" | ";
		cout<<"\t"<<(gl2[1]-gl1[1])<<"\t"<<(gl2[5]-gl1[5])<<"\t"<<(gl2[9]-gl1[9])<<"\t"<<(gl1[13]-gl2[13])<<endl;
		cout<<"\t"<<(gl1[2])<<"\t"<<(gl1[6])<<"\t"<<(gl1[10])<<"\t"<<(gl1[14])<<" | ";
		cout<<"\t"<<(gl2[2]-gl1[2])<<"\t"<<(gl2[6]-gl1[6])<<"\t"<<(gl2[10]-gl1[10])<<"\t"<<(gl1[14]-gl2[14])<<endl;
		cout<<"\t"<<(gl1[3])<<"\t"<<(gl1[7])<<"\t"<<(gl1[11])<<"\t"<<(gl1[15])<<" | ";
		cout<<"\t"<<(gl2[3]-gl1[3])<<"\t"<<(gl2[7]-gl1[7])<<"\t"<<(gl2[11]-gl1[11])<<"\t"<<(gl1[15]-gl2[15])<<endl;
		cout<<"]"<<endl;
	}
	double RotDiff(double *gl1, double *gl2) {
		return sqrt((gl2[0]-gl1[0])*(gl2[0]-gl1[0]) +
		            (gl2[1]-gl1[1])*(gl2[1]-gl1[1]) +
		            (gl2[2]-gl1[2])*(gl2[2]-gl1[2]) +
		            (gl2[4]-gl1[4])*(gl2[4]-gl1[4]) +
		            (gl2[5]-gl1[5])*(gl2[5]-gl1[5]) +
		            (gl2[6]-gl1[6])*(gl2[6]-gl1[6]) +
		            (gl2[8]-gl1[8])*(gl2[8]-gl1[8]) +
		            (gl2[9]-gl1[9])*(gl2[9]-gl1[9]) +
		            (gl2[10]-gl1[10])*(gl2[10]-gl1[10]));
	}
	double TraDiff(double *gl1, double *gl2) {
		return sqrt((gl2[12]-gl1[12])*(gl2[12]-gl1[12]) +
		            (gl2[13]-gl1[13])*(gl2[13]-gl1[13]) +
		            (gl2[14]-gl1[14])*(gl2[14]-gl1[14]));
	}
	void CreatePose(IplImage *img, double m[16], double p[16],
					GLdouble left, GLdouble right,
					GLdouble bottom, GLdouble top,
					GLdouble zNear, GLdouble zFar,
					GLfloat traX, GLfloat traY, GLfloat traZ,
					GLfloat rotX, GLfloat rotY, GLfloat rotZ,
					GLfloat marker_side_len)
	{
		// Nicest result (For some reason we get middle diagonal with polygon smooth?)
		/*
		glEnable(GL_LINE_SMOOTH);
		glEnable(GL_POLYGON_SMOOTH);
		glEnable(GL_BLEND);
		glDisable(GL_DEPTH_TEST);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glLineWidth(0.5);
		glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);
		glHint(GL_POLYGON_SMOOTH_HINT, GL_NICEST);
		*/

		// Pose
		glViewport(0,0,x_res*render_mul,y_res*render_mul);
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glFrustum(left, right, bottom, top, zNear, zFar);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glTranslatef(traX, traY, traZ);
		glRotatef(rotX,1,0,0);
		glRotatef(rotY,0,1,0);
		glRotatef(rotZ,0,0,1);

		// Init
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
		glBindTexture(GL_TEXTURE_2D, texName);

		// draw something (is it possible to anti-alias this?)
		glEnable(GL_TEXTURE_2D);
		glBegin(GL_QUADS);
			glTexCoord2f(0, 0);           glVertex2f(-(marker_side_len/2), -(marker_side_len/2));
			glTexCoord2f(0, 9./16.);	  glVertex2f(-(marker_side_len/2), (marker_side_len/2));
			glTexCoord2f(9./16., 9./16.); glVertex2f((marker_side_len/2), (marker_side_len/2));
			glTexCoord2f(9./16., 0);      glVertex2f((marker_side_len/2), -(marker_side_len/2));
		glEnd();

		// draw outer edge smoothly with anti-aliasing
		/*
		glBegin(GL_LINE_LOOP);
			glVertex2f(-(marker_side_len/2), -(marker_side_len/2));
			glVertex2f(-(marker_side_len/2), (marker_side_len/2));
			glVertex2f((marker_side_len/2), (marker_side_len/2));
			glVertex2f((marker_side_len/2), -(marker_side_len/2));
		glEnd();
		*/

		img->origin = 1;
		if (render_mul == 1) {
			glReadPixels(0,0,img->width,img->height, GL_RGB, GL_UNSIGNED_BYTE, img->imageData);
		} else {
			IplImage *imgl = cvCreateImage(cvSize(x_res*render_mul, y_res*render_mul), IPL_DEPTH_8U, 3);
			glReadPixels(0,0,imgl->width,imgl->height, GL_RGB, GL_UNSIGNED_BYTE, imgl->imageData);
			cvSmooth(imgl, imgl, CV_BLUR, 3);
			//cvResize(imgl, img);
			cvResize(imgl, img, CV_INTER_AREA);
			cvReleaseImage(&imgl); 
		}
		glGetDoublev(GL_PROJECTION_MATRIX, p);
		glGetDoublev(GL_MODELVIEW_MATRIX, m);
		swapGlBuffer("OpenGL");
	}
	void CheckPose(IplImage *img, int marker_id, float marker_side_len, 
		double zNear, double zFar, double m[16], double p[16], 
		bool detect_pose_grayscale,
		bool lower_left_origin=false) 
	{
		// Load image (and flip it if necessary)
		if ((img->origin?true:false) != lower_left_origin) {
			cvFlip(img);
			img->origin = !img->origin;
		}

		// Create camera
		Camera cam;
		cam.SetOpenglProjectionMatrix(p, img->width, img->height);

		// Check the camera
		double pp[16];
		cam.GetOpenglProjectionMatrix(pp, img->width, img->height, zFar, zNear);
		PrintMatrixDiff("Orig projection matrix | Diff with Camera projection", p, pp);
		for (int i=0; i<16; i++) {
			TS_ASSERT_DELTA(p[i],pp[i],0.001);
		}
		//cam.calib_K_data[0][0] = -cam.calib_K_data[0][0];
		//cam.calib_K_data[1][1] = -cam.calib_K_data[1][1];

		// Project "marker corners" and then detect pose using these ideal corners
		Pose pose;
		pose.SetMatrixGL(m);
		vector<CvPoint3D64f> model_points;
		vector<CvPoint2D64f> image_points;
		image_points.resize(4);
		model_points.push_back(cvPoint3D64f(-(marker_side_len/2), -(marker_side_len/2), 0));
		model_points.push_back(cvPoint3D64f((marker_side_len/2), -(marker_side_len/2), 0));
		model_points.push_back(cvPoint3D64f((marker_side_len/2), (marker_side_len/2), 0));
		model_points.push_back(cvPoint3D64f(-(marker_side_len/2), (marker_side_len/2), 0));
		cam.ProjectPoints(model_points, &pose, image_points);
		cam.CalcExteriorOrientation(model_points, image_points, &pose);

		// Check that the pose calculated with "ideal" marker corners is ok
		double mmm[16];
		pose.GetMatrixGL(mmm);
		PrintMatrixDiff("Orig Modelview matrix | Diff with Modelview with ideal corners", m, mmm);
		for (int i=0; i<16; i++) {
			TS_ASSERT_DELTA(m[i],mmm[i],0.001);
		}

		// Detect marker from the image
		marker_detector.SetMarkerSize(marker_side_len);
		marker_detector.SetOptions(detect_pose_grayscale);
		marker_detector.Detect(img, &cam, false, true);

		// Check the result
		TS_ASSERT(marker_detector.markers->size() == 1);

		if (marker_detector.markers->size() == 1) {
			// Compare the "ideal" marker corners with detected marker corners
			cout<<"Ideal corners <-> Detected corners: ["<<endl;
			for (int i=0; i<4; i++) {
				CvPoint2D64f &ideal_corner = image_points[i];
				PointDouble &detected_corner = marker_detector.markers->at(0).marker_corners_img[i];
				TS_ASSERT_DELTA(ideal_corner.x,detected_corner.x,1.5);
				TS_ASSERT_DELTA(ideal_corner.y,detected_corner.y,1.5);
				cout<<"\t"<<ideal_corner.x-detected_corner.x<<", ";
				cout<<ideal_corner.y-detected_corner.y<<endl;
			}
			cout<<"]"<<endl;

			double mm[16];
			cout<<"Marker id "<<marker_detector.markers->at(0).GetId()<<" <-> "<<marker_id<<endl;
			TS_ASSERT(marker_detector.markers->at(0).GetId() == marker_id);
			marker_detector.markers->at(0).pose.GetMatrixGL(mm);
			PrintMatrixDiff("Orig Modelview matrix | Diff with detected Modelview", m, mm);
			for (int i=0; i<16; i++) {
				// Now we allow 10% error (or 5.0 units if it is bigger?)
				TS_ASSERT_DELTA(m[i],mm[i],max(5.0, abs(0.1*m[i])));
			}
			// Collect difference statistics
			double rot_diff = RotDiff(m, mm);
			double tra_diff = TraDiff(m, mm);

			if (detect_pose_grayscale) {
				static double total_tra_diff=0, total_rot_diff=0;
				static int total_diff_count=0;
				total_rot_diff += rot_diff;
				total_tra_diff += tra_diff;
				total_diff_count++;
				cout<<"Modelview rot/tra diffs: "<<rot_diff<<", "<<tra_diff<<" (gray opt average: "<<total_rot_diff/total_diff_count<<", "<<total_tra_diff/total_diff_count<<")"<<endl;
			} else {
				static double total_tra_diff=0, total_rot_diff=0;
				static int total_diff_count=0;
				total_rot_diff += rot_diff;
				total_tra_diff += tra_diff;
				total_diff_count++;
				cout<<"Modelview rot/tra diffs: "<<rot_diff<<", "<<tra_diff<<" (average: "<<total_rot_diff/total_diff_count<<", "<<total_tra_diff/total_diff_count<<")"<<endl;
			}
		}

		// Show for debug purposes
		/*
		cvNamedWindow("img");
		cvShowImage("img",img);
		cvWaitKey(0);
		*/
	}
	void OpenGLWindow(const char *name, int width, int height) {
		cvNamedWindow(name);
		cvResizeWindow(name, width, height);
		cvMoveWindow(name, 0, 0);
		HWND hWnd = (HWND)cvGetWindowHandle(name);
		HDC hDC = GetDC(hWnd);

		PIXELFORMATDESCRIPTOR pfd;
		ZeroMemory(&pfd, sizeof(pfd));
		pfd.nSize = sizeof(pfd);
		pfd.nVersion = 1;
		pfd.dwFlags = PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL | PFD_DOUBLEBUFFER;
		pfd.iPixelType = PFD_TYPE_RGBA;
		pfd.iLayerType = PFD_MAIN_PLANE;
		pfd.cColorBits = 24;
		pfd.cDepthBits = 16;
		int iFormat = ChoosePixelFormat(hDC, &pfd);
		SetPixelFormat(hDC, iFormat, &pfd);
		HGLRC hRC = wglCreateContext(hDC);
		wglMakeCurrent(hDC, hRC);
	}
	void closeGLWindow(const char *name) {
		HGLRC hRC = wglGetCurrentContext();
		wglMakeCurrent(NULL, NULL);
		wglDeleteContext(hRC);
		cvDestroyWindow(name);
	}
	void swapGlBuffer(const char *name) {
		glFlush();
		SwapBuffers(GetDC((HWND)cvGetWindowHandle(name)));
	}
public:
	TestMarkerDetector() {
		x_res = 320; //1280; //640; 320;
		y_res = 240; // 960;  //480; 240;
		render_mul = 2;

		// initialize opengl
		OpenGLWindow("OpenGL", x_res*render_mul, y_res*render_mul);
		glClearColor(1,1,1,1);
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		glGenTextures(1, &texName);
		glBindTexture(GL_TEXTURE_2D, texName);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE8, 16, 16, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, pixels);
	}
	~TestMarkerDetector() {
		closeGLWindow("OpenGL");
	}
	void testVerybasic() {
		cout<<"\n### Very Basic ###"<<endl;
		double m[16], p[16];
		IplImage *img = cvCreateImage(cvSize(x_res,y_res), IPL_DEPTH_8U, 3);
		CreatePose(img, m, p, 
			-(x_res/2), (x_res/2),
			-(y_res/2), (y_res/2),
			x_res, x_res*4, // zNear, zFar
			0, 0, -x_res*2, 0, 0, 0, y_res);
		CheckPose(img, 1, y_res, x_res, x_res*4, m, p, false, false);
		cvReleaseImage(&img);
	}
	void testBasic() {
		cout<<"\n### Basic ###"<<endl;
		double m[16], p[16];
		IplImage *img = cvCreateImage(cvSize(x_res,y_res), IPL_DEPTH_8U, 3);
		CreatePose(img, m, p, 
			-(x_res/2), (x_res/2),
			-(y_res/2), (y_res/2),
			x_res, x_res*4, // zNear, zFar
			0, 0, -x_res*2, 30, 30, 30, y_res);
		CheckPose(img, 1, y_res, x_res, x_res*4, m, p, false, false);
		cvReleaseImage(&img);
	}
	void testBasicNoise() {
		cout<<"\n### Basic ###"<<endl;
		double m[16], p[16];
		IplImage *img = cvCreateImage(cvSize(x_res,y_res), IPL_DEPTH_8U, 3);
		CreatePose(img, m, p, 
			-(x_res/2), (x_res/2),
			-(y_res/2), (y_res/2),
			x_res, x_res*4, // zNear, zFar
			0, 0, -x_res*2, 30, 30, 130, y_res);

		// Adding noise
		IplImage *noise = cvCreateImage(cvSize(x_res,y_res), IPL_DEPTH_8U, 3);
		CvRNG rng = cvRNG(0x12345);
		cvRandArr(&rng, noise, CV_RAND_UNI, cvScalar(0,0,0), cvScalar(64,64,64));
		cvAdd(img, noise, img);
		cvRandArr(&rng, noise, CV_RAND_UNI, cvScalar(0,0,0), cvScalar(64,64,64));
		cvSub(img, noise, img);
		cvReleaseImage(&noise);

		CheckPose(img, 1, y_res, x_res, x_res*4, m, p, false, false);
		cvReleaseImage(&img);
	}
	void testPrincip() {
		cout<<"\n### Principal point not on center ###"<<endl;
		double m[16], p[16];
		IplImage *img = cvCreateImage(cvSize(x_res,y_res), IPL_DEPTH_8U, 3);
		CreatePose(img, m, p, 
			-(x_res/2)-(y_res/4), (x_res/2)-(y_res/4),
			-(y_res/2)-(y_res/4), (y_res/2)-(y_res/4),
			x_res, x_res*4, // zNear, zFar
			0, 0, -x_res*3, 30, 30, 30, y_res);
		CheckPose(img, 1, y_res, x_res, x_res*4, m, p, false, false);
		cvReleaseImage(&img);
	}
	void testAspect() {
		cout<<"\n### Non-square aspect ratio ###"<<endl;
		double m[16], p[16];
		IplImage *img = cvCreateImage(cvSize(x_res,y_res), IPL_DEPTH_8U, 3);
		CreatePose(img, m, p, 
			-(x_res/2), (x_res/2),
			-(x_res/2), (x_res/2),
			x_res, x_res*4, // zNear, zFar
			0, 0, -x_res*3, 30, 30, 30, y_res);
		CheckPose(img, 1, y_res, x_res, x_res*4, m, p, false, false);
		cvReleaseImage(&img);
	}
	void testMisc1() {
		cout<<"\n### Misc 1 ###"<<endl;
		double m[16], p[16];
		IplImage *img = cvCreateImage(cvSize(x_res,y_res), IPL_DEPTH_8U, 3);
		CreatePose(img, m, p, 
			-(x_res/2)-(y_res/4), (x_res/2)-(y_res/4),
			-(y_res/2)-(y_res/4), (y_res/2)-(y_res/4),
			x_res, x_res*4, // zNear, zFar
			(y_res/5), -(y_res/3), -x_res*3, -30, -30, 30, y_res);
		CheckPose(img, 1, y_res, x_res, x_res*4, m, p, false, false);
		cvReleaseImage(&img);
	}
	void testMisc2() {
		cout<<"\n### Misc 2 ###"<<endl;
		double m[16], p[16];
		IplImage *img = cvCreateImage(cvSize(x_res,y_res), IPL_DEPTH_8U, 3);
		CreatePose(img, m, p, 
			-(x_res/2)+(y_res/4), (x_res/2)+(y_res/4),
			-(y_res/2)+(y_res/6), (y_res/2)+(y_res/6),
			x_res, x_res*4, // zNear, zFar
			-(y_res/5), +(y_res/3), -x_res*3.5, -30, 40, -30, y_res);
		CheckPose(img, 1, y_res, x_res, x_res*4, m, p, false, false);
		cvReleaseImage(&img);
	}
	// TODO: Currently all works only with top-left origin...
	//void testVerybasicFlip() {
	//	cout<<"\n### Very Basic Lower left origin ###"<<endl;
	//	double m[16], p[16];
	//	IplImage *img = cvCreateImage(cvSize(x_res,y_res), IPL_DEPTH_8U, 3);
	//	CreatePose(img, m, p, 
	//		-(x_res/2), (x_res/2),
	//		-(y_res/2), (y_res/2),
	//		x_res, x_res*4, // zNear, zFar
	//		0, 0, -x_res*2, 0, 0, 0, y_res);
	//	CheckPose(img, 1, y_res, x_res, x_res*4, m, p, true);
	//	cvReleaseImage(&img);
	//}
};
