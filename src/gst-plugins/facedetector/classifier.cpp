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

#include <string>

#include "classifier.h"

#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/imgproc/imgproc.hpp>

using namespace cv;
class _Classifier
{
public:
  _Classifier (const std::string &filename);
  ~_Classifier() = default;

  bool is_loaded ();

  CascadeClassifier face_cascade;
  std::string filename;
};


_Classifier::_Classifier(const std::string &filename)
{
  if (face_cascade.load ( filename )) {
	  this->filename = filename;
  }
}

bool _Classifier::is_loaded ()
{
	return !face_cascade.empty();
}

void classify_image (Classifier* classifier, IplImage *img, CvSeq *facesList)
{
  std::vector<Rect> faces;
  Mat frame (cv::cvarrToMat(img));
  Mat frame_gray;

  cvtColor ( frame, frame_gray, COLOR_BGR2GRAY );
  equalizeHist ( frame_gray, frame_gray );

  classifier->face_cascade.detectMultiScale ( frame_gray, faces, 1.2, 3, 0,
      Size (frame.cols / 20, frame.rows / 20),
      Size (frame.cols / 2, frame.rows / 2) );

  for (auto &face : faces) {
    CvRect aux = cvRect(face.x, face.y, face.width, face.height);
    cvSeqPush (facesList, &aux);
  }

  faces.clear();
}


Classifier* init_classifier (char* classifier_file)
{
	Classifier *pClassifier = new Classifier (classifier_file);

	if (pClassifier != NULL) {
		if (pClassifier->is_loaded())
			return pClassifier;
		else
			delete pClassifier;

	}

	return NULL;
}

bool is_inited (Classifier* classifier)
{
	if (classifier->is_loaded())
		return true;
	return false;
}


void delete_classifier (Classifier* classifier)
{
	delete classifier;
}

