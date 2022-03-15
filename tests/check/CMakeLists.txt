# Disable error when functions are unused. This allows commenting out calls to
# `tcase_add_test()` to skip running certain tests during a debug session.
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-error=unused-function")

set(KMS_TEST_UTILS_SOURCES
  kmstestutils.h kmstestutils.c
)

add_library(kmstestutils ${KMS_TEST_UTILS_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(kmstestutils)
endif()

target_link_libraries(kmstestutils
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-base-1.0_LIBRARIES}
  ${gstreamer-sdp-1.0_LIBRARIES}
  ${gstreamer-pbutils-1.0_LIBRARIES}
)
set_property(TARGET kmstestutils
  PROPERTY INCLUDE_DIRECTORIES
    ${gstreamer-1.0_INCLUDE_DIRS}
    ${gstreamer-video-1.0_INCLUDE_DIRS}
)

include(TestHelpers)

set(GST_DEBUG_DUMP_DOT_DIR "${CMAKE_BINARY_DIR}/debug/dots" CACHE PATH "Sets the directory where dot files will be dumped")
file(MAKE_DIRECTORY "${GST_DEBUG_DUMP_DOT_DIR}")

set(TEST_VARIABLES
  "GST_DEBUG_DUMP_DOT_DIR=${GST_DEBUG_DUMP_DOT_DIR}"
  "GST_PLUGIN_PATH=${CMAKE_BINARY_DIR}:$ENV{GST_PLUGIN_PATH}"
  "CK_DEFAULT_TIMEOUT=50"
)
set(VALGRIND_TEST_VARIABLES
  "${TEST_VARIABLES}"
  "CK_DEFAULT_TIMEOUT=1000"
)
list(APPEND SUPPRESSIONS
  "${CMAKE_CURRENT_SOURCE_DIR}/valgrind.supp")

add_subdirectory(element)
