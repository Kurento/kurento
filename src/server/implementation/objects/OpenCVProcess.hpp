#ifndef __OPEN_CV_PROCESS_HPP__
#define __OPEN_CV_PROCESS_HPP__

#include <opencv2/opencv.hpp>

namespace kurento
{
class OpenCVProcess
{
public:
  virtual void process (cv::Mat &mat) = 0;
};
} /* kurento */

#endif /* __OPEN_CV_PROCESS_HPP__ */
