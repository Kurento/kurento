set (SUPRESSIONS "${CMAKE_CURRENT_SOURCE_DIR}/../valgrind.supp")

add_test_program (test_chroma chroma.c)
add_dependencies(test_chroma chroma)
target_include_directories(test_chroma PRIVATE
  ${KMSCORE_INCLUDE_DIRS}
  ${GSTREAMER_INCLUDE_DIRS}
  ${GSTREAMER_CHECK_INCLUDE_DIRS}
  "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins"
)
target_link_libraries(test_chroma
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_CHECK_LIBRARIES}
  kmstestutils
)
