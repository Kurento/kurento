#ifndef __PROCESS_H__
#define __PROCESS_H__

#include <glib.h>
#include "pthread.h"
#include <opencv/cv.h>
#include <map>

G_BEGIN_DECLS

class ArProcess {
protected:
  int mShowDebugLevel;
  pthread_mutex_t mMutex;
  std::string overlay_image;
  std::string overlay_text;
  float overlayScale;
  cv::Mat overlay;
  void *owndata;
  cv::Mat readImage(std::string url);
public:
  std::map<int, int> detectedMarkers; // marker_id, count (>0 visible)
  std::map<int, int> detectedMarkersPrev;
  ArProcess();
  ~ArProcess();
  int detect_marker(IplImage* img);
  bool set_overlay(const char *_overlay_image, const char *_overlay_text);
  std::string get_overlay_image() const { return overlay_image; } 
  std::string get_overlay_text() const { return overlay_text; } 
  float set_overlay_scale(float _overlayScale);
  float get_overlay_scale() const { return overlayScale; }
  void setShowDebugLevel(int level) { mShowDebugLevel = level; }
  int getShowDebugLevel() { return mShowDebugLevel; }
};

G_END_DECLS

#endif /* __PROCESS_H__ */
