cmake_minimum_required(VERSION 3.0)

project("kurento-module-filters")
message(STATUS "CMake project: ${PROJECT_NAME}")

include(GNUInstallDirs) # CMAKE_INSTALL_*

# Test configuration
set(GENERATE_TESTS FALSE CACHE BOOL "Always build tests: add `make check_build` to normal `make` calls")
set(DISABLE_TESTS FALSE CACHE BOOL "Enable running `make check` during the building process")
set(TEST_FILES_LOCATION "https://raw.githubusercontent.com/Kurento/test-files/main" CACHE STRING "Root URI with test files (e.g. http:// or file://)")
set(VALGRIND_NUM_CALLERS 20 CACHE STRING "Valgrind option: maximum number of entries shown in stack traces")
enable_testing()

# Add CMake modules provided by this project.
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/src/server")

# Export CMake search paths if this is not the top-level project.
get_directory_property(HAS_PARENT PARENT_DIRECTORY)
if(HAS_PARENT)
  set(CMAKE_MODULE_PATH "${CMAKE_MODULE_PATH}" PARENT_SCOPE)
endif()

find_package(KurentoUtils)
if(NOT KurentoUtils_FOUND)
  message(FATAL_ERROR "KurentoUtils not found; please install package 'kurento-cmake-utils'")
endif()

message("If CodeGenerator is not found, you need to install 'kurento-module-core' from the Kurento repository")
include(CodeGenerator)
get_values_from_model(PREFIX VALUE MODELS ${CMAKE_CURRENT_SOURCE_DIR}/src/server/interface KEYS version)

include(KurentoGitHelpers)
install_git_hook(pre-commit ${CMAKE_CURRENT_SOURCE_DIR}/hooks/pre-commit.hook)
get_git_version(PROJECT_VERSION ${VALUE_VERSION})
message(STATUS "Project version: ${PROJECT_NAME}-${PROJECT_VERSION}")

# Compiler flags
include(CommonBuildFlags)
common_buildflags_set()
#common_buildflags_print()

# Development: Add here exceptions to the "Warnings are Errors" rule.
# Also, DOCUMENT WHY and always remove them as soon as the problem is fixed.
# For example:
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-error=unused-function")
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-error=unused-variable")

# FIXME Disable error when macros __TIME__, __DATE__ or __TIMESTAMP__ are encountered
include(CheckCXXCompilerFlag)
CHECK_CXX_COMPILER_FLAG("-Wno-error=date-time" HAS_WARNING_DATE_TIME)
if(HAS_WARNING_DATE_TIME)
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-error=date-time")
endif()

# FIXME Disable error from deprecated function gst_element_get_request_pad(),
#       should use gst_element_request_pad_simple() instead.
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-error=deprecated-declarations")

# Generate file "config.h"
set(VERSION "${PROJECT_VERSION}")
set(PACKAGE "${PROJECT_NAME}")
set(GETTEXT_PACKAGE "${PROJECT_NAME}")
set(MANUAL_CHECK OFF CACHE BOOL "Tests will generate files")
set(KURENTO_MODULES_SO_DIR "${CMAKE_INSTALL_PREFIX}/${CMAKE_INSTALL_LIBDIR}/${KURENTO_MODULES_DIR_INSTALL_PREFIX}")
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/config.h)
set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   -DHAVE_CONFIG_H")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHAVE_CONFIG_H")

set(GST_REQUIRED ^1.0.0)
set(SOUP_REQUIRED ^2.40)
set(OPENCV_REQUIRED <5.0.0)

include(GenericFind)
generic_find(LIBNAME Boost REQUIRED COMPONENTS filesystem system thread unit_test_framework)
generic_find(LIBNAME gstreamer-1.0 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-base-1.0 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-video-1.0 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-check-1.0 VERSION ${GST_REQUIRED})
generic_find(LIBNAME KmsJsonRpc VERSION ^7.0.0-dev REQUIRED)
generic_find(LIBNAME libsoup-2.4 VERSION ${SOUP_REQUIRED} REQUIRED)
generic_find(LIBNAME OpenCV VERSION ${OPENCV_REQUIRED} REQUIRED)

set(CMAKE_INSTALL_GST_PLUGINS_DIR ${CMAKE_INSTALL_LIBDIR}/gstreamer-1.0)

add_subdirectory(src)
add_subdirectory(tests)
