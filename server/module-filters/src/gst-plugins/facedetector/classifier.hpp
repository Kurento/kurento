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
#include <opencv2/core/types.hpp>
#include <vector>

namespace cv
{
class Mat;
}

G_BEGIN_DECLS

typedef struct _Classifier Classifier;

void classify_image (Classifier *self,
    const cv::Mat &img,
    std::vector<cv::Rect> &faces);

Classifier *init_classifier (const gchar *file);

bool is_init (Classifier *self);

void delete_classifier (Classifier *self);

G_END_DECLS

#endif /* __CLASSIFIER_H__ */
