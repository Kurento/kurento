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
#include <iostream>
#include <map>
#include "EC.h"
#include "highgui.h"
using namespace alvar;
using namespace std;

struct Feature {
	int id;
	int type_id;
	bool has_p2d;
	bool has_p3d;
	CvPoint2D32f p2d;
	CvPoint2D32f p3d;
};

class TestTrack : public CxxTest::TestSuite 
{
protected:
	const static int crop_x_res = 320;
	const static int crop_y_res = 240;
	int dx,dy;
	std::map<int, Feature> container;
	IplImage *img;
	IplImage *crop;
	std::string path;
	TrackerFeaturesEC tf;
	void NextCrop(bool init) {
		static int x=0;
		static int y=0;
		if (init) {
			srand(0);
			x = img->width/2 - crop_x_res/2;
			y = img->height/2 - crop_y_res/2;
		}
		int delta=crop_x_res/8;
		x += (rand()%delta)-(delta/2);
		y += (rand()%delta)-(delta/2);
		if (x < 0) x=0;
		if (y < 0) y=0;
		if (x >= img->width-crop_x_res) x = img->width-crop_x_res-1;
		if (y >= img->height-crop_y_res) y = img->height-crop_y_res-1;
		dx = x-(img->width/2 - crop_x_res/2);
		dy = y-(img->height/2 - crop_y_res/2);
		cvSetImageROI(img, cvRect(x,y,crop_x_res,crop_y_res));
		cvCopy(img, crop);
	}
	void ShowCrop() {
		cvNamedWindow("crop");
		std::map<int, Feature>::iterator iter = container.begin();
		while(iter != container.end()) {
			int id = iter->first;
			if (iter->second.has_p2d)
				cvCircle(crop, cvPointFrom32f(iter->second.p2d), 3, CV_RGB((id*7%256),(id*65%256),(256-(id*13%256))));
			//else 
			//	cvCircle(crop, cvPoint(iter->second.p2d.x, iter->second.p2d.y), 5, CV_RGB(0,0,0));
			iter++;
		}
		cvShowImage("crop", crop);
		cvWaitKey(100);
	}
public:
	TestTrack() : tf(200,190,0.01,0,3) {
		path = std::string(ALVAR_TEST_DIR);
		path = path + "/IMG_4617.jpg";
		std::cout<<"Path: "<<path<<std::endl;
		img = cvLoadImage(path.c_str(), CV_LOAD_IMAGE_GRAYSCALE);
		crop = cvCreateImage(cvSize(crop_x_res, crop_y_res), 8, 1);
	}
	~TestTrack() {
		if (img) cvReleaseImage(&img);
		if (crop) cvReleaseImage(&crop);
	}
	void testEc() {
		// TODO: Make some real tests!
		NextCrop(true); 
		tf.Track(crop,0,container);
		tf.AddFeatures(container);
		for (int i=0; i<100; i++) {
			NextCrop(false); 
			tf.Track(crop,0,container);
			tf.AddFeatures(container);
		ShowCrop();
		}
		TS_ASSERT(0 == 0);
	}
};
