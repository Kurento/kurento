set (SUPRESSIONS "${CMAKE_CURRENT_SOURCE_DIR}/../valgrind.supp")

add_test_program (test_httpendpoint httpendpoint.c)
add_dependencies(test_httpendpoint kmscore ${LIBRARY_NAME}plugins)
target_include_directories(test_httpendpoint PRIVATE
                           ${KmsGstCommons_INCLUDE_DIRS}
                           ${gstreamer-1.0_INCLUDE_DIRS}
                           ${gstreamer-check-1.0_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
target_link_libraries(test_httpendpoint
                      ${gstreamer-1.0_LIBRARIES}
                      ${gstreamer-check-1.0_LIBRARIES}
                      ${KmsGstCommons_LIBRARIES})

add_test_program (test_recorderendpoint recorderendpoint.c)
add_dependencies(test_recorderendpoint kmscore ${LIBRARY_NAME}plugins)
target_include_directories(test_recorderendpoint PRIVATE
                           ${KmsGstCommons_INCLUDE_DIRS}
                           ${gstreamer-1.0_INCLUDE_DIRS}
                           ${gstreamer-check-1.0_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
target_link_libraries(test_recorderendpoint
                      ${gstreamer-1.0_LIBRARIES}
                      ${gstreamer-check-1.0_LIBRARIES}
                      ${KmsGstCommons_LIBRARIES})

#add_test_program (test_playerendpoint playerendpoint.c)
#add_dependencies(test_playerendpoint kmstestutils kmscore ${LIBRARY_NAME}plugins)
#target_include_directories(test_playerendpoint PRIVATE
#                           ${KmsGstCommons_INCLUDE_DIRS}
#                           ${gstreamer-1.0_INCLUDE_DIRS}
#                           ${gstreamer-check-1.0_INCLUDE_DIRS}
#                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins")
#target_link_libraries(test_playerendpoint
#                      ${gstreamer-1.0_LIBRARIES}
#                      ${gstreamer-check-1.0_LIBRARIES}
#                      ${KmsGstCommons_LIBRARIES}
#                      kmstestutils)

# add_test_program (test_rtpendpoint rtpendpoint.c)
# add_dependencies(test_rtpendpoint kmstestutils kmscore ${LIBRARY_NAME}plugins)
# target_include_directories(test_rtpendpoint PRIVATE
#                            ${KmsGstCommons_INCLUDE_DIRS}
#                            ${gstreamer-1.0_INCLUDE_DIRS}
#                            ${gstreamer-check-1.0_INCLUDE_DIRS})
# target_link_libraries(test_rtpendpoint
#                       ${gstreamer-1.0_LIBRARIES}
#                       ${gstreamer-check-1.0_LIBRARIES}
#                       ${KmsGstCommons_LIBRARIES}
#                       kmstestutils)
#
# add_test_program (test_rtpendpoint_audio rtpendpoint_audio.c)
# add_dependencies(test_rtpendpoint_audio kmstestutils kmscore ${LIBRARY_NAME}plugins)
# target_include_directories(test_rtpendpoint_audio PRIVATE
#                            ${KmsGstCommons_INCLUDE_DIRS}
#                            ${gstreamer-1.0_INCLUDE_DIRS}
#                            ${gstreamer-check-1.0_INCLUDE_DIRS})
# target_link_libraries(test_rtpendpoint_audio
#                       ${gstreamer-1.0_LIBRARIES}
#                       ${gstreamer-check-1.0_LIBRARIES}
#                       ${KmsGstCommons_LIBRARIES}
#                       kmstestutils)
#
# add_test_program (test_rtpendpoint_video rtpendpoint_video.c)
# add_dependencies(test_rtpendpoint_video kmstestutils kmscore ${LIBRARY_NAME}plugins)
# target_include_directories(test_rtpendpoint_video PRIVATE
#                            ${KmsGstCommons_INCLUDE_DIRS}
#                            ${gstreamer-1.0_INCLUDE_DIRS}
#                            ${gstreamer-check-1.0_INCLUDE_DIRS})
# target_link_libraries(test_rtpendpoint_video
#                       ${gstreamer-1.0_LIBRARIES}
#                       ${gstreamer-check-1.0_LIBRARIES}
#                       ${KmsGstCommons_LIBRARIES}
#                       kmstestutils)

#add_test_program (test_webrtcendpoint webrtcendpoint.c)
#add_dependencies(test_webrtcendpoint kmstestutils kmscore kmsdtls ${LIBRARY_NAME}plugins)
#target_include_directories(test_webrtcendpoint PRIVATE
#                           ${KmsGstCommons_INCLUDE_DIRS}
#                           ${gstreamer-1.0_INCLUDE_DIRS}
#                           ${gstreamer-check-1.0_INCLUDE_DIRS})
#target_link_libraries(test_webrtcendpoint
#                      ${gstreamer-1.0_LIBRARIES}
#                      ${gstreamer-check-1.0_LIBRARIES}
#                      ${KmsGstCommons_LIBRARIES}
#                      kmstestutils)

#add_test_program (test_compositemixer compositemixer.c)
#add_dependencies(test_compositemixer kmstestutils kmscore kmselements)
#target_include_directories(test_compositemixer PRIVATE
#                           ${KmsGstCommons_INCLUDE_DIRS}
#                           ${gstreamer-1.0_INCLUDE_DIRS}
#                           ${gstreamer-check-1.0_INCLUDE_DIRS})
#target_link_libraries(test_compositemixer
#                      ${gstreamer-1.0_LIBRARIES}
#                      ${gstreamer-check-1.0_LIBRARIES}
#                      ${KmsGstCommons_LIBRARIES})

#add_test_program (test_dispatcheronetomany dispatcheronetomany.c)
#add_dependencies(test_dispatcheronetomany kmstestutils kmscore kmselements)
#target_include_directories(test_dispatcheronetomany PRIVATE
#                           ${KmsGstCommons_INCLUDE_DIRS}
#                           ${gstreamer-1.0_INCLUDE_DIRS}
#                           ${gstreamer-check-1.0_INCLUDE_DIRS})
#target_link_libraries(test_dispatcheronetomany
#                      ${gstreamer-1.0_LIBRARIES}
#                      ${gstreamer-check-1.0_LIBRARIES}
#                      ${KmsGstCommons_LIBRARIES})

add_test_program (test_dtls dtls.c)
add_dependencies(test_dtls rtcpdemux kmsdtls)
target_include_directories(test_dtls PRIVATE
                          ${gstreamer-1.0_INCLUDE_DIRS}
                          ${gstreamer-check-1.0_INCLUDE_DIRS}
                          ${gio-2.0_INCLUDE_DIRS})
target_link_libraries(test_dtls
                      ${gstreamer-1.0_LIBRARIES}
                      ${gstreamer-check-1.0_LIBRARIES}
                      ${gio-2.0_LIBRARIES})

if (${ENABLE_DEBUGGING_TESTS})
  add_test_program (test_dtlssrtp dtlssrtp.c)
  add_dependencies(test_dtlssrtp rtcpdemux kmsdtls)
  target_include_directories(test_dtlssrtp PRIVATE
                              ${gstreamer-1.0_INCLUDE_DIRS}
                              ${gstreamer-check-1.0_INCLUDE_DIRS}
                              ${gio-2.0_INCLUDE_DIRS})
  target_link_libraries(test_dtlssrtp ${gstreamer-1.0_LIBRARIES}
                                      ${gstreamer-check-1.0_LIBRARIES}
                                      ${gio-2.0_LIBRARIES})
endif()
