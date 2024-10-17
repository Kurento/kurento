set(PROTO_FILES
    proto/videoSampler/sampleImage.proto
)


set(PROTO_SRC_DIR ${CMAKE_CURRENT_BINARY_DIR}/proto-src)
file(MAKE_DIRECTORY ${PROTO_SRC_DIR})
include_directories(${PROTO_SRC_DIR})

protobuf_generate_cpp(PROTO_SRCS PROTO_HDRS ${PROTO_FILES})
grpc_generate_cpp(GRPC_SRCS GRPC_HDRS ${PROTO_SRC_DIR} ${PROTO_FILES})


include(CodeGenerator)

generate_code(
  MODELS ${CMAKE_CURRENT_SOURCE_DIR}/interface
  SERVER_IMPL_LIB_EXTRA_SOURCES
      ${PROTO_SRCS}
      ${GRPC_SRCS}
  SERVER_IMPL_LIB_EXTRA_INCLUDE_DIRS
      ${CMAKE_CURRENT_BINARY_DIR}/..
      ${CMAKE_CURRENT_BINARY_DIR}
      ${PROTO_SRC_DIR}
      ${VIDEOSAMPLER_PROTO_INCLUDE_DIR}
      ${CMAKE_CURRENT_SOURCE_DIR}/implementation
  SERVER_IMPL_LIB_EXTRA_LIBRARIES
      ${VIDEOSAMPLER_PROTO_LIB_DIR}
      gRPC::grpc++_reflection
      gRPC::grpc++
      gRPC::grpc
      protobuf::libprotobuf
  MODULE_EXTRA_INCLUDE_DIRS
      ${PROTO_SRC_DIR}
  SERVER_STUB_DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/implementation/objects
)

