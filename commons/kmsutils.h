/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
#ifndef __KMS_UTILS_H__
#define __KMS_UTILS_H__

#include "gst/gst.h"

void kms_utils_set_valve_drop (GstElement * valve, gboolean drop);
void kms_utils_debug_graph_delay (GstBin * bin, guint interval);
gboolean kms_is_valid_uri (const gchar * url);

gboolean gst_element_sync_state_with_parent_target_state (GstElement * element);

#endif /* __KMS_UTILS_H__ */
