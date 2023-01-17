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
#ifdef HAVE_CONFIG_H
#include "config.h"
#endif
#include <gst/gst.h>
#include "kmscrowddetector.h"
#include <opencv/cv.h>
#include <opencv/highgui.h>
#include <math.h>

// FIXME: Compatibility between OpenCV 2.x and 3.x
#include <opencv2/core/version.hpp>
#if CV_MAJOR_VERSION > 2
#include <opencv2/imgcodecs/imgcodecs_c.h>
int
cvRound (double value)
{
  return (ceil (value));
}
#endif

#define PLUGIN_NAME "crowddetector"
#define LBPS_ADD_RATIO ((float) 0.4)
#define BACKGROUND_ADD_RATIO ((float) 0.98)
#define TEMPORAL_LBPS_ADD_RATIO ((float) 0.8)
#define EDGES_ADD_RATIO ((float) 0.985)
#define IMAGE_FUSION_ADD_RATIO ((float) 0.55)
#define RESULTS_ADD_RATIO ((float) 0.6)
#define NEIGHBORS ((int) 8)
#define GRAY_THRESHOLD_VALUE ((int) 45)
#define EDGE_THRESHOLD ((int) 45)
#define NUMBER_FEATURES_OPTICAL_FLOW ((int) 400)
#define WINDOW_SIZE_OPTICAL_FLOW ((int) 5)
#define MAX_ITER_OPTICAL_FLOW ((int) 20)
#define EPSILON_OPTICAL_FLOW ((float) 0.3)
#define HARRIS_DETECTOR_K ((float) 0.04)
#define USE_HARRIS_DETECTOR (FALSE)
#define BLOCK_SIZE ((int) 5)
#define QUALITY_LEVEL ((float) 0.01)
#define MIN_DISTANCE ((float) 0.01)
#define HYPOTENUSE_THRESHOLD ((float) 3.0)
#define RADIAN_TO_DEGREE ((float) 57.3)

#define DEFAULT_MAX_WIDTH 320

GST_DEBUG_CATEGORY_STATIC (kms_crowd_detector_debug_category);
#define GST_CAT_DEFAULT kms_crowd_detector_debug_category

#define KMS_CROWD_DETECTOR_GET_PRIVATE(obj) (   \
  G_TYPE_INSTANCE_GET_PRIVATE (                 \
    (obj),                                      \
    KMS_TYPE_CROWD_DETECTOR,                    \
    KmsCrowdDetectorPrivate                     \
  )                                             \
)

#define KMS_CROWD_DETECTOR_LOCK(obj) (                           \
  g_rec_mutex_lock (&KMS_CROWD_DETECTOR (obj)->priv->mutex)   \
)

#define KMS_CROWD_DETECTOR_UNLOCK(obj) (                         \
  g_rec_mutex_unlock (&KMS_CROWD_DETECTOR (obj)->priv->mutex) \
)

typedef struct _RoiData
{
  gchar *name;
  int n_pixels_roi;
  int actual_occupation_level;
  int potential_occupation_level;
  int num_frames_potential_occupancy_level;
  int actual_fluidity_level;
  int potential_fluidity_level;
  int num_frames_potential_fluidity_level;
  int occupancy_level_min;
  int occupancy_level_med;
  int occupancy_level_max;
  int occupancy_num_frames_to_event;
  int fluidity_level_min;
  int fluidity_level_med;
  int fluidity_level_max;
  int fluidity_num_frames_to_event;
  gboolean send_optical_flow_event;
  int actual_optical_flow_angle;
  int potential_optical_flow_angle;
  int num_frames_potential_optical_flow_angle;
  int num_frames_reset_optical_flow_angle;
  int optical_flow_num_frames_to_event;
  int optical_flow_num_frames_to_reset;
  int optical_flow_angle_offset;
} RoiData;

struct _KmsCrowdDetectorPrivate
{
  IplImage *actual_image, *previous_lbp, *frame_previous_gray, *background,
      *acumulated_edges, *acumulated_lbp, *previous_image, *original_image;
  gboolean show_debug_info;
  int num_rois;
  CvPoint **curves;
  CvPoint **curves_original;
  CvPoint2D32f **curves_percentages;
  int *n_points;
  RoiData *rois_data;
  GstStructure *rois;
  gboolean pixels_rois_counted;
  int image_width;
  int image_height;
  gdouble resize_factor;
  int original_image_width;
  int original_image_height;
  int processing_width;
  GRecMutex mutex;
};

enum
{
  PROP_0,
  PROP_SHOW_DEBUG_INFO,
  PROP_ROIS,
  PROP_PROCESSING_WIDTH,
  N_PROPERTIES
};

/* pad templates */

#define VIDEO_SRC_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

#define VIDEO_SINK_CAPS \
    GST_VIDEO_CAPS_MAKE("{ BGR }")

/* class initialization */

G_DEFINE_TYPE_WITH_CODE (KmsCrowdDetector, kms_crowd_detector,
    GST_TYPE_VIDEO_FILTER,
    GST_DEBUG_CATEGORY_INIT (kms_crowd_detector_debug_category, PLUGIN_NAME,
        0, "debug category for crowddetector element"));

static void
kms_crowd_detector_send_message_occupacy (KmsCrowdDetector * crowddetector,
    double occupation_percentage, int curve)
{
  GstStructure *s;
  GstMessage *m;

  s = gst_structure_new ("occupancy-event",
      "roi", G_TYPE_STRING, crowddetector->priv->rois_data[curve].name,
      "occupancy_percentage", G_TYPE_DOUBLE, occupation_percentage,
      "occupancy_level", G_TYPE_INT,
      crowddetector->priv->rois_data[curve].actual_occupation_level, NULL);
  m = gst_message_new_element (GST_OBJECT (crowddetector), s);
  gst_element_post_message (GST_ELEMENT (crowddetector), m);
}

static void
kms_crowd_detector_send_message_fluidity (KmsCrowdDetector * crowddetector,
    double fluidity_percentage, int curve)
{
  GstStructure *s;
  GstMessage *m;

  s = gst_structure_new ("fluidity-event",
      "roi", G_TYPE_STRING, crowddetector->priv->rois_data[curve].name,
      "fluidity_percentage", G_TYPE_DOUBLE, fluidity_percentage,
      "fluidity_level", G_TYPE_INT,
      crowddetector->priv->rois_data[curve].actual_fluidity_level, NULL);
  m = gst_message_new_element (GST_OBJECT (crowddetector), s);
  gst_element_post_message (GST_ELEMENT (crowddetector), m);
}

static void
kms_crowd_detector_send_message_direction (KmsCrowdDetector * crowddetector,
    double angle, int curve)
{
  GstStructure *s;
  GstMessage *m;

  s = gst_structure_new ("direction-event",
      "roi", G_TYPE_STRING, crowddetector->priv->rois_data[curve].name,
      "direction_angle", G_TYPE_DOUBLE, angle, NULL);
  m = gst_message_new_element (GST_OBJECT (crowddetector), s);
  gst_element_post_message (GST_ELEMENT (crowddetector), m);
}

static void
kms_crowd_detector_analyze_optical_flow_angle (KmsCrowdDetector *
    self, double angle, int curve)
{
  if (self->priv->rois_data[curve].actual_optical_flow_angle != angle) {
    if (self->priv->rois_data[curve].potential_optical_flow_angle == angle) {
      self->priv->rois_data[curve].num_frames_potential_optical_flow_angle++;
    } else {
      self->priv->rois_data[curve].potential_optical_flow_angle = angle;
      self->priv->rois_data[curve].num_frames_potential_optical_flow_angle = 1;
    }
  }

  if (self->priv->rois_data[curve].num_frames_potential_optical_flow_angle >
      self->priv->rois_data[curve].optical_flow_num_frames_to_event) {
    GST_DEBUG ("%s --- direction:%f", self->priv->rois_data[curve].name, angle);
    kms_crowd_detector_send_message_direction (self, angle, curve);
    self->priv->rois_data[curve].actual_optical_flow_angle = angle;
    self->priv->rois_data[curve].num_frames_potential_optical_flow_angle = 1;
  }
}

