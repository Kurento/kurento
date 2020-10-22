cmake_minimum_required(VERSION 2.8)

project("kms-filters")
message(STATUS "CMake project: ${PROJECT_NAME}")

# Test configuration
set(GENERATE_TESTS FALSE CACHE BOOL "Always build tests: add `make check_build` to normal `make` calls")
set(DISABLE_TESTS FALSE CACHE BOOL "Enable running `make check` during the building process")
set(VALGRIND_NUM_CALLERS 20 CACHE STRING "Valgrind option: maximum number of entries shown in stack traces")

message("If KurentoHelpers is not found, you need to install 'kms-cmake-utils' from the Kurento repository")
find_package(KurentoHelpers REQUIRED)

message("If CodeGenerator is not found, you need to install 'kms-core' from the Kurento repository")
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

# Generate file "config.h"
set(VERSION ${PROJECT_VERSION})
set(PACKAGE ${PROJECT_NAME})
set(GETTEXT_PACKAGE "kms-filters")
set(MANUAL_CHECK OFF CACHE BOOL "Tests will generate files")
set(BINARY_LOCATION "http://files.openvidu.io/" CACHE STRING "Storage with test files (as an URI, http:// or file:///)")
include(GNUInstallDirs)
set(KURENTO_MODULES_SO_DIR ${CMAKE_INSTALL_PREFIX}/${CMAKE_INSTALL_LIBDIR}/${KURENTO_MODULES_DIR_INSTALL_PREFIX})
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/config.h)
set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   -DHAVE_CONFIG_H")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHAVE_CONFIG_H")

set(GST_REQUIRED ^1.5.0)
set(GLIB_REQUIRED ^2.38)
set(OPENCV_REQUIRED <4.0.0)
set(LIBSOUP_REQUIRED ^2.40)

include(GenericFind)
generic_find(LIBNAME gstreamer-1.5 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-base-1.5 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-video-1.5 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-check-1.5 VERSION ${GST_REQUIRED})
generic_find(LIBNAME opencv VERSION ${OPENCV_REQUIRED} REQUIRED)
generic_find(LIBNAME libsoup-2.4 VERSION ${LIBSOUP_REQUIRED} REQUIRED)

set(CMAKE_INSTALL_GST_PLUGINS_DIR ${CMAKE_INSTALL_LIBDIR}/gstreamer-1.5)

enable_testing()

add_subdirectory(src)
add_subdirectory(tests)
