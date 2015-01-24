set (SUPRESSIONS "${CMAKE_CURRENT_SOURCE_DIR}/../valgrind.supp")

# add_test_program (test_faceoverlay faceoverlay.c)
# add_dependencies(test_faceoverlay faceoverlay)
# target_include_directories(test_faceoverlay PRIVATE
#                            ${KMSCORE_INCLUDE_DIRS}
#                            ${gstreamer-1.0_INCLUDE_DIRS}
#                            ${gstreamer-check-1.0_INCLUDE_DIRS}
#                            "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
# target_link_libraries(test_faceoverlay
#                       ${gstreamer-1.0_LIBRARIES}
#                       ${gstreamer-check-1.0_LIBRARIES}
#                       kmstestutils)
#
# add_test_program (test_movementdetector movementdetector.c)
# add_dependencies(test_movementdetector movementdetector)
# target_include_directories(test_movementdetector PRIVATE
#                            ${KMSCORE_INCLUDE_DIRS}
#                            ${gstreamer-1.0_INCLUDE_DIRS}
#                            ${gstreamer-check-1.0_INCLUDE_DIRS}
#                            "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
# target_link_libraries(test_movementdetector
#                       ${gstreamer-1.0_LIBRARIES}
#                       ${gstreamer-check-1.0_LIBRARIES}
#                       kmstestutils)
