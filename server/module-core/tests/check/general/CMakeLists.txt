add_test_program (test_utils utils.c)
add_dependencies(test_utils ${LIBRARY_NAME})
target_include_directories(test_utils PRIVATE
                           ${gstreamer-1.0_INCLUDE_DIRS}
                           ${gstreamer-check-1.0_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins/commons/")
target_link_libraries(test_utils
                      ${gstreamer-1.0_LIBRARIES}
                      ${gstreamer-check-1.0_LIBRARIES}
                      kmsgstcommons)

add_test_program (test_refcounts refcounts.c)
add_dependencies(test_refcounts ${LIBRARY_NAME})
target_include_directories(test_refcounts PRIVATE
                           ${gstreamer-1.0_INCLUDE_DIRS}
                           ${gstreamer-check-1.0_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins/commons/")
target_link_libraries(test_refcounts
                      ${gstreamer-1.0_LIBRARIES}
                      ${gstreamer-check-1.0_LIBRARIES}
                      kmsgstcommons)

add_test_program (test_rembmanager rembmanager.c)
add_dependencies(test_rembmanager ${LIBRARY_NAME})
target_include_directories(test_rembmanager PRIVATE
                           ${gstreamer-1.0_INCLUDE_DIRS}
                           ${gstreamer-check-1.0_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins/commons/")
target_link_libraries(test_rembmanager
                      ${gstreamer-1.0_LIBRARIES}
                      ${gstreamer-check-1.0_LIBRARIES}
                      kmsgstcommons)

add_test_program (test_rtpsync rtpsync.c)
add_dependencies(test_rtpsync ${LIBRARY_NAME})
target_include_directories(test_rtpsync PRIVATE
                           ${gstreamer-1.0_INCLUDE_DIRS}
                           ${gstreamer-check-1.0_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins/commons")
target_link_libraries(test_rtpsync
                      ${gstreamer-1.0_LIBRARIES}
                      ${gstreamer-rtp-1.0_LIBRARIES}
                      ${gstreamer-check-1.0_LIBRARIES}
                      kmsgstcommons)
