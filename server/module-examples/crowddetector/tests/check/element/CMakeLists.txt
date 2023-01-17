set (SUPRESSIONS "${CMAKE_CURRENT_SOURCE_DIR}/../valgrind.supp")

 add_test_program (test_crowddetector crowddetector.c)
 add_dependencies(test_crowddetector crowddetector)
 target_include_directories(test_crowddetector PRIVATE
                            ${KMSCORE_INCLUDE_DIRS}
                            ${GSTREAMER_INCLUDE_DIRS}
                            ${GSTREAMER_CHECK_INCLUDE_DIRS}
                            "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
 target_link_libraries(test_crowddetector
                       ${GSTREAMER_LIBRARIES}
                       ${GSTREAMER_CHECK_LIBRARIES}
                       kmstestutils)
