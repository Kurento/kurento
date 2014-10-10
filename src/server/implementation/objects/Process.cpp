#include "Process.h"

#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include "MarkerDetector.h"

void addFgWithAlpha(cv::Mat &bg, cv::Mat &fg) {
  if (fg.channels() < 4) {
    fg.copyTo(bg);  
    return;
  }
  std::vector<cv::Mat> splitted_bg, splitted_fg;
  cv::split(bg, splitted_bg);
  cv::split(fg, splitted_fg);
  cv::Mat mask = splitted_fg[3];
  cv::Mat invmask = ~mask;
  splitted_bg[0] = (splitted_bg[0] - mask) + (splitted_fg[0] - invmask);
  splitted_bg[1] = (splitted_bg[1] - mask) + (splitted_fg[1] - invmask);
  splitted_bg[2] = (splitted_bg[2] - mask) + (splitted_fg[2] - invmask);
  if (bg.channels() > 3) {
    splitted_bg[3] = splitted_bg[3] + splitted_fg[3];
  }
  cv::merge(splitted_bg, bg);
}

cv::Mat overlay;
int set_overlay(const char *overlay_image, const char *overlay_text) {
  cv::Mat fg, bg;
  if (overlay_image && strlen(overlay_image) > 0) {
    bg = cv::imread(overlay_image, CV_LOAD_IMAGE_UNCHANGED);
    if (!bg.data) {
      bg = cv::Mat(256,256,CV_8UC3);
    }
    if (bg.channels() == 3) cv::cvtColor(bg, bg, CV_BGR2BGRA);
  }
  if (overlay_text && strlen(overlay_text) > 0) {
    int font = cv::FONT_HERSHEY_PLAIN, font_thickness = 3;
    double font_scale = 6.0;
    int baseline=0;
    cv::Size textSize = cv::getTextSize(overlay_text, font, font_scale, font_thickness, &baseline);
    fg = cv::Mat(textSize.height+baseline+4, textSize.width+4, CV_8UC4); // TODO: CV_8UC4
    fg.setTo(cv::Scalar(255,255,255,64));
    cv::putText(fg, overlay_text, cv::Point(2, fg.rows-(baseline/2)), font, font_scale, cv::Scalar(0,64,0,255), font_thickness);
  }
  int res = 0;
  if (bg.data) res = std::max(bg.cols, bg.rows);
  if (fg.data) res = std::max(res, std::max(fg.cols, fg.rows));
  if (res < 1) return -1;
  overlay = cv::Mat(res, res, CV_8UC4);
  overlay.setTo(cv::Scalar(255,255,255,0));
  if (bg.data) {
    double fx = res/std::max(bg.cols, bg.rows), fy = fx;
    cv::resize(bg, bg, cv::Size(), fx, fy);
    cv::Mat overlay_bg(overlay, cv::Rect(
      (overlay.cols-bg.cols)/2, (overlay.rows-bg.rows)/2,
      bg.cols, bg.rows));
    bg.copyTo(overlay_bg);
  }
  if (fg.data) {
    double fx = res/std::max(fg.cols, fg.rows), fy = fx;
    cv::resize(fg, fg, cv::Size(), fx, fy);
    cv::Mat overlay_fg(overlay, cv::Rect(
      (overlay.cols-fg.cols)/2, (overlay.rows-fg.rows)/2,
      fg.cols, fg.rows));
    addFgWithAlpha(overlay_fg, fg);
  }
}

int detect_marker(IplImage *image, gboolean show_debug_info) {
  alvar::Camera cam;
  cam.SetRes(image->width, image->height);
  static alvar::MarkerDetector<alvar::MarkerData> marker_detector;
  marker_detector.SetMarkerSize(15);
  marker_detector.Detect(image, &cam, true, show_debug_info);

  //if (!overlay.data) {
  //  set_overlay("/tmp/overlay.png", "Test");
  //}

  // Show overlay if we have any
  if (!overlay.data) return 0;
  for (size_t i=0; i<marker_detector.markers->size(); i++) {
    if (i >= 32) break;

    cv::Point2f source_points[4];
    cv::Point2f dest_points[4];
    source_points[0] = cv::Point2f(0,overlay.rows-1);
    source_points[1] = cv::Point2f(overlay.cols-1,overlay.rows-1);
    source_points[2] = cv::Point2f(overlay.cols-1,0);
    source_points[3] = cv::Point2f(0,0);
    dest_points[0] = cv::Point2f(
      (*(marker_detector.markers))[i].marker_corners_img[0].x,
      (*(marker_detector.markers))[i].marker_corners_img[0].y);
    dest_points[1] = cv::Point2f(
      (*(marker_detector.markers))[i].marker_corners_img[1].x,
      (*(marker_detector.markers))[i].marker_corners_img[1].y);
    dest_points[2] = cv::Point2f(
      (*(marker_detector.markers))[i].marker_corners_img[2].x,
      (*(marker_detector.markers))[i].marker_corners_img[2].y);
    dest_points[3] = cv::Point2f(
      (*(marker_detector.markers))[i].marker_corners_img[3].x,
      (*(marker_detector.markers))[i].marker_corners_img[3].y);

    cv::Mat transform = cv::getPerspectiveTransform(source_points, dest_points);
    cv::Mat warped_overlay(image->height, image->width, CV_8UC4);
    warped_overlay.setTo(cv::Scalar(0,0,0,0));
    cv::warpPerspective(overlay, warped_overlay, transform, cv::Size(warped_overlay.cols, warped_overlay.rows), 0, cv::BORDER_TRANSPARENT); 

    cv::Mat frame(image);
    addFgWithAlpha(frame, warped_overlay);
  }
  return marker_detector.markers->size();
}
