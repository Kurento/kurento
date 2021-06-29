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

#ifndef __CLASSIFIER_H__
#define __CLASSIFIER_H__

#include <glib.h>

#include <opencv/cv.h>

G_BEGIN_DECLS

typedef struct _Classifier Classifier;


// Changed classify_image and added some lifecycle managing objects
// to be able to handle Haar Classifier in non "C" API, as it is outdated

void classify_image (Classifier* classifier, IplImage* img, CvSeq* facesList);

Classifier* init_classifier (char* classifier_file);

bool is_inited (Classifier* classifier);

void delete_classifier (Classifier* clasifier);

G_END_DECLS

#endif /* __CLASSIFIER_H__ */
