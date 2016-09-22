cmake_minimum_required(VERSION 2.8)

enable_testing()

add_subdirectory(kms-cmake-utils)

list (INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/kms-cmake-utils/CMake")
list (INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-cmake-utils/")

add_subdirectory(kms-jsonrpc)

list (INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-jsonrpc/")
list (INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/kms-jsonrpc")
set (JSON_CPP_BINARY_DIR_PREFIX 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-jsonrpc")

execute_process(COMMAND mvn package -DskipTests WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/kurento-module-creator)

list (INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/kurento-module-creator")

list (INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/kurento-module-creator/target/classes")

add_subdirectory(kms-core)
