set (SUPRESSIONS "${CMAKE_CURRENT_SOURCE_DIR}/../valgrind.supp")

add_test_program (test_faceoverlay faceoverlay.c)
add_dependencies(test_faceoverlay faceoverlay)
target_include_directories(test_faceoverlay PRIVATE
                           ${KMSCORE_INCLUDE_DIRS}
                           ${GSTREAMER_INCLUDE_DIRS}
                           ${GSTREAMER_CHECK_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
target_link_libraries(test_faceoverlay
                      ${GSTREAMER_LIBRARIES}
                      ${GSTREAMER_CHECK_LIBRARIES}
                      kmstestutils)

add_test_program (test_movementdetector movementdetector.c)
add_dependencies(test_movementdetector movementdetector)
target_include_directories(test_movementdetector PRIVATE
                           ${KMSCORE_INCLUDE_DIRS}
                           ${GSTREAMER_INCLUDE_DIRS}
                           ${GSTREAMER_CHECK_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
target_link_libraries(test_movementdetector
                      ${GSTREAMER_LIBRARIES}
                      ${GSTREAMER_CHECK_LIBRARIES}
                      kmstestutils)