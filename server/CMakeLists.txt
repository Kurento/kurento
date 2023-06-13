cmake_minimum_required(VERSION 3.0)

project("kurento")
message(STATUS "CMake project: ${PROJECT_NAME}")

# Test configuration
set(GENERATE_TESTS FALSE CACHE BOOL "Always build tests: add `make check_build` to normal `make` calls")
set(DISABLE_TESTS FALSE CACHE BOOL "Enable running `make check` during the building process")
set(DISABLE_IPV6_TESTS FALSE CACHE BOOL "Disable tests that require an IPv6 network")
set(TEST_FILES_LOCATION "https://raw.githubusercontent.com/Kurento/test-files/main" CACHE STRING "Root URI with test files (e.g. http:// or file://)")
set(VALGRIND_NUM_CALLERS 20 CACHE STRING "Valgrind option: maximum number of entries shown in stack traces")
enable_testing()

# Create the SUPPRESSIONS list with all found files
file(GLOB SUPPRESSIONS
  LIST_DIRECTORIES false
  "${CMAKE_CURRENT_SOURCE_DIR}/bin/valgrind/*.supp")



# kurento-cmake-utils
# ===================

add_subdirectory(cmake-utils)

# Make an initial call to `create_check_target()`, so the test targets belong to
# the global Makefile and not to the first subproject that calls it.
include(TestHelpers)
create_check_target()



# kurento-module-creator
# ======================

include(GenericFind)

# DEVELOPER NOTE:
# To use a Kurento Module Creator that you have already installed in the system,
# uncomment this line:
#generic_find(LIBNAME KurentoModuleCreator VERSION ^7.0.1)

if(NOT KurentoModuleCreator_FOUND)
  message(STATUS "Building kurento-module-creator")
  execute_process(COMMAND mvn package -DskipTests
      -DbuildDirectory=${CMAKE_CURRENT_BINARY_DIR}/module-creator
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/module-creator
    RESULT_VARIABLE MAVEN_RET)
  if(MAVEN_RET)
    message("Command result: ${MAVEN_RET}")
    message(FATAL_ERROR "Command failed: mvn package")
  endif()

  execute_process(COMMAND cp
      ${CMAKE_CURRENT_SOURCE_DIR}/module-creator/scripts/kurento-module-creator
      ${CMAKE_CURRENT_BINARY_DIR}/module-creator/kurento-module-creator)

  execute_process(COMMAND touch
      ${CMAKE_CURRENT_BINARY_DIR}/module-creator/kurento-module-creator.generated)
endif()

