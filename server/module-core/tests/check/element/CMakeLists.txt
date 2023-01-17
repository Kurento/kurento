set(SUPRESSIONS "${CMAKE_CURRENT_SOURCE_DIR}/../valgrind.supp")

if(${ENABLE_EXPERIMENTAL_TESTS})
  set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DENABLE_EXPERIMENTAL_TESTS")
endif()

# all test programs
set(ALL_TESTS
  hubport
  filterelement
  agnosticbin
  agnosticbin_negotiation
  agnosticbin3
  audiomixerbin
  #audiomixer
  bufferinjector
  pad_connections
  passthrough
)

# tests targets
foreach(test ${ALL_TESTS})
  add_test_program(test_${test} ${test}.c)

  add_dependencies(test_${test} ${LIBRARY_NAME})

  target_include_directories(test_${test} PRIVATE
    ${gstreamer-1.0_INCLUDE_DIRS}
    ${gstreamer-video-1.0_INCLUDE_DIRS}
    ${gstreamer-check-1.0_INCLUDE_DIRS}
    ${CMAKE_CURRENT_BINARY_DIR}/../../../
  )

  target_link_libraries(test_${test}
    ${gstreamer-1.0_LIBRARIES}
    ${gstreamer-video-1.0_LIBRARIES}
    ${gstreamer-check-1.0_LIBRARIES}
  )

endforeach(test)

#SDP Tests
add_test_program(test_sdp_agent sdp_agent.c)
target_include_directories(test_sdp_agent PRIVATE
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${gstreamer-check-1.0_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins/commons/
  ${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins/commons/sdpagent
  ${CMAKE_CURRENT_BINARY_DIR}/../../../
)

target_link_libraries(test_sdp_agent
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-check-1.0_LIBRARIES}
  kmsgstcommons
  kmssdpagent
  sdputils
)

# metadata
add_test_program(test_metadata metadata.c)
target_include_directories(test_metadata PRIVATE
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${gstreamer-check-1.0_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins/commons/
  ${CMAKE_CURRENT_BINARY_DIR}/../../../
)

target_link_libraries(test_metadata
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-check-1.0_LIBRARIES}
  kmsgstcommons
)

#lists
add_test_program(test_lists lists.c)
target_include_directories(test_lists PRIVATE
  ${gstreamer-1.0_INCLUDE_DIRS}
  ${gstreamer-check-1.0_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}/../../../src/gst-plugins/commons/
)

target_link_libraries(test_lists
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-check-1.0_LIBRARIES}
  kmsgstcommons
)

add_custom_target(clear_directory
  COMMAND ${CMAKE_COMMAND} -E remove_directory ${GST_DEBUG_DUMP_DOT_DIR}
  COMMAND ${CMAKE_COMMAND} -E make_directory ${GST_DEBUG_DUMP_DOT_DIR}
)
add_dependencies(check clear_directory)
