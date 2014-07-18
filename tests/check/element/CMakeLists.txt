set (SUPRESSIONS "${CMAKE_CURRENT_SOURCE_DIR}/../valgrind.supp")

add_test_program (test_httpendpoint httpendpoint.c)
add_dependencies(test_httpendpoint kmsplugin)
target_include_directories(test_httpendpoint PRIVATE
                           ${GSTREAMER_INCLUDE_DIRS}
                           ${GSTREAMER_CHECK_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
target_link_libraries(test_httpendpoint
                      ${GSTREAMER_LIBRARIES}
                      ${GSTREAMER_CHECK_LIBRARIES})
