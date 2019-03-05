set(KMS_TEST_UTILS_SOURCES
  kmstestutils.h kmstestutils.c
)
add_library(kmsfilterstestutils ${KMS_TEST_UTILS_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmsfilterstestutils)
endif()
target_link_libraries(kmsfilterstestutils
  ${gstreamer-1.5_LIBRARIES}
  ${gstreamer-base-1.5_LIBRARIES}
  ${gstreamer-sdp-1.5_LIBRARIES}
  ${gstreamer-pbutils-1.5_LIBRARIES}
)
set_property(TARGET kmsfilterstestutils
  PROPERTY INCLUDE_DIRECTORIES
    ${gstreamer-1.5_INCLUDE_DIRS}
)

include(TestHelpers)
set(TEST_VARIABLES
  "GST_PLUGIN_PATH=${CMAKE_BINARY_DIR}"
  # HTTP requests have 60s timeout for the worst case, plus allow some time for
  # processing the tests
  "CK_DEFAULT_TIMEOUT=120"
)
list(APPEND SUPPRESSIONS
  "${CMAKE_CURRENT_SOURCE_DIR}/valgrind.supp")

add_subdirectory(element)