static void
kms_crowd_detector_compute_roi_direction_vector (KmsCrowdDetector *
    self, int number_of_features,
    char optical_flow_found_feature[NUMBER_FEATURES_OPTICAL_FLOW],
    CvPoint2D32f frame1_features[NUMBER_FEATURES_OPTICAL_FLOW],
    CvPoint2D32f frame2_features[NUMBER_FEATURES_OPTICAL_FLOW], CvScalar Color,
    IplImage * binary_actual_motion, CvRect container, int curve)
{
  CvPoint p, q;
  double hypotenuse;
  double angle = -1;
  int it;
  int total_x = 0;
  int total_y = 0;
  int total_counter = 0;
  int offset = self->priv->rois_data[curve].optical_flow_angle_offset;

  for (it = 0; it < number_of_features; it++) {
    if (optical_flow_found_feature[it] == 0) {
      continue;
    }

    if (((int) frame1_features[it].x < 0)
        || ((int) frame1_features[it].x > container.width)
        || ((int) frame1_features[it].y < 0)
        || ((int) frame1_features[it].y > container.height)) {
      continue;
    }

    p.x = (int) frame1_features[it].x;
    p.y = (int) frame1_features[it].y;
    q.x = (int) frame2_features[it].x;
    q.y = (int) frame2_features[it].y;

    hypotenuse = sqrt (pow ((p.y - q.y), 2) + pow ((p.x - q.x), 2));

    if ((*(uchar *) (binary_actual_motion->imageData +
                (p.y + container.y) * binary_actual_motion->widthStep +
                (p.x + container.x) * 1) == 255)
        && (hypotenuse > HYPOTENUSE_THRESHOLD)) {
      total_x = total_x + (q.x - p.x);
      total_y = total_y + (q.y - p.y);
      total_counter++;
    }
  }

  if (total_counter > 0) {
    cvLine (self->priv->actual_image,
        cvPoint (container.width / 2, container.height / 2),
        cvPoint (container.width / 2 + total_x, container.height / 2 + total_y),
        CV_RGB (0, 255, 0), 6, CV_AA, 0);
  }

  cvCircle (self->priv->actual_image,
      cvPoint (container.width / 2, container.height / 2),
      5, CV_RGB (0, 255, 0), 2, 5, 0);

  angle = atan2 (total_y, total_x) * RADIAN_TO_DEGREE;

  if (angle > 0)
    angle = 360 - angle;
  else
    angle = abs (angle);

  if (angle > 0) {
    if (angle >= 45 + offset && angle < 135 + offset)
      angle = 90 + offset;
    else if (angle >= 135 + offset && angle < 225 + offset)
      angle = 180 + offset;
    else if (angle >= 225 + offset && angle < 315 + offset)
      angle = 270 + offset;
    else if ((angle >= 315 + offset && angle < 360 + offset) ||
        (angle >= 0 + offset && angle < 45 + offset))
      angle = 0 + offset;
    kms_crowd_detector_analyze_optical_flow_angle (self, angle, curve);
    self->priv->rois_data[curve].num_frames_reset_optical_flow_angle = 0;
  } else {
    self->priv->rois_data[curve].num_frames_reset_optical_flow_angle++;
    if (self->priv->rois_data[curve].num_frames_reset_optical_flow_angle >
        self->priv->rois_data[curve].optical_flow_num_frames_to_reset) {
      self->priv->rois_data[curve].actual_optical_flow_angle = -1;
      self->priv->rois_data[curve].potential_optical_flow_angle = -1;
    }
  }
}

static void
kms_crowd_detector_compute_optical_flow (KmsCrowdDetector * crowddetector,
    IplImage * binary_actual_motion, CvRect container, int curve)
{
  IplImage *eig_image;
  IplImage *temp_image;
  IplImage *frame1_1C;
  IplImage *frame2_1C;
  IplImage *pyramid1;
  IplImage *pyramid2;
  CvSize frame_size;
  CvPoint2D32f frame2_features[NUMBER_FEATURES_OPTICAL_FLOW];
  char optical_flow_found_feature[NUMBER_FEATURES_OPTICAL_FLOW];
  float optical_flow_feature_error[NUMBER_FEATURES_OPTICAL_FLOW];
  CvPoint2D32f frame1_features[NUMBER_FEATURES_OPTICAL_FLOW];
  int number_of_features = NUMBER_FEATURES_OPTICAL_FLOW;
  CvSize optical_flow_window =
      cvSize (WINDOW_SIZE_OPTICAL_FLOW, WINDOW_SIZE_OPTICAL_FLOW);

  frame_size.width = crowddetector->priv->actual_image->width;
  frame_size.height = crowddetector->priv->actual_image->height;

  eig_image = cvCreateImage (frame_size, IPL_DEPTH_8U, 1);
  frame1_1C = cvCreateImage (frame_size, IPL_DEPTH_8U, 1);
  frame2_1C = cvCreateImage (frame_size, IPL_DEPTH_8U, 1);

  cvConvertImage (crowddetector->priv->actual_image, frame1_1C, 0);
  cvConvertImage (crowddetector->priv->previous_image, frame2_1C, 0);
  temp_image = cvCreateImage (frame_size, IPL_DEPTH_32F, 1);

  cvGoodFeaturesToTrack (frame1_1C, eig_image, temp_image, frame1_features,
      &number_of_features, QUALITY_LEVEL, MIN_DISTANCE, NULL,
      BLOCK_SIZE, USE_HARRIS_DETECTOR, HARRIS_DETECTOR_K);

  CvTermCriteria optical_flow_termination_criteria =
      cvTermCriteria (CV_TERMCRIT_ITER | CV_TERMCRIT_EPS,
      MAX_ITER_OPTICAL_FLOW, EPSILON_OPTICAL_FLOW);

  pyramid1 = cvCreateImage (frame_size, IPL_DEPTH_8U, 1);
  pyramid2 = cvCreateImage (frame_size, IPL_DEPTH_8U, 1);

  cvCalcOpticalFlowPyrLK (frame2_1C, frame1_1C, pyramid1, pyramid2,
      frame1_features, frame2_features, number_of_features,
      optical_flow_window, 3, optical_flow_found_feature,
      optical_flow_feature_error, optical_flow_termination_criteria, 0);

  cvCopy (crowddetector->priv->actual_image,
      crowddetector->priv->previous_image, 0);

  kms_crowd_detector_compute_roi_direction_vector (crowddetector,
      number_of_features, optical_flow_found_feature, frame1_features,
      frame2_features, CV_RGB (255, 0, 0), binary_actual_motion, container,
      curve);

  cvReleaseImage (&eig_image);
  cvReleaseImage (&temp_image);
  cvReleaseImage (&frame1_1C);
  cvReleaseImage (&frame2_1C);
  cvReleaseImage (&pyramid1);
  cvReleaseImage (&pyramid2);
}

static void
kms_crowd_detector_release_data (KmsCrowdDetector * crowddetector)
{
  int it;

  if (crowddetector->priv->curves != NULL) {
    for (it = 0; it < crowddetector->priv->num_rois; it++) {
      g_free (crowddetector->priv->curves[it]);
    }
    g_free (crowddetector->priv->curves);
    crowddetector->priv->curves = NULL;

    for (it = 0; it < crowddetector->priv->num_rois; it++) {
      g_free (crowddetector->priv->curves_original[it]);
    }
    g_free (crowddetector->priv->curves_original);
    crowddetector->priv->curves_original = NULL;

    for (it = 0; it < crowddetector->priv->num_rois; it++) {
      g_free (crowddetector->priv->curves_percentages[it]);
    }
    g_free (crowddetector->priv->curves_percentages);
    crowddetector->priv->curves_percentages = NULL;
  }

  if (crowddetector->priv->rois != NULL) {
    gst_structure_free (crowddetector->priv->rois);
    crowddetector->priv->rois = NULL;
  }

  if (crowddetector->priv->n_points != NULL) {
    g_free (crowddetector->priv->n_points);
    crowddetector->priv->n_points = NULL;
  }

  if (crowddetector->priv->rois_data != NULL) {
    for (it = 0; it < crowddetector->priv->num_rois; it++) {
      g_free (crowddetector->priv->rois_data[it].name);
    }
    g_free (crowddetector->priv->rois_data);
    crowddetector->priv->rois_data = NULL;
  }
}

