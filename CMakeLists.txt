cmake_minimum_required(VERSION 2.8)

project("kms-omni-build")
message(STATUS "CMake project: ${PROJECT_NAME}")

# Disable Policy CMP0058, which takes care of Ninja complaining in QtCreator
# (generate_kurento_libraries() creates files that aren't part of any target)
#cmake_policy(SET CMP0058 OLD)

# Test configuration
set(GENERATE_TESTS FALSE CACHE BOOL "Always build tests: add `make check_build` to normal `make` calls")
set(DISABLE_TESTS FALSE CACHE BOOL "Enable running `make check` during the building process")
set(VALGRIND_NUM_CALLERS 20 CACHE STRING "Valgrind option: maximum number of entries shown in stack traces")

enable_testing()

# Create the SUPPRESSIONS list with all found files
file(GLOB SUPPRESSIONS
  LIST_DIRECTORIES false
  "${CMAKE_CURRENT_SOURCE_DIR}/bin/valgrind/*.supp")

add_subdirectory(kms-cmake-utils)

list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-cmake-utils")
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/kms-cmake-utils/CMake")
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/kms-cmake-utils/3rdparty/sanitizers-cmake/cmake")

add_subdirectory(kms-jsonrpc)

list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-jsonrpc/")
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/kms-jsonrpc")
set(JSON_CPP_BINARY_DIR_PREFIX "${CMAKE_CURRENT_BINARY_DIR}/kms-jsonrpc" CACHE PATH "Path prefix used to look for binary files" FORCE)

include(GenericFind)

generic_find(LIBNAME KurentoModuleCreator VERSION ^6.7.0)

if(NOT KurentoModuleCreator_FOUND)
  message(STATUS "Generating module creator")
  execute_process(COMMAND mvn package -DskipTests
      -DbuildDirectory=${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/kurento-module-creator
    RESULT_VARIABLE MAVEN_RET)
  if(MAVEN_RET)
    message("Command result: ${MAVEN_RET}")
    message(FATAL_ERROR "Command failed: mvn package")
  endif()

  execute_process(COMMAND cp
      ${CMAKE_CURRENT_SOURCE_DIR}/kurento-module-creator/scripts/kurento-module-creator
      ${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator/kurento-module-creator)

  execute_process(COMMAND touch
      ${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator/kurento-module-creator.generated)
endif()

file(GLOB_RECURSE MODULE_CREATOR_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/kurento-module-creator/src/*)
list(APPEND MODULE_CREATOR_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/kurento-module-creator/pom.xml)

add_custom_command(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator/kurento-module-creator.generated
  COMMAND touch ${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator/kurento-module-creator.generated
  COMMAND ${CMAKE_COMMAND} ${CMAKE_SOURCE_DIR}
  DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator/kurento-module-creator-jar-with-dependencies.jar
  WORKING_DIRECTORY ${CMAKE_BINARY_DIR}
)

add_custom_command(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator/kurento-module-creator-jar-with-dependencies.jar
  COMMAND mvn package -DskipTests -DbuildDirectory=${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator
  DEPENDS ${MODULE_CREATOR_SOURCES}
  WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/kurento-module-creator
  )

add_custom_command(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator/kurento-module-creator
  DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator/kurento-module-creator.generated ${CMAKE_CURRENT_SOURCE_DIR}/kurento-module-creator/scripts/kurento-module-creator
  COMMAND cp ${CMAKE_CURRENT_SOURCE_DIR}/kurento-module-creator/scripts/kurento-module-creator ${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator/kurento-module-creator
  )

add_custom_target(kurento-module-creator
  DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator/kurento-module-creator
)

list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator")
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator/classes")

find_package(Sanitizers MODULE)
if(DEFINED SANITIZE_LINK_STATIC)
  set(SANITIZERS_ENABLED ON)
endif()

if(SANITIZERS_ENABLED)
  sanitizer_add_blacklist_file("bin/sanitizers/blacklist.txt")
endif()

add_subdirectory(kms-core)

list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}")
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-core/src/server")
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/kms-core/CMake")

list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-core")
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/kms-core")
set(GST_COMMONS_BINARY_DIR_PREFIX "${CMAKE_CURRENT_BINARY_DIR}/kms-core" CACHE PATH "Path prefix used to look for binary files" FORCE)
set(KMSCORE_BINARY_DIR_PREFIX "${CMAKE_CURRENT_BINARY_DIR}" CACHE PATH "Path prefix used to look for binary files" FORCE)
set(KMSCORE_SOURCE_DIR_PREFIX "${CMAKE_CURRENT_SOURCE_DIR}" CACHE PATH "Path prefix used to look for source files" FORCE)

set(KURENTO_MODULES_DIR ${CMAKE_CURRENT_BINARY_DIR}/kms-core/src/server/kmd CACHE PATH "Directory where kurento modules descriptors can be found" FORCE)

add_subdirectory(kms-elements)

list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-elements/src/server")

list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-elements")
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/kms-elements")
set(KMSELEMENTS_BINARY_DIR_PREFIX "${CMAKE_CURRENT_BINARY_DIR}" CACHE PATH "Path prefix used to look for binary files" FORCE)
set(KMSELEMENTS_SOURCE_DIR_PREFIX "${CMAKE_CURRENT_SOURCE_DIR}" CACHE PATH "Path prefix used to look for source files" FORCE)

set(KURENTO_MODULES_DIR "${KURENTO_MODULES_DIR};${CMAKE_CURRENT_BINARY_DIR}/kms-elements/src/server/kmd" CACHE PATH "Directory where kurento modules descriptors can be found" FORCE)
set(KMS_WEBRTC_ENDPOINT_BINARY_DIR_PREFIX "${CMAKE_CURRENT_BINARY_DIR}/kms-elements" CACHE PATH "Path prefix used to look for binary files" FORCE)

add_subdirectory(kms-filters)

list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-filters/src/server")

list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/kms-filters")
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/kms-filters")
set(KMSFILTERS_BINARY_DIR_PREFIX "${CMAKE_CURRENT_BINARY_DIR}" CACHE PATH "Path prefix used to look for binary files" FORCE)
set(KMSFILTERS_SOURCE_DIR_PREFIX "${CMAKE_CURRENT_SOURCE_DIR}" CACHE PATH "Path prefix used to look for source files" FORCE)

set(KURENTO_MODULES_DIR "${KURENTO_MODULES_DIR};${CMAKE_CURRENT_BINARY_DIR}/kms-filters/src/server/kmd" CACHE PATH "Directory where kurento modules descriptors can be found" FORCE)

add_subdirectory(kurento-media-server)

add_custom_target(kurento-media-server-run
  COMMAND ${CMAKE_COMMAND} -E env GST_DEBUG=Kurento*:5 env KURENTO_MODULES_PATH=${CMAKE_CURRENT_BINARY_DIR} env GST_PLUGIN_PATH=${CMAKE_CURRENT_BINARY_DIR} ${CMAKE_CURRENT_BINARY_DIR}/kurento-media-server/server/kurento-media-server --modules-config-path=${CMAKE_BINARY_DIR}/config --conf-file=${CMAKE_SOURCE_DIR}/kurento-media-server/kurento.conf.json
  DEPENDS kurento-media-server
  )
