#ifndef __KMS_UTILS_H__
#define __KMS_UTILS_H__

#include "gst/gst.h"

void kms_utils_set_valve_drop (GstElement * valve, gboolean drop);
void kms_utils_debug_graph_delay (GstBin * bin, guint interval);

#endif /* __KMS_UTILS_H__ */