static void
kms_crowd_detector_count_num_pixels_rois (KmsCrowdDetector * self)
{
  int curve;

  IplImage *src = cvCreateImage (cvSize (self->priv->actual_image->width,
          self->priv->actual_image->height), IPL_DEPTH_8U, 1);

  cvZero (src);

  for (curve = 0; curve < self->priv->num_rois; curve++) {
    cvFillConvexPoly (src, self->priv->curves[curve],
        self->priv->n_points[curve], cvScalar (255, 255, 255, 0), 8, 0);
    self->priv->rois_data[curve].n_pixels_roi = cvCountNonZero (src);
    self->priv->rois_data[curve].actual_occupation_level = 0;
    self->priv->rois_data[curve].potential_occupation_level = 0;
    self->priv->rois_data[curve].num_frames_potential_occupancy_level = 0;
    cvSetZero ((src));
  }
  cvReleaseImage (&src);
}

static void
kms_crowd_detector_extract_rois (KmsCrowdDetector * self)
{
  int it = 0, it2;

  self->priv->num_rois = gst_structure_n_fields (self->priv->rois);
  if (self->priv->num_rois != 0) {
    self->priv->curves = g_malloc0 (sizeof (CvPoint *) * self->priv->num_rois);
    self->priv->curves_original =
        g_malloc0 (sizeof (CvPoint *) * self->priv->num_rois);
    self->priv->curves_percentages =
        g_malloc0 (sizeof (CvPoint2D32f *) * self->priv->num_rois);
    self->priv->n_points = g_malloc (sizeof (int) * self->priv->num_rois);
    self->priv->rois_data = g_malloc0 (sizeof (RoiData) * self->priv->num_rois);
  }

  while (it < self->priv->num_rois) {
    int len;

    GstStructure *roi;
    gboolean ret2;
    const gchar *nameRoi = gst_structure_nth_field_name (self->priv->rois, it);

    ret2 = gst_structure_get (self->priv->rois, nameRoi,
        GST_TYPE_STRUCTURE, &roi, NULL);
    if (!ret2) {
      continue;
    }
    len = gst_structure_n_fields (roi) - 1;
    self->priv->n_points[it] = len;
    if (len == 0) {
      self->priv->num_rois--;
      continue;
    } else {
      self->priv->curves[it] = g_malloc (sizeof (CvPoint) * len);
      self->priv->curves_original[it] = g_malloc (sizeof (CvPoint) * len);
      self->priv->curves_percentages[it] =
          g_malloc (sizeof (CvPoint2D32f) * len);
    }

    for (it2 = 0; it2 < len; it2++) {
      const gchar *name = gst_structure_nth_field_name (roi, it2);
      GstStructure *point;
      gboolean ret;

      ret = gst_structure_get (roi, name, GST_TYPE_STRUCTURE, &point, NULL);

      if (ret) {
        gfloat percentageX;
        gfloat percentageY;

        gst_structure_get (point, "x", G_TYPE_FLOAT, &percentageX, NULL);
        gst_structure_get (point, "y", G_TYPE_FLOAT, &percentageY, NULL);

        self->priv->curves[it][it2].x = percentageX * self->priv->image_width;
        self->priv->curves[it][it2].y = percentageY * self->priv->image_height;

        self->priv->curves_original[it][it2].x =
            percentageX * self->priv->original_image_width;
        self->priv->curves_original[it][it2].y =
            percentageY * self->priv->original_image_height;

        self->priv->curves_percentages[it][it2].x = percentageX;
        self->priv->curves_percentages[it][it2].y = percentageY;
      }
      gst_structure_free (point);
    }

    {
      const gchar *name = gst_structure_nth_field_name (roi, it2);
      GstStructure *point;
      gboolean ret;

      ret = gst_structure_get (roi, name, GST_TYPE_STRUCTURE, &point, NULL);

      if (ret) {
        self->priv->rois_data[it].name = NULL;

        gst_structure_get (point, "id", G_TYPE_STRING,
            &self->priv->rois_data[it].name, NULL);
        gst_structure_get (point, "occupancy_level_min", G_TYPE_INT,
            &self->priv->rois_data[it].occupancy_level_min, NULL);
        gst_structure_get (point, "occupancy_level_med", G_TYPE_INT,
            &self->priv->rois_data[it].occupancy_level_med, NULL);
        gst_structure_get (point, "occupancy_level_max", G_TYPE_INT,
            &self->priv->rois_data[it].occupancy_level_max, NULL);
        gst_structure_get (point, "occupancy_num_frames_to_event", G_TYPE_INT,
            &self->priv->rois_data[it].occupancy_num_frames_to_event, NULL);
        gst_structure_get (point, "fluidity_level_min", G_TYPE_INT,
            &self->priv->rois_data[it].fluidity_level_min, NULL);
        gst_structure_get (point, "fluidity_level_med", G_TYPE_INT,
            &self->priv->rois_data[it].fluidity_level_med, NULL);
        gst_structure_get (point, "fluidity_level_max", G_TYPE_INT,
            &self->priv->rois_data[it].fluidity_level_max, NULL);
        gst_structure_get (point, "fluidity_num_frames_to_event", G_TYPE_INT,
            &self->priv->rois_data[it].fluidity_num_frames_to_event, NULL);
        gst_structure_get (point, "send_optical_flow_event", G_TYPE_BOOLEAN,
            &self->priv->rois_data[it].send_optical_flow_event, NULL);
        gst_structure_get (point, "optical_flow_num_frames_to_event",
            G_TYPE_INT,
            &self->priv->rois_data[it].optical_flow_num_frames_to_event, NULL);
        gst_structure_get (point, "optical_flow_num_frames_to_reset",
            G_TYPE_INT,
            &self->priv->rois_data[it].optical_flow_num_frames_to_reset, NULL);
        gst_structure_get (point, "optical_flow_angle_offset", G_TYPE_INT,
            &self->priv->rois_data[it].optical_flow_angle_offset, NULL);
        GST_DEBUG
            ("rois info loaded: %s %d %d %d %d %d %d %d %d %d %d %d %d",
            self->priv->rois_data[it].name,
            self->priv->rois_data[it].occupancy_level_min,
            self->priv->rois_data[it].occupancy_level_med,
            self->priv->rois_data[it].occupancy_level_max,
            self->priv->rois_data[it].occupancy_num_frames_to_event,
            self->priv->rois_data[it].fluidity_level_min,
            self->priv->rois_data[it].fluidity_level_med,
            self->priv->rois_data[it].fluidity_level_max,
            self->priv->rois_data[it].fluidity_num_frames_to_event,
            self->priv->rois_data[it].send_optical_flow_event,
            self->priv->rois_data[it].optical_flow_num_frames_to_event,
            self->priv->rois_data[it].optical_flow_num_frames_to_reset,
            self->priv->rois_data[it].optical_flow_angle_offset);
      }

      gst_structure_free (point);
    }

    gst_structure_free (roi);
    it++;
  }
  self->priv->pixels_rois_counted = TRUE;
}

