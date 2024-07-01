cmake_minimum_required(VERSION 3.0)

project("kurento-module-core")
message(STATUS "CMake project: ${PROJECT_NAME}")

include(GNUInstallDirs) # CMAKE_INSTALL_*

# Test configuration
set(GENERATE_TESTS FALSE CACHE BOOL "Always build tests: add `make check_build` to normal `make` calls")
set(DISABLE_TESTS FALSE CACHE BOOL "Enable running `make check` during the building process")
set(VALGRIND_NUM_CALLERS 20 CACHE STRING "Valgrind option: maximum number of entries shown in stack traces")
set(ENABLE_EXPERIMENTAL_TESTS OFF CACHE BOOL "Enable tests that are not yet stable")
enable_testing()

# Add CMake modules provided by this project.
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/cmake/Kurento")
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}/src/server")
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}") # find_package() search path for `FindKmsGstCommons.cmake`.

# Export CMake search paths if this is not the top-level project.
get_directory_property(HAS_PARENT PARENT_DIRECTORY)
if(HAS_PARENT)
  set(CMAKE_MODULE_PATH "${CMAKE_MODULE_PATH}" PARENT_SCOPE)
endif()

# Declare installation of CMake modules provided by this project.
install(
  FILES
    cmake/Kurento/CodeGenerator.cmake
    cmake/Kurento/FindMaven.cmake
    cmake/Kurento/FindNpm.cmake
  DESTINATION ${CMAKE_INSTALL_DATAROOTDIR}/cmake/Kurento
)

find_package(KurentoUtils)
if(NOT KurentoUtils_FOUND)
  message(FATAL_ERROR "KurentoUtils not found; please install package 'kurento-cmake-utils'")
endif()

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

# Decide between std::regex or boost::regex
include(CheckCXXSourceCompiles)
set(CMAKE_REQUIRED_FLAGS "${CMAKE_CXX_FLAGS}")
check_cxx_source_compiles("
  #include <iostream>
  #include <regex>
  int main() {
    std::regex re(\"//*\");
    std::string orig = \"//\";
    std::cout << std::regex_replace(orig, re, \"/\") << std::endl;
    return 0;
  }"
  HAS_STD_REGEX_REPLACE
)
unset(CMAKE_REQUIRED_FLAGS)

# Detect if pthread_setname is available
include(CheckCSourceCompiles)
set(CMAKE_REQUIRED_LIBRARIES pthread)
check_c_source_compiles("
  #include <pthread.h>
  int main() {
    pthread_setname_np((pthread_t)0, (const char *)0);
    return 0;
  }"
  HAVE_PTHREAD_SETNAME_NP_WITH_TID
)
unset(CMAKE_REQUIRED_LIBRARIES)
if(${HAVE_PTHREAD_SETNAME_NP_WITH_TID})
  set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   -DHAVE_PTHREAD_SETNAME_NP_WITH_TID")
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHAVE_PTHREAD_SETNAME_NP_WITH_TID")
endif()

# Generate file "config.h"
set(VERSION "${PROJECT_VERSION}")
set(PACKAGE "${PROJECT_NAME}")
set(GETTEXT_PACKAGE "${PROJECT_NAME}")
set(MANUAL_CHECK OFF CACHE BOOL "Tests will generate files")
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/config.h)
set(CMAKE_C_FLAGS   "${CMAKE_C_FLAGS}   -DHAVE_CONFIG_H")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DHAVE_CONFIG_H")

set(GST_REQUIRED ^1.7.1)
set(JSON_RPC_REQUIRED ^7.0.0)
set(SIGCPP_REQUIRED ^2.0.10)
set(GLIBMM_REQUIRED ^2.37)

include(GenericFind)

if(${HAS_STD_REGEX_REPLACE})
  set(BOOST_REQUIRED_COMPONENTS "filesystem system thread")
  generic_find(LIBNAME Boost REQUIRED COMPONENTS filesystem system thread unit_test_framework)
else()
  set(BOOST_REQUIRED_COMPONENTS "filesystem regex system thread")
  generic_find(LIBNAME Boost REQUIRED COMPONENTS filesystem regex system thread unit_test_framework)
endif()

generic_find(LIBNAME gstreamer-1.0 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-base-1.0 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-video-1.0 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-check-1.0 VERSION ${GST_REQUIRED})
generic_find(LIBNAME gstreamer-sdp-1.0 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-pbutils-1.0 VERSION ${GST_REQUIRED} REQUIRED)
generic_find(LIBNAME gstreamer-rtp-1.0 VERSION ${GST_REQUIRED} REQUIRED)

generic_find(LIBNAME KmsJsonRpc VERSION ${JSON_RPC_REQUIRED} REQUIRED)
generic_find(LIBNAME sigc++-2.0 VERSION ${SIGCPP_REQUIRED} REQUIRED)
generic_find(LIBNAME glibmm-2.4 VERSION ${GLIBMM_REQUIRED} REQUIRED)
generic_find(LIBNAME uuid REQUIRED)

find_package(LibVpx REQUIRED)

set(CMAKE_INSTALL_GST_PLUGINS_DIR ${CMAKE_INSTALL_LIBDIR}/gstreamer-1.0)

add_subdirectory(src)
add_subdirectory(tests)
