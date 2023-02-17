/*
 * Copyright 2022 Kurento
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "OpenCVExampleOpenCVImpl.hpp"
#include <KurentoException.hpp>

#include <opencv2/core.hpp> // Mat
#include <opencv2/imgproc.hpp> // cvtColor

namespace kurento
{
namespace module
{
namespace opencvexample
{

OpenCVExampleOpenCVImpl::OpenCVExampleOpenCVImpl ()
{
  this->filterType = 0;
  this->edgeValue = 125;
}

/*
 * This function will be called with each new frame. mat variable
 * contains the current frame. You should insert your image processing code
 * here. Any changes in mat, will be sent through the Media Pipeline.
 */
void
OpenCVExampleOpenCVImpl::process (cv::Mat &mat)
{
  cv::Mat matBN (mat.rows, mat.cols, CV_8UC1);
  cv::cvtColor (mat, matBN, cv::COLOR_BGRA2GRAY);

  if (this->filterType == 0) {
    cv::Canny (matBN, matBN, edgeValue, 125);
  }
  cv::cvtColor (matBN, mat, cv::COLOR_GRAY2BGRA);
}

void
OpenCVExampleOpenCVImpl::setFilterType (int filterType)
{
  this->filterType = filterType;
}

void
OpenCVExampleOpenCVImpl::setEdgeThreshold (int edgeValue)
{
  this->edgeValue = edgeValue;
}

} // namespace opencvexample
} // namespace module
} // namespace kurento
