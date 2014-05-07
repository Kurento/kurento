include (TestHelper)

set (TEST_PROPERTIES
  GST_PLUGIN_PATH=${CMAKE_BINARY_DIR}
  CK_DEFAULT_TIMEOUT=20
  )

add_test_program (dtls)
add_dependencies(dtls rtcpdemux kmsdtls)
target_include_directories(dtls PRIVATE ${GSTREAMER_INCLUDE_DIRS} ${GSTREAMER_CHECK_INCLUDE_DIRS} ${GIO_INCLUDE_DIRS})
target_link_libraries(dtls ${GSTREAMER_LIBRARIES} ${GSTREAMER_CHECK_LIBRARIES} ${GIO_LIBRARIES})

add_test_program (dtlssrtp)
add_dependencies(dtlssrtp rtcpdemux kmsdtls)
target_include_directories(dtlssrtp PRIVATE ${GSTREAMER_INCLUDE_DIRS} ${GSTREAMER_CHECK_INCLUDE_DIRS} ${GIO_INCLUDE_DIRS})
target_link_libraries(dtlssrtp ${GSTREAMER_LIBRARIES} ${GSTREAMER_CHECK_LIBRARIES} ${GIO_LIBRARIES})
