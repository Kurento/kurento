cmake_minimum_required(VERSION 2.8)

project ("kms-filters")

message ("If KurentoHelpers is not found, then it is necessary to install
          kms-cmake-utils package from the kurento repository")

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/CMake")
find_package(KurentoHelpers REQUIRED)

include (KurentoGitHelpers)
message ("If CodeGenerator is not found, then it is necessary to install
          kms-core package from the kurento repository")
include (CodeGenerator)

get_values_from_model(PREFIX VALUE MODELS ${CMAKE_SOURCE_DIR}/src/server/interface KEYS version)

get_git_version (PROJECT_VERSION ${VALUE_VERSION})
install_git_hook (pre-commit ${CMAKE_CURRENT_SOURCE_DIR}/hooks/pre-commit.hook)

include (GenericFind)

set (GST_REQUIRED ~1.4)
set (GLIB_REQUIRED ^2.38)
set (OPENCV_REQUIRED ^2.0.0)
set (LIBSOUP_REQUIRED ^2.40)

#gst-plugins dependencies
generic_find (LIBNAME gstreamer-1.0 VERSION ${GST_REQUIRED} REQUIRED)
generic_find (LIBNAME gstreamer-base-1.0 VERSION ${GST_REQUIRED} REQUIRED)
generic_find (LIBNAME gstreamer-video-1.0 VERSION ${GST_REQUIRED} REQUIRED)
generic_find (LIBNAME gstreamer-check-1.0 VERSION ${GST_REQUIRED})
generic_find (LIBNAME opencv VERSION ${OPENCV_REQUIRED} REQUIRED)
generic_find (LIBNAME libsoup-2.4 VERSION ${LIBSOUP_REQUIRED} REQUIRED)

set (VERSION ${PROJECT_VERSION})
set (PACKAGE ${PROJECT_NAME})
set (GETTEXT_PACKAGE "kms-filters")
set (MANUAL_CHECK OFF CACHE BOOL "Tests will generate files")

include(GNUInstallDirs)

set (CMAKE_INSTALL_GST_PLUGINS_DIR ${CMAKE_INSTALL_LIBDIR}/gstreamer-1.0)
set (KURENTO_MODULES_SO_DIR ${CMAKE_INSTALL_PREFIX}/${CMAKE_INSTALL_LIBDIR}/${KURENTO_MODULES_DIR_INSTALL_PREFIX})

set (BINARY_LOCATION "http://files.kurento.org/" CACHE STRING "Local binary directory expressed as an URI ( http:// or file:/// )")

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/config.h)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -g -DHAVE_CONFIG_H -Werror -Wall -Werror=declaration-after-statement")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -DHAVE_CONFIG_H -Wall -Werror -std=c++11")

enable_testing()

include_directories(${CMAKE_CURRENT_BINARY_DIR})
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

message(STATUS "Compiling : ${PROJECT_NAME}-${PROJECT_VERSION}")

add_subdirectory (src)
add_subdirectory(tests)
