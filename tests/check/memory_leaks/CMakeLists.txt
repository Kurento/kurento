add_test_program (test_memory_playerendpoint playerendpoint.c)
add_dependencies(test_memory_playerendpoint kmsplugin)
target_include_directories(test_memory_playerendpoint PRIVATE
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src")
target_link_libraries(test_memory_playerendpoint
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES})

add_test_program (test_memory_recorderendpoint recorderendpoint.c)
add_dependencies(test_memory_recorderendpoint kmsplugin)
target_include_directories(test_memory_recorderendpoint PRIVATE
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src")
target_link_libraries(test_memory_recorderendpoint
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES})

add_test_program (test_memory_webrtcendpoint webrtcendpoint.c)
add_dependencies(test_memory_webrtcendpoint kmsplugin)
target_include_directories(test_memory_webrtcendpoint PRIVATE
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS}
                           ${gstreamer-1.5_SDP_SOURCE_DIRS})
target_link_libraries(test_memory_webrtcendpoint
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES}
                      ${gstreamer-1.5_SDP_LIBRARIES})
