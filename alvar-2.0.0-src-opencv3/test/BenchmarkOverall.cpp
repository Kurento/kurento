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

#include <iostream>
#include <vector>
#include <string>
#include <sstream>

#include "highgui.h"

#include "MarkerDetector.h"

using namespace alvar;
using namespace std;

void usage(const string &application)
{
    cout << "usage: " << application << " id" << endl;
}

void detect(int id, const string &filename, int numberImages, bool tracking)
{
    IplImage *image = cvLoadImage(filename.data());
    cout << "benchmark: " << id
         << ", image size: " << image->width << "x" << image->height
         << ", number images: " << numberImages
         << ", tracking: " << tracking << endl;
         
    vector<IplImage *> images;
    for (int i = 0; i < numberImages; ++i) {
        images.push_back(cvCreateImage(cvGetSize(image), IPL_DEPTH_8U, 3));
        cvCopy(image, images[i]);
    }
    
    Camera camera;
    camera.SetRes(image->width, image->height);
    
    MarkerDetector<MarkerData> markerDetector;
    markerDetector.SetMarkerSize(15);
    
    for (int i = 0; i < 1000; ++i) {
        markerDetector.Detect(images[i % numberImages], &camera, tracking, false);
    }
    
    for (int i = 0; i < numberImages; ++i) {
        cvReleaseImage(&images[i]);
    }
    cvReleaseImage(&image);
    
    cout << "number markers: " << markerDetector.markers->size() << endl;
}

int main(int argc, char *argv[])
{
    vector<string> arguments;
    for (int i = 0; i < argc; ++i) {
        arguments.push_back(argv[i]);
    }
    
    if (argc < 2) {
        usage(arguments.at(0));
        return 1;
    }
    
    bool tracking = false;
    int id;
    for (int i = 1; i < int(arguments.size()); ++i) {
        if (arguments.at(i) == "-t") {
            tracking = true;
        }
        else {
            istringstream stream(arguments.at(1));
            stream >> id;
        }
    }
    
    if (id == 1) {
        detect(id, string("benchmark_image1_small.jpg"), 1, tracking);
    }
    else if (id == 2) {
        detect(id, string("benchmark_image1_small.jpg"), 100, tracking);
    }
    else if (id == 3) {
        detect(id, string("benchmark_image1_medium.jpg"), 1, tracking);
    }
    else if (id == 4) {
        detect(id, string("benchmark_image1_medium.jpg"), 100, tracking);
    }
    else if (id == 5) {
        detect(id, string("benchmark_image1_large.jpg"), 1, tracking);
    }
    else if (id == 6) {
        detect(id, string("benchmark_image1_large.jpg"), 100, tracking);
    }
    else if (id == 7) {
        detect(id, string("benchmark_image2_small.jpg"), 1, tracking);
    }
    else if (id == 8) {
        detect(id, string("benchmark_image2_small.jpg"), 100, tracking);
    }
    else if (id == 9) {
        detect(id, string("benchmark_image2_medium.jpg"), 1, tracking);
    }
    else if (id == 10) {
        detect(id, string("benchmark_image2_medium.jpg"), 100, tracking);
    }
    else if (id == 11) {
        detect(id, string("benchmark_image2_large.jpg"), 1, tracking);
    }
    else if (id == 12) {
        detect(id, string("benchmark_image2_large.jpg"), 100, tracking);
    }
    
    return 0;
}
