cmake_minimum_required(VERSION 2.8)

project("kms-elements")
message(STATUS "Running CMake for project: ${PROJECT_NAME}")

message("If KurentoHelpers is not found, you need to install 'kms-cmake-utils' from the Kurento repository")
find_package(KurentoHelpers REQUIRED)

# Set GCC flags common to all projects
# Note - default flags added by dpkg-buildflags for C and C++:
# -g -O2 -fstack-protector-strong -Wformat -Werror=format-security -Wdate-time -D_FORTIFY_SOURCE=2
include(DpkgBuildFlags)
get_dpkg_buildflags_c(DPKG_BUILDFLAGS_C)
get_dpkg_buildflags_cxx(DPKG_BUILDFLAGS_CXX)
set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   ${DPKG_BUILDFLAGS_C}   -std=c11   -Wall -Werror -pthread")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${DPKG_BUILDFLAGS_CXX} -std=c++11 -Wall -Werror -pthread")
set(CMAKE_C_FLAGS_DEBUG   "-g -O0") # FIXME Ideal is '-Og' but a bug in GCC prevents this,
set(CMAKE_CXX_FLAGS_DEBUG "-g -O0") # causing "may be used uninitialized" errors: https://gcc.gnu.org/bugzilla/show_bug.cgi?id=58455
set(CMAKE_C_FLAGS_RELEASE   "-DNDEBUG") # CMake adds '-O3' by default,
set(CMAKE_CXX_FLAGS_RELEASE "-DNDEBUG") # which would overwrite our '-O2' from dpkg-buildflags
set(CMAKE_POSITION_INDEPENDENT_CODE ON) # Use "-fPIC" for all targets, including static libs

# FIXME Disable error when macros __TIME__, __DATE__ or __TIMESTAMP__ are encountered
include(CheckCXXCompilerFlag)
CHECK_CXX_COMPILER_FLAG("-Wno-error=date-time" HAS_WARNING_DATE_TIME)
if(HAS_WARNING_DATE_TIME)
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-error=date-time")
endif()

# This project includes an input "config.h.cmake"
set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   -DHAVE_CONFIG_H")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHAVE_CONFIG_H")

message("If CodeGenerator is not found, you need to install 'kms-core' from the Kurento repository")
include(CodeGenerator)
get_values_from_model(PREFIX VALUE MODELS ${CMAKE_CURRENT_SOURCE_DIR}/src/server/interface KEYS version)

include(KurentoGitHelpers)
get_git_version(PROJECT_VERSION ${VALUE_VERSION})
install_git_hook(pre-commit ${CMAKE_CURRENT_SOURCE_DIR}/hooks/pre-commit.hook)

include(GenericFind)
generic_find(LIBNAME Boost REQUIRED COMPONENTS unit_test_framework)

set(GST_REQUIRED ^1.5.0)
set(GLIB_REQUIRED ^2.38)
set(SOUP_REQUIRED ^2.40)
set(NICE_REQUIRED ^0.1.13)
set(GLIBMM_REQUIRED ^2.37)

generic_find(LIBNAME gstreamer-1.5 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-base-1.5 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-video-1.5 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-app-1.5 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-check-1.5 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-sdp-1.5 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-rtp-1.5 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-pbutils-1.5 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-sctp-1.5 REQUIRED)
generic_find(LIBNAME glibmm-2.4 VERSION ${GLIBMM_REQUIRED} REQUIRED)
generic_find(LIBNAME KmsGstCommons REQUIRED)
generic_find(LIBNAME libsoup-2.4 VERSION ${SOUP_REQUIRED} REQUIRED)
generic_find(LIBNAME nice VERSION ${NICE_REQUIRED} REQUIRED)
generic_find(LIBNAME gio-2.0 VERSION ${GLIB_REQUIRED} REQUIRED)
generic_find(LIBNAME uuid REQUIRED)
generic_find(LIBNAME openssl REQUIRED)

set(VERSION ${PROJECT_VERSION})
set(PACKAGE ${PROJECT_NAME})
set(GETTEXT_PACKAGE "kms-elements")
set(MANUAL_CHECK OFF CACHE BOOL "Tests will generate files")
set(ENABLE_DEBUGGING_TESTS OFF CACHE BOOL "Enable test that are not yet stable")

include(GNUInstallDirs)

set(CMAKE_INSTALL_GST_PLUGINS_DIR ${CMAKE_INSTALL_LIBDIR}/gstreamer-1.5)
set(KURENTO_MODULES_SO_DIR ${CMAKE_INSTALL_PREFIX}/${CMAKE_INSTALL_LIBDIR}/${KURENTO_MODULES_DIR_INSTALL_PREFIX})

set(BINARY_LOCATION "http://files.kurento.org/" CACHE STRING "Local binary directory expressed as an URI ( http:// or file:/// )")

configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/config.h)

enable_testing()

message(STATUS "Compiling : ${PROJECT_NAME}-${PROJECT_VERSION}")

add_subdirectory(src)
add_subdirectory(tests)
