set(TEST_VARIABLES
  "GST_PLUGIN_PATH=$ENV{GST_PLUGIN_PATH}:${CMAKE_BINARY_DIR}:${CMAKE_BINARY_DIR}/src"
  "KURENTO_MODULES_PATH=${CMAKE_BINARY_DIR}:${CMAKE_BINARY_DIR}/src:$ENV{KURENTO_MODULES_PATH}"
)
set(VALGRIND_TEST_VARIABLES
  "${TEST_VARIABLES}"
  "DEBUG_MEDIASET=TRUE"
)
list(APPEND SUPPRESSIONS
  "${CMAKE_CURRENT_SOURCE_DIR}/valgrind.supp")

if (Boost_FOUND)
    include_directories(${Boost_INCLUDE_DIRS})

    message("Boost VERSION: ${Boost_VERSION}")
    message("Boost INCLUDE_DIRS: ${Boost_INCLUDE_DIRS}")
    message("Boost Boost_LIBRARY_DIRS: ${Boost_LIBRARY_DIRS}")
    message("Boost LIBRARIES: ${Boost_SYSTEM_LIBRARY}")
endif ()

add_test_program(test_videosampler videosampler.cpp )
set_property(TARGET test_videosampler
  PROPERTY INCLUDE_DIRECTORIES
    ${KmsJsonRpc_INCLUDE_DIRS}
    ${sigc++-2.0_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation/objects
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/implementation
    ${CMAKE_CURRENT_SOURCE_DIR}/../../src/server/interface
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/interface/generated-cpp
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/implementation/generated-cpp
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server/proto-src
    ${CMAKE_CURRENT_BINARY_DIR}/../../src/server
    ${KMSCORE_INCLUDE_DIRS}
    ${KMSELEMENTS_INCLUDE_DIRS}
    ${gstreamer-1.0_INCLUDE_DIRS}
)
target_link_libraries(test_videosampler
  ${LIBRARY_NAME}impl
  ${KMSCORE_LIBRARIES}
  ${KMSELEMENTS_LIBRARIES}
  ${Boost_FILESYSTEM_LIBRARY}
  ${Boost_SYSTEM_LIBRARY}
)


