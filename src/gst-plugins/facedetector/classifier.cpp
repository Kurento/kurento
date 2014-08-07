/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

#include "classifier.h"

#define FACE_CASCADE "/usr/share/opencv/lbpcascades/lbpcascade_frontalface.xml"

#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/imgproc/imgproc.hpp>

using namespace cv;
class Classifier
{
public:
  Classifier ();
  ~Classifier() {};

  CascadeClassifier face_cascade;
};

Classifier::Classifier()
{
  face_cascade.load ( FACE_CASCADE );
}

static Classifier lbpClassifier = Classifier ();

void classify_image (IplImage *img, CvSeq *facesList)
{
  std::vector<Rect> faces;
  Mat frame (img);
  Mat frame_gray;

  cvtColor ( frame, frame_gray, COLOR_BGR2GRAY );
  equalizeHist ( frame_gray, frame_gray );

  lbpClassifier.face_cascade.detectMultiScale ( frame_gray, faces, 1.2, 3, 0,
      Size (frame.cols / 20, frame.rows / 20),
      Size (frame.cols / 2, frame.rows / 2) );

  for ( size_t i = 0; i < faces.size(); i++ ) {
    CvRect aux = cvRect (faces[i].x, faces[i].y, faces[i].width, faces[i].height);
    cvSeqPush (facesList, &aux);
  }

  faces.clear();
}
