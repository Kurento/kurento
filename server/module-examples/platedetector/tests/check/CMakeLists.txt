include_directories(
  ${GSTREAMER_INCLUDE_DIRS}
  ${GSTREAMER_VIDEO_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
)

set(KMS_TEST_UTILS_SOURCES
  kmstestutils.h kmstestutils.c
)
add_library(kmsplatetestutils ${KMS_TEST_UTILS_SOURCES})
target_link_libraries(kmsplatetestutils
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_BASE_LIBRARIES}
  ${GSTREAMER_SDP_LIBRARIES}
  ${GSTREAMER_PBUTILS_LIBRARIES}
)

include(TestHelpers)
set(TEST_VARIABLES
  "GST_PLUGIN_PATH=${CMAKE_BINARY_DIR}"
  "CK_DEFAULT_TIMEOUT=50"
  "TESSDATA_PREFIX=${CMAKE_CURRENT_SOURCE_DIR}/../../src/gst-plugins/platedetector/tessdata"
)
set(SUPPRESSIONS "${CMAKE_CURRENT_SOURCE_DIR}/valgrind.supp")

add_subdirectory(element)
