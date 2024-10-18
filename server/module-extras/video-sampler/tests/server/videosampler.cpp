#define BOOST_TEST_STATIC_LINK
#define BOOST_TEST_PROTECTED_VIRTUAL

#include <boost/test/included/unit_test.hpp>
#include <MediaPipelineImpl.hpp>
#include <ModuleManager.hpp>
#include <KurentoException.hpp>
#include <MediaSet.hpp>
#include <MediaElementImpl.hpp>
#include <MediaFlowInStateChanged.hpp>
#include <MediaFlowState.hpp>
#include <MediaType.hpp>
#include <GstreamerDotDetails.hpp>
#include <sigc++/connection.h>

#include <RegisterParent.hpp>

#include <PassThroughImpl.hpp>
#include <VideoSamplerImpl.hpp>

#include <thread>

#include <grpc++/grpc++.h>
#include <sampleImage.grpc.pb.h>



#define TEST_ENDPOINT "0.0.0.0:50051"

kurento::ModuleManager moduleManager;

namespace kurento
{
  ModuleManager& getModuleManager ();
}


kurento::ModuleManager& kurento::getModuleManager ()
{
  return moduleManager;
}



using namespace kurento;
using namespace kurento::module::videosampler;
using namespace boost::unit_test;
using namespace grpc;
using namespace videoSampler;

boost::property_tree::ptree config;

struct GF {
  GF();
  ~GF();
};

BOOST_GLOBAL_FIXTURE (GF);

GF::GF()
{
  boost::property_tree::ptree ac, audioCodecs, vc, videoCodecs;
  gst_init(nullptr, nullptr);
  moduleManager.loadModulesFromDirectories ("../../src/server:./:../../../../module-core");

  config.add ("configPath", "../../../tests" );
  config.add ("modules.kurento.SdpEndpoint.numAudioMedias", 1);
  config.add ("modules.kurento.SdpEndpoint.numVideoMedias", 1);

  ac.put ("name", "opus/48000/2");
  audioCodecs.push_back (std::make_pair ("", ac) );
  config.add_child ("modules.kurento.SdpEndpoint.audioCodecs", audioCodecs);

  vc.put ("name", "H264/90000");
  videoCodecs.push_back (std::make_pair ("", vc) );
  config.add_child ("modules.kurento.SdpEndpoint.videoCodecs", videoCodecs);
}

GF::~GF()
{
  sleep (3);
  MediaSet::deleteMediaSet();
}

static void
dumpPipeline (std::shared_ptr<MediaPipeline> pipeline, std::string fileName)
{
  std::string pipelineDot;
  std::shared_ptr<GstreamerDotDetails> details (new GstreamerDotDetails ("SHOW_ALL"));

  pipelineDot = pipeline->getGstreamerDot (details);
  std::ofstream out(fileName);

  out << pipelineDot;
  out.close ();

}

void
dumpPipeline (std::string pipelineId, std::string fileName)
{
  std::shared_ptr<MediaPipeline> pipeline = std::dynamic_pointer_cast<MediaPipeline> (MediaSet::getMediaSet ()->getMediaObject (pipelineId));
  dumpPipeline (pipeline, fileName);

//  MediaSet::getMediaSet ()->release (pipelineId);
}

static void
checkPipelineEmpty (std::string pipelineId)
{
  std::shared_ptr<MediaPipeline> pipeline = std::dynamic_pointer_cast<MediaPipeline> (MediaSet::getMediaSet ()->getMediaObject (pipelineId));
  std::vector<std::shared_ptr<MediaObject> > elements =  pipeline->getChildren ();
  std::stringstream strStream;

  for (auto & elem : elements) {
    BOOST_TEST_MESSAGE ("Still Media Element alive");
    BOOST_TEST_MESSAGE (elem->getId ());
  }

  if (!elements.empty ()) {
    BOOST_TEST_MESSAGE ("It should not remain any media element");
  } else {
    BOOST_TEST_MESSAGE ("Media pipeline empty");
  }

  strStream << pipelineId << "_final.dot";
  dumpPipeline (pipeline, strStream.str ());

  //MediaSet::getMediaSet ()->release (pipeline->getId ());
}

static std::string
createPipeline ()
{
  return moduleManager.getFactory ("MediaPipeline")->createObject (
                      config, "",
                      Json::Value() )->getId();
}

static void
releasePipeline (std::string pipelineId)
{
  MediaSet::getMediaSet ()->release (pipelineId);
}

class ImageDeliverImpl final : public ImageDeliver::Service {
  public:
  Status deliverImage(ServerContext* context, const SampleImage* request, Empty* response)
  {
    BOOST_TEST_MESSAGE ("Received image");
    std::string codec = request->codec();
    std::string data = request->data();
    std::string timestamp = request->timestamp();

    std::ofstream file("./"+timestamp+".jpg", std::ios::out | std::ios::binary);
    if (!file) {
      BOOST_ERROR ("No se pudo abrir el fichero para escribir.");
    } else {
      // Escribir el array de bytes en el fichero
      file.write(data.data(), data.length());

      // Cerrar el fichero
      file.close();
    }
    return ::grpc::Status::OK;
  }

