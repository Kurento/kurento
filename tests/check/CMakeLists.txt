# Disable error when functions are unused
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-error=unused-function")

set(KMS_TEST_UTILS_SOURCES
  kmstestutils.h kmstestutils.c
)

add_library(kmstestutils ${KMS_TEST_UTILS_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmstestutils)
endif()

target_link_libraries(kmstestutils
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-base-1.5_LIBRARIES}
  ${gstreamer-sdp-1.5_LIBRARIES}
  ${gstreamer-pbutils-1.5_LIBRARIES}
)
set_property(TARGET kmstestutils
  PROPERTY INCLUDE_DIRECTORIES
    ${gstreamer-1.5_INCLUDE_DIRS}
    ${gstreamer-video-1.5_INCLUDE_DIRS}
)

include(TestHelpers)
set(TEST_VARIABLES
  "GST_PLUGIN_PATH=$ENV{GST_PLUGIN_PATH}:${CMAKE_BINARY_DIR}"
  "CK_DEFAULT_TIMEOUT=50"
)
set(VALGRIND_TEST_VARIABLES
  "${TEST_VARIABLES}"
  "CK_DEFAULT_TIMEOUT=1000"
)
list(APPEND SUPPRESSIONS
  "${CMAKE_CURRENT_SOURCE_DIR}/valgrind.supp")

add_subdirectory(element)
