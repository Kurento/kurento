/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

#include "DotGraph.hpp"
#include <cstring>

namespace kurento
{

static GstDebugGraphDetails
convert_details (std::shared_ptr<GstreamerDotDetails> details)
{
  switch (details->getValue() ) {
  case GstreamerDotDetails::SHOW_MEDIA_TYPE:
    return GST_DEBUG_GRAPH_SHOW_MEDIA_TYPE;

  case GstreamerDotDetails::SHOW_CAPS_DETAILS:
    return GST_DEBUG_GRAPH_SHOW_CAPS_DETAILS;

  case GstreamerDotDetails::SHOW_NON_DEFAULT_PARAMS:
    return GST_DEBUG_GRAPH_SHOW_NON_DEFAULT_PARAMS;

  case GstreamerDotDetails::SHOW_STATES:
    return GST_DEBUG_GRAPH_SHOW_STATES;

  case GstreamerDotDetails::SHOW_FULL_PARAMS:
    return GST_DEBUG_GRAPH_SHOW_FULL_PARAMS;

  case GstreamerDotDetails::SHOW_ALL:
    return GST_DEBUG_GRAPH_SHOW_ALL;

  case GstreamerDotDetails::SHOW_VERBOSE:
  default:
    return GST_DEBUG_GRAPH_SHOW_ALL;
  }
}

std::string
generateDotGraph (GstBin *bin, std::shared_ptr<GstreamerDotDetails> details)
{
  std::string retString;
  gchar *data = gst_debug_bin_to_dot_data (bin, convert_details (details) );

  retString = std::string (data);
  g_free (data);

  return retString;
}

void
dumpDotGraph (GstBin *bin, std::shared_ptr<GstreamerDotDetails> details, std::string name)
{
  GST_DEBUG_BIN_TO_DOT_FILE_WITH_TS (
      bin,
      convert_details (details),
      name.c_str ()
  );
}


} /* kurento */