static void
kms_crowd_detector_set_property (GObject * object, guint property_id,
    const GValue * value, GParamSpec * pspec)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (object);

  GST_DEBUG_OBJECT (crowddetector, "set_property");

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      crowddetector->priv->show_debug_info = g_value_get_boolean (value);
      break;
    case PROP_ROIS:
      kms_crowd_detector_release_data (crowddetector);
      crowddetector->priv->rois = g_value_dup_boxed (value);
      break;
    case PROP_PROCESSING_WIDTH:
      KMS_CROWD_DETECTOR_LOCK (crowddetector);
      crowddetector->priv->processing_width = g_value_get_int (value);
      GST_DEBUG_OBJECT (crowddetector, "New proccesing width = %d",
          crowddetector->priv->processing_width);
      KMS_CROWD_DETECTOR_UNLOCK (crowddetector);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_crowd_detector_get_property (GObject * object, guint property_id,
    GValue * value, GParamSpec * pspec)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (object);

  GST_DEBUG_OBJECT (crowddetector, "get_property");

  switch (property_id) {
    case PROP_SHOW_DEBUG_INFO:
      g_value_set_boolean (value, crowddetector->priv->show_debug_info);
      break;
    case PROP_ROIS:
      if (crowddetector->priv->rois == NULL) {
        crowddetector->priv->rois = gst_structure_new_empty ("rois");
      }
      g_value_set_boxed (value, crowddetector->priv->rois);
      break;
    case PROP_PROCESSING_WIDTH:
      KMS_CROWD_DETECTOR_LOCK (crowddetector);
      g_value_set_int (value, crowddetector->priv->processing_width);
      KMS_CROWD_DETECTOR_UNLOCK (crowddetector);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, property_id, pspec);
      break;
  }
}

static void
kms_crowd_detector_dispose (GObject * object)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (object);

  GST_DEBUG_OBJECT (crowddetector, "dispose");

  /* clean up as possible.  may be called multiple times */

  G_OBJECT_CLASS (kms_crowd_detector_parent_class)->dispose (object);
}

static void
kms_crowd_detector_release_images (KmsCrowdDetector * crowddetector)
{
  cvReleaseImage (&crowddetector->priv->original_image);
  cvReleaseImage (&crowddetector->priv->actual_image);
  cvReleaseImage (&crowddetector->priv->previous_lbp);
  cvReleaseImage (&crowddetector->priv->frame_previous_gray);
  cvReleaseImage (&crowddetector->priv->background);
  cvReleaseImage (&crowddetector->priv->acumulated_edges);
  cvReleaseImage (&crowddetector->priv->acumulated_lbp);
  cvReleaseImage (&crowddetector->priv->previous_image);
}

static void
kms_crowd_detector_finalize (GObject * object)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (object);

  GST_DEBUG_OBJECT (crowddetector, "finalize");

  g_rec_mutex_clear (&crowddetector->priv->mutex);
  kms_crowd_detector_release_images (crowddetector);
  kms_crowd_detector_release_data (crowddetector);

  G_OBJECT_CLASS (kms_crowd_detector_parent_class)->finalize (object);
}

static gboolean
kms_crowd_detector_start (GstBaseTransform * trans)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (trans);

  GST_DEBUG_OBJECT (crowddetector, "start");

  return TRUE;
}

static gboolean
kms_crowd_detector_stop (GstBaseTransform * trans)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (trans);

  GST_DEBUG_OBJECT (crowddetector, "stop");

  return TRUE;
}

static gboolean
kms_crowd_detector_set_info (GstVideoFilter * filter, GstCaps * incaps,
    GstVideoInfo * in_info, GstCaps * outcaps, GstVideoInfo * out_info)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (filter);

  GST_DEBUG_OBJECT (crowddetector, "set_info");

  return TRUE;
}

static void
kms_crowd_detector_create_images (KmsCrowdDetector * crowddetector,
    GstVideoFrame * frame, int target_width)
{
  crowddetector->priv->resize_factor = frame->info.width / target_width;

  crowddetector->priv->image_width = target_width;
  crowddetector->priv->image_height =
      frame->info.height / crowddetector->priv->resize_factor;

  crowddetector->priv->original_image_width = frame->info.width;
  crowddetector->priv->original_image_height = frame->info.height;

  crowddetector->priv->original_image =
      cvCreateImage (cvSize (frame->info.width, frame->info.height),
      IPL_DEPTH_8U, 3);

  crowddetector->priv->actual_image =
      cvCreateImage (cvSize (crowddetector->priv->image_width,
          crowddetector->priv->image_height), IPL_DEPTH_8U, 3);
  cvSet (crowddetector->priv->actual_image, CV_RGB (0, 0, 0), 0);

  crowddetector->priv->previous_lbp =
      cvCreateImage (cvSize (crowddetector->priv->image_width,
          crowddetector->priv->image_height), IPL_DEPTH_8U, 1);
  cvZero (crowddetector->priv->previous_lbp);

  crowddetector->priv->frame_previous_gray =
      cvCreateImage (cvSize (crowddetector->priv->image_width,
          crowddetector->priv->image_height), IPL_DEPTH_8U, 1);
  cvZero (crowddetector->priv->frame_previous_gray);

  crowddetector->priv->background =
      cvCreateImage (cvSize (crowddetector->priv->image_width,
          crowddetector->priv->image_height), IPL_DEPTH_8U, 1);
  cvZero (crowddetector->priv->background);

  crowddetector->priv->acumulated_edges =
      cvCreateImage (cvSize (crowddetector->priv->image_width,
          crowddetector->priv->image_height), IPL_DEPTH_8U, 1);
  cvZero (crowddetector->priv->acumulated_edges);

  crowddetector->priv->acumulated_lbp =
      cvCreateImage (cvSize (crowddetector->priv->image_width,
          crowddetector->priv->image_height), IPL_DEPTH_8U, 1);
  cvZero (crowddetector->priv->acumulated_lbp);

  crowddetector->priv->previous_image =
      cvCreateImage (cvSize (crowddetector->priv->image_width,
          crowddetector->priv->image_height), IPL_DEPTH_8U, 3);
  cvSet (crowddetector->priv->previous_image, CV_RGB (0, 0, 0), 0);
}

static void
kms_crowd_detector_update_rois_size (KmsCrowdDetector * crowddetector)
{
  int it = 0, it2;

  if (crowddetector->priv->curves != NULL) {
    for (it = 0; it < crowddetector->priv->num_rois; it++) {
      for (it2 = 0; it2 < crowddetector->priv->n_points[it]; it2++) {
        crowddetector->priv->curves[it][it2].x =
            crowddetector->priv->curves_percentages[it][it2].x
            * crowddetector->priv->image_width;
        crowddetector->priv->curves[it][it2].y =
            crowddetector->priv->curves_percentages[it][it2].y
            * crowddetector->priv->image_height;
      }
    }
  }
}

static void
kms_crowd_detector_initialize_images (KmsCrowdDetector * crowddetector,
    GstVideoFrame * frame)
{
  KMS_CROWD_DETECTOR_LOCK (crowddetector);
  int target_width =
      frame->info.width <= crowddetector->priv->processing_width ?
      frame->info.width : crowddetector->priv->processing_width;
  KMS_CROWD_DETECTOR_UNLOCK (crowddetector);

  if (crowddetector->priv->actual_image == NULL) {
    kms_crowd_detector_create_images (crowddetector, frame, target_width);
  } else if ((crowddetector->priv->original_image->width != frame->info.width)
      || (crowddetector->priv->original_image->height != frame->info.height)
      || (crowddetector->priv->image_width != target_width)) {
    GST_DEBUG_OBJECT (crowddetector, "Changing processing image size");
    kms_crowd_detector_release_images (crowddetector);
    kms_crowd_detector_create_images (crowddetector, frame, target_width);
    kms_crowd_detector_update_rois_size (crowddetector);
  }
}

