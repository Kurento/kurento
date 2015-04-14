add_test_program (test_integration_playerendpoint playerendpoint.c)
add_dependencies(test_integration_playerendpoint kmstestutils kmsplugin)
target_include_directories(test_integration_playerendpoint PRIVATE
                           ${gstreamer-1.5_INCLUDE_DIRS}
                           ${gstreamer-check-1.5_INCLUDE_DIRS}
                           "${CMAKE_CURRENT_SOURCE_DIR}/../../../src")
target_link_libraries(test_integration_playerendpoint
                      ${gstreamer-1.5_LIBRARIES}
                      ${gstreamer-check-1.5_LIBRARIES}
                      kmstestutils)
