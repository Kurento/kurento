set(SUPRESSIONS "${CMAKE_CURRENT_SOURCE_DIR}/../valgrind.supp")

if(${ENABLE_EXPERIMENTAL_TESTS})
  set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DENABLE_EXPERIMENTAL_TESTS")
endif()

add_test_program(test_httpendpoint httpendpoint.c)
add_dependencies(test_httpendpoint ${LIBRARY_NAME}plugins)
target_include_directories(test_httpendpoint PRIVATE
                           ${CMAKE_CURRENT_BINARY_DIR}/../../..
                           ${KmsGstCommons_INCLUDE_DIRS}
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
target_link_libraries(test_httpendpoint
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES}
                      ${KmsGstCommons_LIBRARIES})

add_test_program(test_recorderendpoint recorderendpoint.c)
add_dependencies(test_recorderendpoint ${LIBRARY_NAME}plugins)
target_include_directories(test_recorderendpoint PRIVATE
                           ${KmsGstCommons_INCLUDE_DIRS}
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS}
                           "${PROJECT_SOURCE_DIR}/3rdparty/valgrind/include")
target_link_libraries(test_recorderendpoint
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES}
                      ${KmsGstCommons_LIBRARIES})

add_test_program(test_playerendpoint playerendpoint.c)
add_dependencies(test_playerendpoint ${LIBRARY_NAME}plugins)
target_include_directories(test_playerendpoint PRIVATE
                           ${CMAKE_CURRENT_BINARY_DIR}/../../..
                           ${CMAKE_CURRENT_SOURCE_DIR}/..
                           ${KmsGstCommons_INCLUDE_DIRS}
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
target_link_libraries(test_playerendpoint
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES}
                      ${KmsGstCommons_LIBRARIES}
                      kmstestutils)

add_test_program(test_rtpendpoint rtpendpoint.c)
add_dependencies(test_rtpendpoint ${LIBRARY_NAME}plugins)
target_include_directories(test_rtpendpoint PRIVATE
                           ${CMAKE_CURRENT_SOURCE_DIR}/..
                           ${KmsGstCommons_INCLUDE_DIRS}
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS})
target_link_libraries(test_rtpendpoint
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-sdp-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES}
                      ${KmsGstCommons_LIBRARIES})

add_test_program(test_rtpendpoint_audio rtpendpoint_audio.c)
add_dependencies(test_rtpendpoint_audio ${LIBRARY_NAME}plugins)
target_include_directories(test_rtpendpoint_audio PRIVATE
                           ${CMAKE_CURRENT_SOURCE_DIR}/..
                           ${KmsGstCommons_INCLUDE_DIRS}
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS})
target_link_libraries(test_rtpendpoint_audio
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-sdp-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES}
                      ${KmsGstCommons_LIBRARIES})

# add_test_program(test_rtpendpoint_video rtpendpoint_video.c)
# add_dependencies(test_rtpendpoint_video kmstestutils ${LIBRARY_NAME}plugins)
# target_include_directories(test_rtpendpoint_video PRIVATE
#                            ${KmsGstCommons_INCLUDE_DIRS}
#                            ${gstreamer-1.5_INCLUDE_DIRS}
#                            ${gstreamer-check-1.5_INCLUDE_DIRS})
# target_link_libraries(test_rtpendpoint_video
#                       ${gstreamer-1.5_LIBRARIES}
#                       ${gstreamer-check-1.5_LIBRARIES}
#                       ${KmsGstCommons_LIBRARIES}
#                       kmstestutils)

add_test_program(test_webrtcendpoint webrtcendpoint.c)
add_dependencies(test_webrtcendpoint ${LIBRARY_NAME}plugins)
target_include_directories(test_webrtcendpoint PRIVATE
                           ${CMAKE_CURRENT_BINARY_DIR}/../../..
                           ${KmsGstCommons_INCLUDE_DIRS}
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS}
                           ${nice_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
target_link_libraries(test_webrtcendpoint
                      kmswebrtcendpointlib
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-sdp-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES}
                      ${nice_LIBRARIES}
                      ${KmsGstCommons_LIBRARIES})

#add_test_program(test_compositemixer compositemixer.c)
#add_dependencies(test_compositemixer kmstestutils)
#target_include_directories(test_compositemixer PRIVATE
#                           ${KmsGstCommons_INCLUDE_DIRS}
#                           ${gstreamer-1.5_INCLUDE_DIRS}
#                           ${gstreamer-check-1.5_INCLUDE_DIRS})
#target_link_libraries(test_compositemixer
#                      ${gstreamer-1.5_LIBRARIES}
#                      ${gstreamer-check-1.5_LIBRARIES}
#                      ${KmsGstCommons_LIBRARIES})