static void
kms_crowd_detector_compute_temporal_lbp (IplImage * frame_gray,
    IplImage * frame_result, IplImage * previous_frame_gray, gboolean temporal)
{
  int w, h;
  uint8_t *image_pointer;
  uint8_t *image_pointer_aux;
  int refValue = 0;

  if (temporal == TRUE)
    image_pointer = (uint8_t *) previous_frame_gray->imageData;
  else
    image_pointer = (uint8_t *) frame_gray->imageData;

  for (h = 1; h < frame_gray->height - 1; h++) {
    image_pointer_aux = image_pointer;
    for (w = 1; w < frame_gray->width - 1; w++) {
      refValue = *image_pointer_aux;
      image_pointer_aux++;
      unsigned int code = 0;

      code |= (*(uchar *) (frame_gray->imageData + (h - 1) *
              frame_gray->widthStep + (w - 1) * 1) > refValue) << 7;
      code |= (*(uchar *) (frame_gray->imageData + (h) *
              frame_gray->widthStep + (w - 1) * 1) > refValue) << 6;
      code |= (*(uchar *) (frame_gray->imageData + (h + 1) *
              frame_gray->widthStep + (w - 1) * 1) > refValue) << 5;
      code |= (*(uchar *) (frame_gray->imageData + (h) *
              frame_gray->widthStep + (w) * 1) > refValue) << 4;
      code |= (*(uchar *) (frame_gray->imageData + (h + 1) *
              frame_gray->widthStep + (w + 1) * 1) > refValue) << 3;
      code |= (*(uchar *) (frame_gray->imageData + (h) *
              frame_gray->widthStep + (w + 1) * 1) > refValue) << 2;
      code |= (*(uchar *) (frame_gray->imageData + (h - 1) *
              frame_gray->widthStep + (w + 1) * 1) > refValue) << 1;
      code |= (*(uchar *) (frame_gray->imageData + (h) *
              frame_gray->widthStep + (w + 1) * 1) > refValue) << 0;

      *(uchar *) (frame_result->imageData + h *
          frame_result->widthStep + w * 1) = code;
    }
    image_pointer += previous_frame_gray->widthStep;
  }
}

static void
kms_crowd_detector_mask_image (IplImage * src, IplImage * mask,
    int threshold_value)
{
  int w, h;
  uint8_t *mask_pointer_aux;
  uint8_t *mask_pointer = (uint8_t *) mask->imageData;
  uint8_t *src_pointer_aux;
  uint8_t *src_pointer = (uint8_t *) src->imageData;

  for (h = 0; h < mask->height; h++) {
    mask_pointer_aux = mask_pointer;
    src_pointer_aux = src_pointer;
    for (w = 0; w < mask->width; w++) {
      if (*mask_pointer_aux == threshold_value)
        *src_pointer_aux = 0;
      mask_pointer_aux++;
      src_pointer_aux++;
    }
    mask_pointer += mask->widthStep;
    src_pointer += src->widthStep;
  }
}

static void
kms_crowd_detector_substract_background (IplImage * frame,
    IplImage * background, IplImage * actual_image)
{
  int w, h;
  uint8_t *frame_pointer_aux;
  uint8_t *frame_pointer = (uint8_t *) frame->imageData;
  uint8_t *background_pointer_aux;
  uint8_t *background_pointer = (uint8_t *) background->imageData;
  uint8_t *actual_image_pointer_aux;
  uint8_t *actual_image_pointer = (uint8_t *) actual_image->imageData;

  for (h = 0; h < frame->height; h++) {
    frame_pointer_aux = frame_pointer;
    background_pointer_aux = background_pointer;
    actual_image_pointer_aux = actual_image_pointer;
    for (w = 0; w < frame->width; w++) {
      if (abs (*frame_pointer_aux - *background_pointer_aux) <
          GRAY_THRESHOLD_VALUE)
        *actual_image_pointer_aux = 255;
      else
        *actual_image_pointer_aux = *frame_pointer_aux;
      frame_pointer_aux++;
      background_pointer_aux++;
      actual_image_pointer_aux++;
    }
    frame_pointer += frame->widthStep;
    background_pointer += background->widthStep;
    actual_image_pointer += actual_image->widthStep;
  }
}

static void
kms_crowd_detector_process_edges_image (KmsCrowdDetector * crowddetector,
    IplImage * speed_map, int window_margin)
{
  int w, h, w2, h2;
  uint8_t *speed_map_pointer_aux;
  uint8_t *speed_map_pointer = (uint8_t *) speed_map->imageData;

  speed_map_pointer =
      speed_map_pointer + (speed_map->widthStep * (window_margin));

  for (h2 = window_margin;
      h2 < crowddetector->priv->acumulated_edges->height - window_margin - 1;
      h2++) {
    speed_map_pointer_aux = speed_map_pointer + window_margin;
    for (w2 = window_margin;
        w2 < crowddetector->priv->acumulated_edges->width - window_margin - 1;
        w2++) {
      int pixel_counter = 0;

      for (h = -window_margin; h < window_margin + 1; h++) {
        for (w = -window_margin; w < window_margin + 1; w++) {
          if (h != 0 || w != 0) {
            if (*(uchar *) (crowddetector->priv->acumulated_edges->imageData +
                    (h2 +
                        h) * crowddetector->priv->acumulated_edges->widthStep +
                    (w2 + w)) > EDGE_THRESHOLD)
              pixel_counter++;
          }
        }
      }
      if (pixel_counter > pow (window_margin, 2)) {
        *speed_map_pointer_aux = 255;
      } else
        *speed_map_pointer_aux = 0;
      speed_map_pointer_aux++;
    }
    speed_map_pointer += speed_map->widthStep;
  }
}

static void
kms_crowd_detector_roi_occup_analysis (KmsCrowdDetector * crowddetector,
    double occupation_percentage, int occupancy_num_frames_to_event,
    int occupancy_level_min, int occupancy_level_med, int occupancy_level_max,
    int curve)
{
  if (occupation_percentage > occupancy_level_max) {
    if (crowddetector->priv->rois_data[curve].potential_occupation_level != 3) {
      crowddetector->priv->rois_data[curve].potential_occupation_level = 3;
      crowddetector->priv->
          rois_data[curve].num_frames_potential_occupancy_level = 1;
    } else {
      crowddetector->priv->
          rois_data[curve].num_frames_potential_occupancy_level++;
    }
  } else if (occupation_percentage > occupancy_level_med) {
    if (crowddetector->priv->rois_data[curve].potential_occupation_level != 2) {
      crowddetector->priv->rois_data[curve].potential_occupation_level = 2;
      crowddetector->priv->
          rois_data[curve].num_frames_potential_occupancy_level = 1;
    } else {
      crowddetector->priv->
          rois_data[curve].num_frames_potential_occupancy_level++;
    }
  } else if (occupation_percentage > occupancy_level_min) {
    if (crowddetector->priv->rois_data[curve].potential_occupation_level != 1) {
      crowddetector->priv->rois_data[curve].potential_occupation_level = 1;
      crowddetector->priv->
          rois_data[curve].num_frames_potential_occupancy_level = 1;
    } else {
      crowddetector->priv->
          rois_data[curve].num_frames_potential_occupancy_level++;
    }
  } else {
    if (crowddetector->priv->rois_data[curve].potential_occupation_level != 0) {
      crowddetector->priv->rois_data[curve].potential_occupation_level = 0;
      crowddetector->priv->
          rois_data[curve].num_frames_potential_occupancy_level = 1;
    } else {
      crowddetector->priv->
          rois_data[curve].num_frames_potential_occupancy_level++;
    }
  }

  if (crowddetector->priv->
      rois_data[curve].num_frames_potential_occupancy_level >
      occupancy_num_frames_to_event) {
    crowddetector->priv->rois_data[curve].num_frames_potential_occupancy_level =
        occupancy_num_frames_to_event;
  }

  if (crowddetector->priv->
      rois_data[curve].num_frames_potential_occupancy_level ==
      occupancy_num_frames_to_event
      && crowddetector->priv->rois_data[curve].actual_occupation_level !=
      crowddetector->priv->rois_data[curve].potential_occupation_level) {
    crowddetector->priv->rois_data[curve].actual_occupation_level =
        crowddetector->priv->rois_data[curve].potential_occupation_level;
    GST_DEBUG ("%s: occupancy_percentage:%f occupancy_level:%d",
        crowddetector->priv->rois_data[curve].name, occupation_percentage,
        crowddetector->priv->rois_data[curve].actual_occupation_level);
    kms_crowd_detector_send_message_occupacy (crowddetector,
        occupation_percentage, curve);
  }
}

