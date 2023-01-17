# Disable error when functions are unused. This allows commenting out calls
# to skip running certain tests during a debug session.
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-error=unused-function")

include_directories(
  ${CMAKE_CURRENT_SOURCE_DIR}/../../src/gst-plugins
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${gstreamer-video-1.0_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
)

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
add_subdirectory(general)

set(ENABLE_MEMORY_LEAKS_TESTS FALSE CACHE BOOL "Enable memory leaks tests")

if(${ENABLE_MEMORY_LEAKS_TESTS})
  add_subdirectory(memory_leaks)
endif()
