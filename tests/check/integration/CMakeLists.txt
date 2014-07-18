add_test_program (test_integration_playerendpoint playerendpoint.c)
add_dependencies(test_integration_playerendpoint kmstestutils kmsplugin)
target_include_directories(test_integration_playerendpoint PRIVATE
                           ${GSTREAMER_INCLUDE_DIRS}
                           ${GSTREAMER_CHECK_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src")
target_link_libraries(test_integration_playerendpoint
                      ${GSTREAMER_LIBRARIES}
                      ${GSTREAMER_CHECK_LIBRARIES}
                      kmstestutils)