  std::unique_ptr<Server> server;
};


static void
grpcServer (ImageDeliverImpl *service)
{
  std::string server_address(TEST_ENDPOINT);

  ServerBuilder builder;
  // Listen on the given address without any authentication mechanism.
  builder.AddListeningPort(server_address, grpc::InsecureServerCredentials());
  // Register "service" as the instance through which we'll communicate with
  // clients. In this case it corresponds to an *synchronous* service.
  builder.RegisterService(service);
  // Finally assemble the server.
  service->server = builder.BuildAndStart();
  //std::cout << "Server listening on " << server_address << std::endl;

  // Wait for the server to shutdown. Note that some other thread must be
  // responsible for shutting down the server for this call to ever return.
  service->server->Wait();

}


static std::shared_ptr<VideoSamplerImpl>
createVideoSampler (std::string mediaPipelineId)
{
  std::shared_ptr <kurento::MediaObjectImpl> videosampler;
  Json::Value constructorParams;

  constructorParams ["mediaPipeline"] = mediaPipelineId;
  constructorParams ["framePeriod"] = 1000;
  constructorParams ["imageDeliveryMethod"] = "GRPC";
  constructorParams ["height"] = 480;
  constructorParams ["imageEncoding"] = "JPEG";
  constructorParams ["endpointUrl"] = TEST_ENDPOINT;


  videosampler = moduleManager.getFactory ("VideoSampler")->createObject (
                  config, "",
                  constructorParams );

  return std::dynamic_pointer_cast <VideoSamplerImpl> (videosampler);
}

static void
releaseVideoSampler (std::shared_ptr<VideoSamplerImpl> subscriber)
{
  std::string id = std::dynamic_pointer_cast<MediaElement> (subscriber)->getId();

  subscriber.reset();
  MediaSet::getMediaSet ()->release (id);
}

static std::shared_ptr<MediaElementImpl> createTestSrc(std::string mediaPipelineId) {
  std::shared_ptr <MediaElementImpl> src = std::dynamic_pointer_cast
      <MediaElementImpl> (MediaSet::getMediaSet()->ref (new  MediaElementImpl (
                            boost::property_tree::ptree(),
                            MediaSet::getMediaSet()->getMediaObject (mediaPipelineId),
                            "dummysrc") ) );

  g_object_set (src->getGstreamerElement(), "audio", TRUE, "video", TRUE, NULL);

  return std::dynamic_pointer_cast <MediaElementImpl> (src);
}

static void
releaseTestElement (std::shared_ptr<MediaElementImpl> &ep)
{
  std::string id = ep->getId();

  ep.reset();
  MediaSet::getMediaSet ()->release (id);
}



static std::shared_ptr <PassThroughImpl>
createPassThrough (std::string mediaPipelineId)
{
  std::shared_ptr <kurento::MediaObjectImpl> pt;
  Json::Value constructorParams;

  constructorParams ["mediaPipeline"] = mediaPipelineId;

  pt = moduleManager.getFactory ("PassThrough")->createObject (
                  config, "",
                  constructorParams );

  return std::dynamic_pointer_cast <PassThroughImpl> (pt);
}

static void
releasePassTrhough (std::shared_ptr<PassThroughImpl> &ep)
{
  std::string id = ep->getId();

  ep.reset();
  MediaSet::getMediaSet ()->release (id);
}


static void
test_videosampler ()
{
  std::atomic<bool> media_state_changed (false);
  std::condition_variable cv;
  std::mutex mtx;
  std::unique_lock<std::mutex> lck (mtx);

  std::string pipelineId = createPipeline ();

  std::shared_ptr<VideoSamplerImpl> videosampler = createVideoSampler (pipelineId);
  std::shared_ptr<PassThroughImpl> passthrough = createPassThrough (pipelineId);
  std::shared_ptr<MediaElementImpl> src = createTestSrc(pipelineId);

  ImageDeliverImpl service;
  std::thread serverThread (grpcServer, &service);
  int count = 5;

  if (videosampler == nullptr) {
      BOOST_ERROR ("Could not create videosampler");
  } else {
      BOOST_TEST_MESSAGE ("VideoSampler created");
      src->connect(videosampler);
      std::dynamic_pointer_cast<MediaElementImpl>(videosampler)->connect(passthrough);

      sigc::connection conn =  videosampler->signalSampleImageDelivered.connect([&] (SampleImageDelivered event) {
        count--;
        if (count <= 0) {
          service.server->Shutdown();
        }
      });
      sleep(1);
      dumpPipeline (pipelineId, "videosampler.dot");
      serverThread.join ();
      dumpPipeline (pipelineId, "videosampler2.dot");

      videosampler->disconnect (passthrough);
      src->disconnect(videosampler);

      releasePassTrhough (passthrough);
      releaseVideoSampler (videosampler);
      releaseTestElement (src);
  }

  checkPipelineEmpty (pipelineId);
  releasePipeline (pipelineId);
}


test_suite *
init_unit_test_suite ( int , char *[] )
{
  test_suite *test = BOOST_TEST_SUITE ( "videosampler" );

  test->add (BOOST_TEST_CASE ( &test_videosampler ), 0, /* timeout */ 15000);

  return test;
}