set(KMS_TEST_UTILS_SOURCES
  kmstestutils.h kmstestutils.c
  )

add_library(kmsfilterstestutils ${KMS_TEST_UTILS_SOURCES})

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