static void
kms_crowd_detector_roi_fluidity_analysis (KmsCrowdDetector * crowddetector,
    int high_speed_points, int low_speed_points, int fluid_num_frames_to_event,
    int fluidity_level_min, int fluidity_level_med, int fluidity_level_max,
    int curve)
{
  double fluidity_percentage = 0.0;

  if (high_speed_points + low_speed_points > 0) {
    fluidity_percentage =
        low_speed_points * 100 / (high_speed_points + low_speed_points);
  } else {
    fluidity_percentage = 0.0;
  }
  if (fluidity_percentage >= fluidity_level_max) {
    if (crowddetector->priv->rois_data[curve].potential_fluidity_level != 3) {
      crowddetector->priv->rois_data[curve].potential_fluidity_level = 3;
      crowddetector->priv->rois_data[curve].
          num_frames_potential_fluidity_level = 1;
    } else {
      crowddetector->priv->
          rois_data[curve].num_frames_potential_fluidity_level++;
    }
  } else if (fluidity_percentage > fluidity_level_med) {
    if (crowddetector->priv->rois_data[curve].potential_fluidity_level != 2) {
      crowddetector->priv->rois_data[curve].potential_fluidity_level = 2;
      crowddetector->priv->rois_data[curve].
          num_frames_potential_fluidity_level = 1;
    } else {
      crowddetector->priv->
          rois_data[curve].num_frames_potential_fluidity_level++;
    }
  } else if (fluidity_percentage > fluidity_level_min) {
    if (crowddetector->priv->rois_data[curve].potential_fluidity_level != 1) {
      crowddetector->priv->rois_data[curve].potential_fluidity_level = 1;
      crowddetector->priv->rois_data[curve].
          num_frames_potential_fluidity_level = 1;
    } else {
      crowddetector->priv->
          rois_data[curve].num_frames_potential_fluidity_level++;
    }
  } else {
    if (crowddetector->priv->rois_data[curve].potential_fluidity_level != 0) {
      crowddetector->priv->rois_data[curve].potential_fluidity_level = 0;
      crowddetector->priv->rois_data[curve].
          num_frames_potential_fluidity_level = 1;
    } else {
      crowddetector->priv->
          rois_data[curve].num_frames_potential_fluidity_level++;
    }
  }

  if (crowddetector->priv->rois_data[curve].
      num_frames_potential_fluidity_level > fluid_num_frames_to_event) {
    crowddetector->priv->rois_data[curve].num_frames_potential_fluidity_level =
        fluid_num_frames_to_event;
  }

  if (crowddetector->priv->
      rois_data[curve].num_frames_potential_fluidity_level ==
      fluid_num_frames_to_event
      && crowddetector->priv->rois_data[curve].actual_fluidity_level !=
      crowddetector->priv->rois_data[curve].potential_fluidity_level) {
    crowddetector->priv->rois_data[curve].actual_fluidity_level =
        crowddetector->priv->rois_data[curve].potential_fluidity_level;
    GST_DEBUG ("%s: FLUIDITY_percentage:%f fluidity_level:%d",
        crowddetector->priv->rois_data[curve].name, fluidity_percentage,
        crowddetector->priv->rois_data[curve].actual_fluidity_level);
    kms_crowd_detector_send_message_fluidity (crowddetector,
        fluidity_percentage, curve);
  }
}

static CvRect
kms_crowd_detector_get_square_roi_contaniner (KmsCrowdDetector * crowddetector,
    int curve)
{
  int w1, w2, h1, h2;
  CvRect container;
  int point;

  w1 = crowddetector->priv->actual_image->width;
  h1 = crowddetector->priv->actual_image->height;
  w2 = 0;
  h2 = 0;

  for (point = 0; point < crowddetector->priv->n_points[curve]; point++) {
    if (crowddetector->priv->curves[curve][point].x < w1)
      w1 = crowddetector->priv->curves[curve][point].x;
    if (crowddetector->priv->curves[curve][point].x > w2)
      w2 = crowddetector->priv->curves[curve][point].x;
    if (crowddetector->priv->curves[curve][point].y < h1)
      h1 = crowddetector->priv->curves[curve][point].y;
    if (crowddetector->priv->curves[curve][point].y > h2)
      h2 = crowddetector->priv->curves[curve][point].y;
  }

  container.x = w1;
  container.y = h1;
  container.width = abs (w2 - w1);
  container.height = abs (h2 - h1);

  return container;
}

static void
kms_crowd_detector_roi_analysis (KmsCrowdDetector * crowddetector,
    IplImage * low_speed_map, IplImage * high_speed_map)
{
  int curve;

  for (curve = 0; curve < crowddetector->priv->num_rois; curve++) {

    int high_speed_points = 0;
    int low_speed_points = 0;
    int total_pixels_occupied = 0;
    double occupation_percentage = 0.0;
    CvRect container =
        kms_crowd_detector_get_square_roi_contaniner (crowddetector, curve);

    cvRectangle (low_speed_map, cvPoint (container.x, container.y),
        cvPoint (container.x + container.width, container.y + container.height),
        cvScalar (0, 255, 0, 0), 1, 8, 0);
    cvSetImageROI (low_speed_map, container);
    cvSetImageROI (high_speed_map, container);
    low_speed_points = cvCountNonZero (low_speed_map);
    high_speed_points = cvCountNonZero (high_speed_map);
    cvResetImageROI (low_speed_map);
    cvResetImageROI (high_speed_map);
    total_pixels_occupied = high_speed_points + low_speed_points;
    if (crowddetector->priv->rois_data[curve].n_pixels_roi > 0) {
      occupation_percentage = ((double) total_pixels_occupied * 100 /
          crowddetector->priv->rois_data[curve].n_pixels_roi);
    } else {
      occupation_percentage = 0.0;
    }
    kms_crowd_detector_roi_occup_analysis (crowddetector,
        occupation_percentage,
        crowddetector->priv->rois_data[curve].occupancy_num_frames_to_event,
        crowddetector->priv->rois_data[curve].occupancy_level_min,
        crowddetector->priv->rois_data[curve].occupancy_level_med,
        crowddetector->priv->rois_data[curve].occupancy_level_max, curve);
    kms_crowd_detector_roi_fluidity_analysis (crowddetector,
        high_speed_points, low_speed_points,
        crowddetector->priv->rois_data[curve].fluidity_num_frames_to_event,
        crowddetector->priv->rois_data[curve].fluidity_level_min,
        crowddetector->priv->rois_data[curve].fluidity_level_med,
        crowddetector->priv->rois_data[curve].fluidity_level_max, curve);
  }

}

