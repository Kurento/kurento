/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
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

#include <gst/gst.h>
#include "MediaPipeline.hpp"
#include "MediaPipelineImpl.hpp"
#include <ZBarFilterImplFactory.hpp>
#include "ZBarFilterImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>
#include <gst/gst.h>
#include "SignalHandler.hpp"

#define GST_CAT_DEFAULT kurento_zbar_filter_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoZBarFilterImpl"

namespace kurento
{
void ZBarFilterImpl::busMessage (GstMessage *message)
{

  if (GST_MESSAGE_SRC (message) == GST_OBJECT (zbar) &&
      GST_MESSAGE_TYPE (message) == GST_MESSAGE_ELEMENT) {
    const GstStructure *st;
    guint64 ts;
    gchar *type, *symbol;

    st = gst_message_get_structure (message);

    if (g_strcmp0 (gst_structure_get_name (st), "barcode") != 0) {
      return;
    }

    if (!gst_structure_get (st, "timestamp", G_TYPE_UINT64, &ts,
                            "type", G_TYPE_STRING, &type, "symbol",
                            G_TYPE_STRING, &symbol, NULL) ) {
      return;
    }

    std::string symbolStr (symbol);
    std::string typeStr (type);

    g_free (type);
    g_free (symbol);

    barcodeDetected (ts, typeStr, symbolStr);
  }
}

void ZBarFilterImpl::postConstructor ()
{
  GstBus *bus;
  std::shared_ptr<MediaPipelineImpl> pipe;

  FilterImpl::postConstructor ();

  pipe = std::dynamic_pointer_cast<MediaPipelineImpl> (getMediaPipeline() );

  bus = gst_pipeline_get_bus (GST_PIPELINE (pipe->getPipeline() ) );

  bus_handler_id = register_signal_handler (G_OBJECT (bus),
                   "message",
                   std::function <void (GstElement *, GstMessage *) >
                   (std::bind (&ZBarFilterImpl::busMessage, this,
                               std::placeholders::_2) ),
                   std::dynamic_pointer_cast<ZBarFilterImpl>
                   (shared_from_this() ) );
  g_object_unref (bus);
}

ZBarFilterImpl::ZBarFilterImpl (const boost::property_tree::ptree &conf,
                                std::shared_ptr<MediaPipeline> mediaPipeline) :
  FilterImpl (conf, std::dynamic_pointer_cast<MediaObjectImpl> ( mediaPipeline) )
{
  g_object_set (element, "filter-factory", "zbar", NULL);
  g_object_get (G_OBJECT (element), "filter", &zbar, NULL);

  if (zbar == nullptr) {
    throw KurentoException (MEDIA_OBJECT_NOT_FOUND, "ZBarFilter plugin not available: zbar");
  }

  g_object_set (G_OBJECT (zbar), "qos", FALSE, NULL);

  bus_handler_id = 0;
  // There is no need to reference zbar becase its live cycle is the same as the filter live cycle
  g_object_unref (zbar);
}

void
ZBarFilterImpl::barcodeDetected (guint64 ts, std::string &type,
                                 std::string &symbol)
{
  if (lastSymbol != symbol || lastType != type ||
      lastTs == G_GUINT64_CONSTANT (0) || ( (ts - lastTs) >= GST_SECOND) ) {
    // TODO: Hold a lock here to avoid race conditions

    lastSymbol = symbol;
    lastType = type;
    lastTs = ts;

    try {
      CodeFound event (shared_from_this (), CodeFound::getName (), type,
          symbol);
      sigcSignalEmit(signalCodeFound, event);
    } catch (const std::bad_weak_ptr &e) {
      // shared_from_this()
      GST_ERROR ("BUG creating %s: %s", CodeFound::getName ().c_str (),
          e.what ());
    }
  }
}

MediaObjectImpl *
ZBarFilterImplFactory::createObject (const boost::property_tree::ptree &conf,
                                     std::shared_ptr<MediaPipeline>
                                     mediaPipeline) const
{
  return new ZBarFilterImpl (conf, mediaPipeline);
}

ZBarFilterImpl::~ZBarFilterImpl()
{
  std::shared_ptr<MediaPipelineImpl> pipe;

  if (bus_handler_id > 0) {
    pipe = std::dynamic_pointer_cast<MediaPipelineImpl> (getMediaPipeline() );
    GstBus *bus = gst_pipeline_get_bus (GST_PIPELINE (pipe->getPipeline() ) );
    unregister_signal_handler (bus, bus_handler_id);
    g_object_unref (bus);
  }
}

ZBarFilterImpl::StaticConstructor ZBarFilterImpl::staticConstructor;

ZBarFilterImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* kurento */
