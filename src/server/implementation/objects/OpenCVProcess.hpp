#ifndef __OPEN_CV_PROCESS_HPP__
#define __OPEN_CV_PROCESS_HPP__

#include <opencv2/opencv.hpp>
#include <memory>
#include <MediaObject.hpp>

namespace kurento
{
class OpenCVProcess
{
public:
  virtual void process (cv::Mat &mat) = 0;
protected:
  std::shared_ptr<MediaObject> getSharedPtr() {
    try {
      return dynamic_cast <MediaObject *> (this)->shared_from_this();
    } catch (...) {
      return std::shared_ptr<MediaObject> ();
    }
  }
};
} /* kurento */

#endif /* __OPEN_CV_PROCESS_HPP__ */