static GstFlowReturn
kms_crowd_detector_transform_frame_ip (GstVideoFilter * filter,
    GstVideoFrame * frame)
{
  KmsCrowdDetector *crowddetector = KMS_CROWD_DETECTOR (filter);
  GstMapInfo info;

  kms_crowd_detector_initialize_images (crowddetector, frame);

  if ((crowddetector->priv->num_rois == 0)
      && (crowddetector->priv->rois != NULL)) {
    kms_crowd_detector_extract_rois (crowddetector);
  }
  if (crowddetector->priv->pixels_rois_counted == TRUE &&
      crowddetector->priv->actual_image != NULL) {
    kms_crowd_detector_count_num_pixels_rois (crowddetector);
    crowddetector->priv->pixels_rois_counted = FALSE;
  }
  gst_buffer_map (frame->buffer, &info, GST_MAP_READ);
  crowddetector->priv->original_image->imageData = (char *) info.data;

  cvResize (crowddetector->priv->original_image,
      crowddetector->priv->actual_image, CV_INTER_LINEAR);

  IplImage *frame_actual_gray =
      cvCreateImage (cvSize (crowddetector->priv->actual_image->width,
          crowddetector->priv->actual_image->height),
      IPL_DEPTH_8U, 1);

  cvZero (frame_actual_gray);

  IplImage *actual_lbp =
      cvCreateImage (cvSize (crowddetector->priv->actual_image->width,
          crowddetector->priv->actual_image->height),
      IPL_DEPTH_8U, 1);

  cvZero (actual_lbp);

  IplImage *lbp_temporal_result =
      cvCreateImage (cvSize (crowddetector->priv->actual_image->width,
          crowddetector->priv->actual_image->height),
      IPL_DEPTH_8U, 1);

  cvZero (lbp_temporal_result);

  IplImage *add_lbps_result =
      cvCreateImage (cvSize (crowddetector->priv->actual_image->width,
          crowddetector->priv->actual_image->height),
      IPL_DEPTH_8U, 1);

  cvZero (add_lbps_result);

  IplImage *lbps_alpha_result_rgb =
      cvCreateImage (cvSize (crowddetector->priv->actual_image->width,
          crowddetector->priv->actual_image->height),
      IPL_DEPTH_8U, 3);

  cvSet (lbps_alpha_result_rgb, CV_RGB (0, 0, 0), 0);

  IplImage *actual_image_masked =
      cvCreateImage (cvSize (crowddetector->priv->actual_image->width,
          crowddetector->priv->actual_image->height), IPL_DEPTH_8U, 1);

  cvZero (actual_image_masked);

  IplImage *substract_background_to_actual =
      cvCreateImage (cvSize (crowddetector->priv->actual_image->width,
          crowddetector->priv->actual_image->height),
      IPL_DEPTH_8U, 1);

  cvZero (substract_background_to_actual);

  IplImage *low_speed_map =
      cvCreateImage (cvSize (crowddetector->priv->actual_image->width,
          crowddetector->priv->actual_image->height),
      IPL_DEPTH_8U, 1);

  cvZero (low_speed_map);

  IplImage *high_speed_map =
      cvCreateImage (cvSize (crowddetector->priv->actual_image->width,
          crowddetector->priv->actual_image->height),
      IPL_DEPTH_8U, 1);

  cvZero (high_speed_map);

  IplImage *actual_motion =
      cvCreateImage (cvSize (crowddetector->priv->actual_image->width,
          crowddetector->priv->actual_image->height),
      IPL_DEPTH_8U, 3);

  cvSet (actual_motion, CV_RGB (0, 0, 0), 0);

  IplImage *binary_actual_motion =
      cvCreateImage (cvSize (crowddetector->priv->actual_image->width,
          crowddetector->priv->actual_image->height),
      IPL_DEPTH_8U, 1);

  cvZero (binary_actual_motion);

  uint8_t *low_speed_pointer;
  uint8_t *low_speed_pointer_aux;
  uint8_t *high_speed_pointer;
  uint8_t *high_speed_pointer_aux;
  uint8_t *actual_motion_pointer;
  uint8_t *actual_motion_pointer_aux;
  uint8_t *binary_actual_motion_pointer;
  uint8_t *binary_actual_motion_pointer_aux;

  int w, h;

  if (crowddetector->priv->num_rois != 0) {
    cvFillPoly (actual_image_masked, crowddetector->priv->curves,
        crowddetector->priv->n_points, crowddetector->priv->num_rois,
        cvScalar (255, 255, 255, 0), CV_AA, 0);
  }
  cvCvtColor (crowddetector->priv->actual_image, frame_actual_gray,
      CV_BGR2GRAY);
  kms_crowd_detector_mask_image (frame_actual_gray, actual_image_masked, 0);

  if (crowddetector->priv->background == NULL) {
    cvCopy (frame_actual_gray, crowddetector->priv->background, 0);
  } else {
    cvAddWeighted (crowddetector->priv->background, BACKGROUND_ADD_RATIO,
        frame_actual_gray, 1 - BACKGROUND_ADD_RATIO, 0,
        crowddetector->priv->background);
  }

  kms_crowd_detector_compute_temporal_lbp (frame_actual_gray, actual_lbp,
      actual_lbp, FALSE);
  kms_crowd_detector_compute_temporal_lbp (frame_actual_gray,
      lbp_temporal_result, crowddetector->priv->frame_previous_gray, TRUE);
  cvAddWeighted (crowddetector->priv->previous_lbp, LBPS_ADD_RATIO, actual_lbp,
      (1 - LBPS_ADD_RATIO), 0, add_lbps_result);
  cvSub (crowddetector->priv->previous_lbp, actual_lbp, add_lbps_result, 0);
  cvThreshold (add_lbps_result, add_lbps_result, 70.0, 255.0, CV_THRESH_OTSU);
  cvNot (add_lbps_result, add_lbps_result);
  cvErode (add_lbps_result, add_lbps_result, 0, 4);
  cvDilate (add_lbps_result, add_lbps_result, 0, 11);
  cvErode (add_lbps_result, add_lbps_result, 0, 3);
  cvCvtColor (add_lbps_result, lbps_alpha_result_rgb, CV_GRAY2BGR);
  cvCopy (actual_lbp, crowddetector->priv->previous_lbp, 0);
  cvCopy (frame_actual_gray, crowddetector->priv->frame_previous_gray, 0);

  if (crowddetector->priv->acumulated_lbp == NULL) {
    cvCopy (add_lbps_result, crowddetector->priv->acumulated_lbp, 0);
  } else {
    cvAddWeighted (crowddetector->priv->acumulated_lbp, TEMPORAL_LBPS_ADD_RATIO,
        add_lbps_result, 1 - TEMPORAL_LBPS_ADD_RATIO, 0,
        crowddetector->priv->acumulated_lbp);
  }

  cvThreshold (crowddetector->priv->acumulated_lbp, high_speed_map,
      150.0, 255.0, CV_THRESH_BINARY);
  cvSmooth (high_speed_map, high_speed_map, CV_MEDIAN, 3, 0, 0.0, 0.0);
  kms_crowd_detector_substract_background (frame_actual_gray,
      crowddetector->priv->background, substract_background_to_actual);
  cvThreshold (substract_background_to_actual, substract_background_to_actual,
      70.0, 255.0, CV_THRESH_OTSU);

  cvCanny (substract_background_to_actual,
      substract_background_to_actual, 70.0, 150.0, 3);

  if (crowddetector->priv->acumulated_edges == NULL) {
    cvCopy (substract_background_to_actual,
        crowddetector->priv->acumulated_edges, 0);
  } else {
    cvAddWeighted (crowddetector->priv->acumulated_edges, EDGES_ADD_RATIO,
        substract_background_to_actual, 1 - EDGES_ADD_RATIO, 0,
        crowddetector->priv->acumulated_edges);
  }

  kms_crowd_detector_process_edges_image (crowddetector, low_speed_map, 3);
  cvErode (low_speed_map, low_speed_map, 0, 1);

  low_speed_pointer = (uint8_t *) low_speed_map->imageData;
  high_speed_pointer = (uint8_t *) high_speed_map->imageData;
  actual_motion_pointer = (uint8_t *) actual_motion->imageData;
  binary_actual_motion_pointer = (uint8_t *) binary_actual_motion->imageData;

  for (h = 0; h < low_speed_map->height; h++) {
    low_speed_pointer_aux = low_speed_pointer;
    high_speed_pointer_aux = high_speed_pointer;
    actual_motion_pointer_aux = actual_motion_pointer;
    binary_actual_motion_pointer_aux = binary_actual_motion_pointer;
    for (w = 0; w < low_speed_map->width; w++) {
      if (*high_speed_pointer_aux == 0) {
        actual_motion_pointer_aux[0] = 255;
        binary_actual_motion_pointer_aux[0] = 255;
      } else if (*low_speed_pointer_aux == 255) {
        actual_motion_pointer_aux[2] = 255;
        binary_actual_motion_pointer_aux[0] = 255;
      }
      low_speed_pointer_aux++;
      high_speed_pointer_aux++;
      actual_motion_pointer_aux = actual_motion_pointer_aux + 3;
      binary_actual_motion_pointer_aux++;
    }
    low_speed_pointer += low_speed_map->widthStep;
    high_speed_pointer += high_speed_map->widthStep;
    actual_motion_pointer += actual_motion->widthStep;
    binary_actual_motion_pointer += binary_actual_motion->widthStep;
  }

  int curve;

  for (curve = 0; curve < crowddetector->priv->num_rois; curve++) {

    if (crowddetector->priv->rois_data[curve].send_optical_flow_event == TRUE) {

      CvRect container =
          kms_crowd_detector_get_square_roi_contaniner (crowddetector, curve);

      cvSetImageROI (crowddetector->priv->actual_image, container);
      cvSetImageROI (crowddetector->priv->previous_image, container);
      cvSetImageROI (actual_motion, container);

      kms_crowd_detector_compute_optical_flow (crowddetector,
          binary_actual_motion, container, curve);

      cvResetImageROI (crowddetector->priv->actual_image);
      cvResetImageROI (crowddetector->priv->previous_image);
      cvResetImageROI (actual_motion);
    }
  }

  //drawing blur/red mask over rois regions
  {
    uint8_t *orig_row_pointer;
    uint8_t *overlay_row_pointer;
    IplImage *actual_motion_original =
        cvCreateImage (cvSize (crowddetector->priv->original_image->width,
            crowddetector->priv->original_image->height),
        IPL_DEPTH_8U, 3);

    cvResize (actual_motion, actual_motion_original, CV_INTER_LINEAR);

    orig_row_pointer =
        (uint8_t *) crowddetector->priv->original_image->imageData;
    overlay_row_pointer = (uint8_t *) actual_motion_original->imageData;

    for (h = 0; h < crowddetector->priv->original_image->height; h++) {
      uint8_t *orig_column_pointer = orig_row_pointer;
      uint8_t *overlay_column_pointer = overlay_row_pointer;

      for (w = 0; w < crowddetector->priv->original_image->width; w++) {
        int c;

        for (c = 0; c < crowddetector->priv->original_image->nChannels; c++) {
          if (overlay_column_pointer[c] != 0) {
            orig_column_pointer[c] = overlay_column_pointer[c];
          }
        }

        orig_column_pointer += crowddetector->priv->original_image->nChannels;
        overlay_column_pointer += actual_motion_original->nChannels;
      }
      orig_row_pointer += crowddetector->priv->original_image->widthStep;
      overlay_row_pointer += actual_motion_original->widthStep;
    }

    cvReleaseImage (&actual_motion_original);
  }

  if (crowddetector->priv->num_rois != 0) {
    cvPolyLine (crowddetector->priv->original_image,
        crowddetector->priv->curves_original, crowddetector->priv->n_points,
        crowddetector->priv->num_rois, 1, cvScalar (255, 255, 255, 0), 1, 8, 0);
  }

  cvNot (high_speed_map, high_speed_map);
  kms_crowd_detector_roi_analysis (crowddetector, low_speed_map,
      high_speed_map);

  cvReleaseImage (&frame_actual_gray);
  cvReleaseImage (&actual_lbp);
  cvReleaseImage (&lbp_temporal_result);
  cvReleaseImage (&add_lbps_result);
  cvReleaseImage (&lbps_alpha_result_rgb);
  cvReleaseImage (&actual_image_masked);
  cvReleaseImage (&substract_background_to_actual);
  cvReleaseImage (&low_speed_map);
  cvReleaseImage (&high_speed_map);
  cvReleaseImage (&actual_motion);
  cvReleaseImage (&binary_actual_motion);

  gst_buffer_unmap (frame->buffer, &info);

  return GST_FLOW_OK;
}

