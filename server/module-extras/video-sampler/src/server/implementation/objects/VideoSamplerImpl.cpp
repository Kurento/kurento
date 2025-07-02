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
#include <VideoSamplerImplFactory.hpp>
#include "VideoSamplerImpl.hpp"
#include <jsonrpc/JsonSerializer.hpp>
#include <KurentoException.hpp>

#include <grpc++/grpc++.h>
#include <sampleImage.grpc.pb.h>



#define GST_CAT_DEFAULT kurento_video_sampler_impl
GST_DEBUG_CATEGORY_STATIC (GST_CAT_DEFAULT);
#define GST_DEFAULT_NAME "KurentoVideoSamplerImpl"

#define FACTORY_NAME "videosampler"

using grpc::Channel;
using grpc::ClientContext;
using grpc::Status;
using videoSampler::SampleImage;
using videoSampler::Empty;
using videoSampler::ImageDeliver;



class VideoSamplerGRPC: public kurento::module::videosampler::VideoSamplerImpl
{
  public: 
      VideoSamplerGRPC (const boost::property_tree::ptree &config,
                  std::shared_ptr<kurento::MediaPipeline> mediaPipeline,
                  int framePeriod,
                  std::shared_ptr<kurento::module::videosampler::ImageDelivery> imageDeliveryMethod,
                  int height,
                  int width,
                  std::shared_ptr<kurento::module::videosampler::ImageEncoding> imageEncoding,
                  std::string endpointUrl,
                  const std::string &metadata);

      bool send_frame_data (guint8* data, guint len);
      void sendImageDeliveredEvent (bool delivered);

      ~VideoSamplerGRPC ();


  private:
    std::shared_ptr<videoSampler::ImageDeliver::Stub> imageDeliverStub;
    gulong frameSampleId = 0;


};

static void 
kms_videosampler_handle_sample(gpointer obj, GByteArray *byte_array, VideoSamplerGRPC *self) 
{
  if (byte_array->len > 0) {
    bool delivered = self->send_frame_data(byte_array->data, byte_array->len);
    self->sendImageDeliveredEvent(delivered);
  }
}

VideoSamplerGRPC::VideoSamplerGRPC (const boost::property_tree::ptree &config,
            std::shared_ptr<kurento::MediaPipeline> mediaPipeline,
            int framePeriod,
            std::shared_ptr<kurento::module::videosampler::ImageDelivery> imageDeliveryMethod,
            int height,
            int width,
            std::shared_ptr<kurento::module::videosampler::ImageEncoding> imageEncoding,
            std::string endpointUrl,
            const std::string &metadata): VideoSamplerImpl (config, mediaPipeline, framePeriod, imageDeliveryMethod, height, width, imageEncoding, endpointUrl, metadata)
{
  imageDeliverStub = nullptr;
  g_object_set(element, "frame-period", msFramePeriod,
                        "height", height,
                        "width", width,
                        "image-encoding", encoding->getString().c_str(),
                        NULL);
  if (getEndpointUrl() != "") {
    std::shared_ptr<grpc::Channel> channel =  grpc::CreateChannel (getEndpointUrl(),grpc::InsecureChannelCredentials());

    if (channel != nullptr) {
      imageDeliverStub = ImageDeliver::NewStub(channel);

      // Once the channel is established, we connect the signal
      frameSampleId = g_signal_connect (G_OBJECT (element), "frame-sample",
          G_CALLBACK (kms_videosampler_handle_sample), this);
    }
  }
}

static std::string 
epochToString() {
  // Obtener el tiempo actual en formato epoch
  auto now = std::chrono::system_clock::now();
  std::time_t epoch_time = std::chrono::system_clock::to_time_t(now);

  // Convertir el tiempo epoch a una cadena de caracteres
  std::stringstream ss;
  ss << std::ctime(&epoch_time);

  // Eliminar el carácter de nueva línea al final de la cadena
  std::string time_str = ss.str();
  if (!time_str.empty() && time_str.back() == '\n') {
    time_str.pop_back();
  }

  return time_str;
}

bool
VideoSamplerGRPC::send_frame_data (guint8 *data, guint len)
{
  SampleImage request;
  Empty reply;
  ClientContext context;
  Status status;

  request.set_codec (getEncodingStr());
  request.set_data (data, len);
  request.set_timestamp (epochToString());
  request.set_metadata (getMetadata());

  status = imageDeliverStub->deliverImage(&context, request, &reply);

  // Act upon its status.
  if (!status.ok()) {
    GST_WARNING_OBJECT (this, "Could not deliver image to gRPC, error %d, %s", status.error_code(), status.error_message().c_str());
    return false;
  }
  return true;  
}

void 
VideoSamplerGRPC::sendImageDeliveredEvent (bool delivered)
{
  try {
    kurento::module::videosampler::SampleImageDelivered event (shared_from_this (), kurento::module::videosampler::SampleImageDelivered::getName (), delivered);
    sigcSignalEmit(signalSampleImageDelivered, event);
  } catch (const std::bad_weak_ptr &e) {
    // shared_from_this()
    GST_ERROR_OBJECT (this, "BUG creating %s: %s", kurento::module::videosampler::SampleImageDelivered::getName ().c_str (),
        e.what ());
  }
}

VideoSamplerGRPC::~VideoSamplerGRPC ()
{
  if (frameSampleId != 0L) {
    g_signal_handler_disconnect (element, frameSampleId);
    frameSampleId = 0L;
  }
}





namespace kurento
{
namespace module
{
namespace videosampler
{

VideoSamplerImpl::VideoSamplerImpl (const boost::property_tree::ptree &config,
                                    std::shared_ptr<MediaPipeline> mediaPipeline,
                                    int framePeriod,
                                    std::shared_ptr<ImageDelivery> imageDeliveryMethod,
                                    int h,
                                    int w,
                                    std::shared_ptr<ImageEncoding> imageEncoding,
                                    std::string url,
                                    std::string metadata)  : 
                                        MediaElementImpl (config, 
                                                          std::dynamic_pointer_cast<MediaPipelineImpl> (mediaPipeline), 
                                                          FACTORY_NAME ),
                                        encoding(imageEncoding),
                                        delivery(imageDeliveryMethod),
                                        endpointUrl (url),
                                        width(w),
                                        height(h),
                                        msFramePeriod(framePeriod),
                                        metadata(metadata)

{
}


MediaObjectImpl *
VideoSamplerImplFactory::createObject (const boost::property_tree::ptree &conf, 
                                       std::shared_ptr<MediaPipeline> mediaPipeline, 
                                       int framePeriod, 
                                       std::shared_ptr<ImageDelivery> imageDeliveryMethod, 
                                       int height, 
                                       int width, 
                                       std::shared_ptr<ImageEncoding> imageEncoding, 
                                       const std::string &endpointUrl,
                                       const std::string &metadata) const
{
  return new VideoSamplerGRPC (conf, mediaPipeline, framePeriod, imageDeliveryMethod, height, width, imageEncoding, endpointUrl, metadata);
}



VideoSamplerImpl::StaticConstructor VideoSamplerImpl::staticConstructor;

VideoSamplerImpl::StaticConstructor::StaticConstructor()
{
  GST_DEBUG_CATEGORY_INIT (GST_CAT_DEFAULT, GST_DEFAULT_NAME, 0,
                           GST_DEFAULT_NAME);
}

} /* videosampler */
} /* module */
} /* kurento */
