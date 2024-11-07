#
# Protobuf/Grpc source files
#
set(PROTO_FILES
    videoSampler/sampleImage.proto
)

#
# Add Library target with protobuf sources
#
#add_library(videoSampler ${PROTO_FILES})
#target_link_libraries(videoSampler
#    PUBLIC
#        protobuf::libprotobuf
#        gRPC::grpc
#        gRPC::grpc++
#)
#target_include_directories(videoSampler PUBLIC ${CMAKE_CURRENT_BINARY_DIR})

#
# Compile protobuf and grpc files in myproto target to cpp
#
#get_target_property(grpc_cpp_plugin_location gRPC::grpc_cpp_plugin LOCATION)
#protobuf_generate(TARGET videoSampler LANGUAGE cpp)
#protobuf_generate(TARGET videoSampler LANGUAGE grpc GENERATE_EXTENSIONS .grpc.pb.h .grpc.pb.cpp PLUGIN "protoc-gen-grpc=${grpc_cpp_plugin_location}")


set(PROTO_SRC_DIR ${CMAKE_CURRENT_BINARY_DIR}/proto-src)
file(MAKE_DIRECTORY ${PROTO_SRC_DIR})
include_directories(${PROTO_SRC_DIR})