static void
kms_crowd_detector_class_init (KmsCrowdDetectorClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstBaseTransformClass *base_transform_class =
      GST_BASE_TRANSFORM_CLASS (klass);
  GstVideoFilterClass *video_filter_class = GST_VIDEO_FILTER_CLASS (klass);

  /* Setting up pads and setting metadata should be moved to
     base_class_init if you intend to subclass this class. */
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("src", GST_PAD_SRC, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SRC_CAPS)));
  gst_element_class_add_pad_template (GST_ELEMENT_CLASS (klass),
      gst_pad_template_new ("sink", GST_PAD_SINK, GST_PAD_ALWAYS,
          gst_caps_from_string (VIDEO_SINK_CAPS)));

  gst_element_class_set_static_metadata (GST_ELEMENT_CLASS (klass),
      "Crowd detector element", "Video/Filter",
      "Detects crowded areas based on movement detection",
      "Francisco Rivero <fj.riverog@gmail.com>");

  gobject_class->set_property = kms_crowd_detector_set_property;
  gobject_class->get_property = kms_crowd_detector_get_property;
  gobject_class->dispose = kms_crowd_detector_dispose;
  gobject_class->finalize = kms_crowd_detector_finalize;
  base_transform_class->start = GST_DEBUG_FUNCPTR (kms_crowd_detector_start);
  base_transform_class->stop = GST_DEBUG_FUNCPTR (kms_crowd_detector_stop);
  video_filter_class->set_info =
      GST_DEBUG_FUNCPTR (kms_crowd_detector_set_info);
  video_filter_class->transform_frame_ip =
      GST_DEBUG_FUNCPTR (kms_crowd_detector_transform_frame_ip);

  g_object_class_install_property (gobject_class, PROP_SHOW_DEBUG_INFO,
      g_param_spec_boolean ("show-debug-info", "show debug info",
          "show debug info", FALSE, G_PARAM_READWRITE));

  g_object_class_install_property (gobject_class, PROP_ROIS,
      g_param_spec_boxed ("rois", "rois",
          "set regions of interest to analize",
          GST_TYPE_STRUCTURE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_PROCESSING_WIDTH,
      g_param_spec_int ("processing-width", "processing width",
          "The processing image will be resized to this width (in pixels)", 160,
          1280, 320, G_PARAM_READWRITE));

  /* Registers a private structure for the instantiatable type */
  g_type_class_add_private (klass, sizeof (KmsCrowdDetectorPrivate));
}

static void
kms_crowd_detector_init (KmsCrowdDetector * crowddetector)
{
  crowddetector->priv = KMS_CROWD_DETECTOR_GET_PRIVATE (crowddetector);
  crowddetector->priv->actual_image = NULL;
  crowddetector->priv->previous_lbp = NULL;
  crowddetector->priv->frame_previous_gray = NULL;
  crowddetector->priv->num_rois = 0;
  crowddetector->priv->curves = NULL;
  crowddetector->priv->curves_original = NULL;
  crowddetector->priv->curves_percentages = NULL;
  crowddetector->priv->n_points = NULL;
  crowddetector->priv->rois = NULL;
  crowddetector->priv->rois_data = NULL;
  crowddetector->priv->pixels_rois_counted = FALSE;
  crowddetector->priv->processing_width = DEFAULT_MAX_WIDTH;

  g_rec_mutex_init (&crowddetector->priv->mutex);
}

gboolean
kms_crowd_detector_plugin_init (GstPlugin * plugin)
{
  return gst_element_register (plugin, PLUGIN_NAME, GST_RANK_NONE,
      KMS_TYPE_CROWD_DETECTOR);
}
