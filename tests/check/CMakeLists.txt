# FIXME: Disable error when functions are unused
# This is needed to temporarily disable test "player_with_filter"
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-error=unused-function")

include_directories(
  ${GSTREAMER_INCLUDE_DIRS}
  ${GSTREAMER_VIDEO_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
)

set(KMS_TEST_UTILS_SOURCES
  kmstestutils.h kmstestutils.c
)
add_library(kmstestutils ${KMS_TEST_UTILS_SOURCES})
target_link_libraries(kmstestutils
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_BASE_LIBRARIES}
  ${GSTREAMER_SDP_LIBRARIES}
  ${GSTREAMER_PBUTILS_LIBRARIES}
)

include(TestHelpers)
set(TEST_VARIABLES
  "GST_PLUGIN_PATH=${CMAKE_BINARY_DIR}"
  "CK_DEFAULT_TIMEOUT=50"
)
set(SUPPRESSIONS "${CMAKE_CURRENT_SOURCE_DIR}/valgrind.supp")

add_subdirectory(element)
