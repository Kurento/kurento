/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
  ~Classifier() = default;

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
  Mat frame (cv::cvarrToMat(img));
  Mat frame_gray;

  cvtColor ( frame, frame_gray, COLOR_BGR2GRAY );
  equalizeHist ( frame_gray, frame_gray );

  lbpClassifier.face_cascade.detectMultiScale ( frame_gray, faces, 1.2, 3, 0,
      Size (frame.cols / 20, frame.rows / 20),
      Size (frame.cols / 2, frame.rows / 2) );

  for (auto &face : faces) {
    CvRect aux = cvRect(face.x, face.y, face.width, face.height);
    cvSeqPush (facesList, &aux);
  }

  faces.clear();
}
