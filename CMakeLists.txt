include (TestHelper)

set (TEST_PROPERTIES
  GST_PLUGIN_PATH=${CMAKE_BINARY_DIR}
  CK_DEFAULT_TIMEOUT=20
  )

add_test_program (test_dtls dtls.c)
add_dependencies(test_dtls rtcpdemux kmsdtls)
target_include_directories(test_dtls PRIVATE ${GSTREAMER_INCLUDE_DIRS} ${GSTREAMER_CHECK_INCLUDE_DIRS} ${GIO_INCLUDE_DIRS})
target_link_libraries(test_dtls ${GSTREAMER_LIBRARIES} ${GSTREAMER_CHECK_LIBRARIES} ${GIO_LIBRARIES})

add_test_program (test_dtlssrtp dtlssrtp.c)
add_dependencies(test_dtlssrtp rtcpdemux kmsdtls)
target_include_directories(test_dtlssrtp PRIVATE ${GSTREAMER_INCLUDE_DIRS} ${GSTREAMER_CHECK_INCLUDE_DIRS} ${GIO_INCLUDE_DIRS})
target_link_libraries(test_dtlssrtp ${GSTREAMER_LIBRARIES} ${GSTREAMER_CHECK_LIBRARIES} ${GIO_LIBRARIES})
