#ifndef __PROCESS_H__
#define __PROCESS_H__

#include <glib.h>

#include <opencv/cv.h>

G_BEGIN_DECLS

int detect_marker(IplImage* img, gboolean show_debug_info);
int set_overlay(const char *overlay_image, const char *overlay_text);

G_END_DECLS

#endif /* __PROCESS_H__ */
