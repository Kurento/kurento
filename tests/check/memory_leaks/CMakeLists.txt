add_test_program (test_memory_playerendpoint playerendpoint.c)
add_dependencies(test_memory_playerendpoint kmsplugin)
target_include_directories(test_memory_playerendpoint PRIVATE
                           ${GSTREAMER_INCLUDE_DIRS}
                           ${GSTREAMER_CHECK_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src")
target_link_libraries(test_memory_playerendpoint
                      ${GSTREAMER_LIBRARIES}
                      ${GSTREAMER_CHECK_LIBRARIES})

add_test_program (test_memory_recorderendpoint recorderendpoint.c)
add_dependencies(test_memory_recorderendpoint kmsplugin)
target_include_directories(test_memory_recorderendpoint PRIVATE
                           ${GSTREAMER_INCLUDE_DIRS}
                           ${GSTREAMER_CHECK_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src")
target_link_libraries(test_memory_recorderendpoint
                      ${GSTREAMER_LIBRARIES}
                      ${GSTREAMER_CHECK_LIBRARIES})

add_test_program (test_memory_webrtcendpoint webrtcendpoint.c)
add_dependencies(test_memory_webrtcendpoint kmsplugin)
target_include_directories(test_memory_webrtcendpoint PRIVATE
                           ${GSTREAMER_INCLUDE_DIRS}
                           ${GSTREAMER_CHECK_INCLUDE_DIRS}
                           ${GSTREAMER_SDP_SOURCE_DIRS})
target_link_libraries(test_memory_webrtcendpoint
                      ${GSTREAMER_LIBRARIES}
                      ${GSTREAMER_CHECK_LIBRARIES}
                      ${GSTREAMER_SDP_LIBRARIES})
