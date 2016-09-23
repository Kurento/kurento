cmake_minimum_required(VERSION 2.8)

project ("kms-omni-build")

enable_testing()

add_subdirectory(kms-cmake-utils)

list (INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/kms-cmake-utils/CMake")
list (INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-cmake-utils/")

add_subdirectory(kms-jsonrpc)

list (INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-jsonrpc/")
list (INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/kms-jsonrpc")
set (JSON_CPP_BINARY_DIR_PREFIX 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-jsonrpc")

execute_process(COMMAND mvn package -DskipTests -DbuildDirectory=${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/kurento-module-creator)
execute_process(COMMAND cp ${CMAKE_CURRENT_SOURCE_DIR}/kurento-module-creator/scripts/kurento-module-creator ${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator)

list (INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator")

list (INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator/classes")

add_subdirectory(kms-core)