file(GLOB_RECURSE MODULE_CREATOR_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/module-creator/src/*)
list(APPEND MODULE_CREATOR_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/module-creator/pom.xml)

add_custom_command(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/module-creator/kurento-module-creator.generated
  COMMAND touch ${CMAKE_CURRENT_BINARY_DIR}/module-creator/kurento-module-creator.generated
  COMMAND ${CMAKE_COMMAND} ${CMAKE_SOURCE_DIR}
  DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/module-creator/kurento-module-creator-jar-with-dependencies.jar
  WORKING_DIRECTORY ${CMAKE_BINARY_DIR}
)

add_custom_command(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/module-creator/kurento-module-creator-jar-with-dependencies.jar
  COMMAND mvn package -DskipTests -DbuildDirectory=${CMAKE_CURRENT_BINARY_DIR}/module-creator
  DEPENDS ${MODULE_CREATOR_SOURCES}
  WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/module-creator
  )

add_custom_command(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/module-creator/kurento-module-creator
  DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/module-creator/kurento-module-creator.generated ${CMAKE_CURRENT_SOURCE_DIR}/module-creator/scripts/kurento-module-creator
  COMMAND cp ${CMAKE_CURRENT_SOURCE_DIR}/module-creator/scripts/kurento-module-creator ${CMAKE_CURRENT_BINARY_DIR}/module-creator/kurento-module-creator
  )

add_custom_target(kurento-module-creator
  DEPENDS ${CMAKE_CURRENT_BINARY_DIR}/module-creator/kurento-module-creator
)

# Add CMake modules provided by kurento-module-creator.
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/module-creator/classes/Kurento")
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/module-creator/classes/KurentoModuleCreator") # find_package() search path for `KurentoModuleCreatorConfig.cmake`.
list(INSERT CMAKE_PROGRAM_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/module-creator") # find_program() search path for `kurento-module-creator` binary (launcher script).

find_package(Sanitizers MODULE)
if(DEFINED SANITIZE_LINK_STATIC)
  set(SANITIZERS_ENABLED ON)
endif()

if(SANITIZERS_ENABLED)
  sanitizer_add_blacklist_file("bin/sanitizers/blacklist.txt")
endif()



# kurento-jsonrpc
# ===============

add_subdirectory(jsonrpc)

# Add CMake settings provided by kurento-jsonrpc.
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/jsonrpc")
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/jsonrpc")

set(JSON_CPP_BINARY_DIR_PREFIX "${CMAKE_CURRENT_BINARY_DIR}/jsonrpc" CACHE PATH "Path prefix used to look for binary files" FORCE)



# kurento-module-core
# ===================

add_subdirectory(module-core)

# Add CMake settings provided by kurento-module-core.
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/module-core")
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/module-core")

set(GST_COMMONS_BINARY_DIR_PREFIX "${CMAKE_CURRENT_BINARY_DIR}/module-core" CACHE PATH "Path prefix used to look for binary files" FORCE)

set(KMSCORE_BINARY_DIR_PREFIX "${CMAKE_CURRENT_BINARY_DIR}" CACHE PATH "Path prefix used to look for binary files" FORCE)
set(KMSCORE_SOURCE_DIR_PREFIX "${CMAKE_CURRENT_SOURCE_DIR}" CACHE PATH "Path prefix used to look for source files" FORCE)

set(KURENTO_MODULES_DIR ${CMAKE_CURRENT_BINARY_DIR}/module-core/src/server/kmd CACHE PATH "Path to Kurento Module Descriptor files" FORCE)



# kurento-module-elements
# =======================

add_subdirectory(module-elements)

# Add CMake settings provided by kurento-module-elements.
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/module-elements")
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/module-elements")

set(KMSELEMENTS_BINARY_DIR_PREFIX "${CMAKE_CURRENT_BINARY_DIR}" CACHE PATH "Path prefix used to look for binary files" FORCE)
set(KMSELEMENTS_SOURCE_DIR_PREFIX "${CMAKE_CURRENT_SOURCE_DIR}" CACHE PATH "Path prefix used to look for source files" FORCE)

set(KURENTO_MODULES_DIR "${KURENTO_MODULES_DIR};${CMAKE_CURRENT_BINARY_DIR}/module-elements/src/server/kmd" CACHE PATH "Path to Kurento Module Descriptor files" FORCE)

set(KMS_RTP_ENDPOINT_BINARY_DIR_PREFIX "${CMAKE_CURRENT_BINARY_DIR}/module-elements" CACHE PATH "Path prefix used to look for binary files" FORCE)
set(KMS_WEBRTC_ENDPOINT_BINARY_DIR_PREFIX "${CMAKE_CURRENT_BINARY_DIR}/module-elements" CACHE PATH "Path prefix used to look for binary files" FORCE)



# kurento-module-filters
# ======================

add_subdirectory(module-filters)

# Add CMake settings provided by kurento-module-filters.
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/module-filters")
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/module-filters")

set(KMSFILTERS_BINARY_DIR_PREFIX "${CMAKE_CURRENT_BINARY_DIR}" CACHE PATH "Path prefix used to look for binary files" FORCE)
set(KMSFILTERS_SOURCE_DIR_PREFIX "${CMAKE_CURRENT_SOURCE_DIR}" CACHE PATH "Path prefix used to look for source files" FORCE)

set(KURENTO_MODULES_DIR "${KURENTO_MODULES_DIR};${CMAKE_CURRENT_BINARY_DIR}/module-filters/src/server/kmd" CACHE PATH "Path to Kurento Module Descriptor files" FORCE)



# kurento-media-server
# ====================

add_subdirectory(media-server)

add_custom_target(kurento-media-server-run
  COMMAND ${CMAKE_COMMAND} -E env GST_DEBUG=Kurento*:5 env KURENTO_MODULES_PATH=${CMAKE_CURRENT_BINARY_DIR} env GST_PLUGIN_PATH=${CMAKE_CURRENT_BINARY_DIR} ${CMAKE_CURRENT_BINARY_DIR}/media-server/server/kurento-media-server --modules-config-path=${CMAKE_BINARY_DIR}/config --conf-file=${CMAKE_SOURCE_DIR}/media-server/kurento.conf.json
  DEPENDS kurento-media-server
)



# kurento-module-gstreamer-example
# ================================

add_subdirectory(module-examples/gstreamer-example)

set(KURENTO_MODULES_DIR "${KURENTO_MODULES_DIR};${CMAKE_CURRENT_BINARY_DIR}/module-examples/gstreamer-example/src/server/kmd" CACHE PATH "Path to Kurento Module Descriptor files" FORCE)



# kurento-module-opencv-example
# =============================

add_subdirectory(module-examples/opencv-example)

set(KURENTO_MODULES_DIR "${KURENTO_MODULES_DIR};${CMAKE_CURRENT_BINARY_DIR}/module-examples/opencv-example/src/server/kmd" CACHE PATH "Path to Kurento Module Descriptor files" FORCE)



# kurento-module-chroma
# =====================

add_subdirectory(module-examples/chroma)

set(KURENTO_MODULES_DIR "${KURENTO_MODULES_DIR};${CMAKE_CURRENT_BINARY_DIR}/module-examples/chroma/src/server/kmd" CACHE PATH "Path to Kurento Module Descriptor files" FORCE)



# kurento-module-datachannelexample
# =================================

add_subdirectory(module-examples/datachannelexample)

set(KURENTO_MODULES_DIR "${KURENTO_MODULES_DIR};${CMAKE_CURRENT_BINARY_DIR}/module-examples/datachannelexample/src/server/kmd" CACHE PATH "Path to Kurento Module Descriptor files" FORCE)