add_test_program(test_dispatcheronetomany dispatcheronetomany.c)
target_include_directories(test_dispatcheronetomany PRIVATE
                           ${KmsGstCommons_INCLUDE_DIRS}
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS})
target_link_libraries(test_dispatcheronetomany
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES}
                      ${KmsGstCommons_LIBRARIES})

if(${ENABLE_EXPERIMENTAL_TESTS})
  add_test_program(test_dispatcher dispatcher.c)
  target_include_directories(test_dispatcher PRIVATE
                            ${KmsGstCommons_INCLUDE_DIRS}
                            ${gstreamer-1.5_INCLUDE_DIRS}
                            ${gstreamer-check-1.5_INCLUDE_DIRS})
  target_link_libraries(test_dispatcher
                        ${gstreamer-1.5_LIBRARIES}
                        ${gstreamer-check-1.5_LIBRARIES}
                        ${KmsGstCommons_LIBRARIES})
endif()

#add_test_program(test_alphablending alphablending.c)
#add_dependencies(test_alphablending kmstestutils)
#target_include_directories(test_alphablending PRIVATE
#                           ${KmsGstCommons_INCLUDE_DIRS}
#                           ${gstreamer-1.5_INCLUDE_DIRS}
#                           ${gstreamer-check-1.5_INCLUDE_DIRS})
#target_link_libraries(test_alphablending
#                      ${gstreamer-1.5_LIBRARIES}
#                      ${gstreamer-check-1.5_LIBRARIES}
#                      ${KmsGstCommons_LIBRARIES})

#add_test_program(test_dtls dtls.c)
#add_dependencies(test_dtls rtcpdemux)
#target_include_directories(test_dtls PRIVATE
#                          ${gstreamer-1.5_INCLUDE_DIRS}
#                          ${gstreamer-check-1.5_INCLUDE_DIRS}
#                          ${gio-2.0_INCLUDE_DIRS})
#target_link_libraries(test_dtls
#                      ${gstreamer-1.5_LIBRARIES}
#                      ${gstreamer-check-1.5_LIBRARIES}
#                      ${gio-2.0_LIBRARIES})

if(${ENABLE_EXPERIMENTAL_TESTS})
  add_test_program(test_dtlssrtp dtlssrtp.c)
  add_dependencies(test_dtlssrtp rtcpdemux)
  target_include_directories(test_dtlssrtp PRIVATE
                              ${gstreamer-1.5_INCLUDE_DIRS}
                              ${gstreamer-check-1.5_INCLUDE_DIRS}
                              ${gio-2.0_INCLUDE_DIRS})
  target_link_libraries(test_dtlssrtp ${gstreamer-1.5_LIBRARIES}
                                      ${gstreamer-check-1.5_LIBRARIES}
                                      ${gio-2.0_LIBRARIES})
endif()

add_test_program(test_webrtc_data_proto webrtc_data_proto.c)
add_dependencies(test_webrtc_data_proto ${LIBRARY_NAME}plugins)
target_include_directories(test_webrtc_data_proto PRIVATE
                           ${CMAKE_CURRENT_BINARY_DIR}/../../..
                           ${KmsGstCommons_INCLUDE_DIRS}
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS}
                           ${nice_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
target_link_libraries(test_webrtc_data_proto
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES}
                      ${nice_LIBRARIES}
                      ${KmsGstCommons_LIBRARIES}
                      webrtcdataproto)

add_test_program(test_srtp srtp.c)
add_dependencies(test_srtp ${LIBRARY_NAME}plugins)
target_include_directories(test_srtp PRIVATE
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-rtp-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS}
                           ${KmsGstCommons_INCLUDE_DIRS})
target_link_libraries(test_srtp
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-rtp-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES}
                      ${KmsGstCommons_LIBRARIES})

add_test_program(test_ice_candidates ice_candidates.c)
add_dependencies(test_ice_candidates ${LIBRARY_NAME}plugins)
target_include_directories(test_ice_candidates PRIVATE
                           ${KmsGstCommons_INCLUDE_DIRS}
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS}
                           ${nice_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
target_link_libraries(test_ice_candidates
                      kmswebrtcendpointlib
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-sdp-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES}
                      ${nice_LIBRARIES}
                      ${KmsGstCommons_LIBRARIES})
