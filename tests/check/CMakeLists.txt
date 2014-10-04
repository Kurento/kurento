include_directories(
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${gstreamer-video-1.0_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
)

set(KMS_TEST_UTILS_SOURCES
  kmstestutils.h kmstestutils.c
  )

add_library(kmstestutils ${KMS_TEST_UTILS_SOURCES})

target_link_libraries(kmstestutils
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-base-1.0_LIBRARIES}
  ${gstreamer-sdp-1.0_LIBRARIES}
  ${gstreamer-pbutils-1.0_LIBRARIES}
)

include (TestHelpers)

set (SUPPRESSIONS "${CMAKE_CURRENT_SOURCE_DIR}/valgrind.supp")

set (TEST_PROPERTIES
  GST_PLUGIN_PATH=${CMAKE_BINARY_DIR}
  CK_DEFAULT_TIMEOUT=50
  )

add_subdirectory(element)

# if (${ENABLE_INTEGRATION_TESTS})
#   add_subdirectory(integration)
# endif()

# if (${ENABLE_MEMORY_LEAKS_TESTS})
#   add_subdirectory(memory_leaks)
# endif()
