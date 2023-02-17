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

#include "classifier.hpp"

#include <opencv2/core.hpp> // Mat
#include <opencv2/imgproc.hpp> // cvtColor
#include <opencv2/objdetect.hpp> // CascadeClassifier

class _Classifier
{
public:
  _Classifier (const std::string &filename);
  ~_Classifier () = default;

  bool is_loaded ();

  cv::CascadeClassifier face_cascade;
  std::string filename;
};

_Classifier::_Classifier (const std::string &filename)
{
  if (face_cascade.load (filename)) {
    this->filename = filename;
  }
}

bool
_Classifier::is_loaded ()
{
  return !face_cascade.empty ();
}

void
classify_image (Classifier *self,
    const cv::Mat &img,
    std::vector<cv::Rect> &faces)
{
  cv::Mat img_gray;

  cv::cvtColor (img, img_gray, cv::COLOR_BGR2GRAY);

  cv::equalizeHist (img_gray, img_gray);

  self->face_cascade.detectMultiScale (img_gray, faces, 1.2, 3, 0,
      cv::Size (img.cols / 20, img.rows / 20),
      cv::Size (img.cols / 2, img.rows / 2));
}

Classifier *
init_classifier (const gchar *file)
{
  Classifier *self = new Classifier (file);

  if (self != NULL) {
    if (self->is_loaded ())
      return self;
    else
      delete self;
  }

  return NULL;
}

bool
is_init (Classifier *self)
{
  return self->is_loaded ();
}

void
delete_classifier (Classifier *self)
{
  delete self;
}
