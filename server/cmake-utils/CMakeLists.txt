cmake_minimum_required(VERSION 3.0)

project("kurento-cmake-utils")
message(STATUS "CMake project: ${PROJECT_NAME}")

include(GNUInstallDirs) # CMAKE_INSTALL_*

# Test configuration
set(GENERATE_TESTS FALSE CACHE BOOL "Always build tests: add `make check_build` to normal `make` calls")
set(DISABLE_TESTS FALSE CACHE BOOL "Enable running `make check` during the building process")
set(VALGRIND_NUM_CALLERS 20 CACHE STRING "Valgrind option: maximum number of entries shown in stack traces")

# Add CMake modules provided by this project.
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/cmake/Kurento")
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/3rdparty/sanitizers-cmake/cmake")
list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_BINARY_DIR}") # find_package() search path for `FindKurentoUtils.cmake`.
list(INSERT CMAKE_PREFIX_PATH 0 "${CMAKE_CURRENT_SOURCE_DIR}/cmake/KurentoUtils") # find_package() search path for `KurentoUtilsConfig.cmake`.

# Export CMake search paths if this is not the top-level project.
get_directory_property(HAS_PARENT PARENT_DIRECTORY)
if(HAS_PARENT)
  set(CMAKE_MODULE_PATH "${CMAKE_MODULE_PATH}" PARENT_SCOPE)
  set(CMAKE_PREFIX_PATH "${CMAKE_PREFIX_PATH}" PARENT_SCOPE)
endif()

include(KurentoGitHelpers)
get_git_version(PROJECT_VERSION 7.0.1-dev)
message(STATUS "Project version: ${PROJECT_NAME}-${PROJECT_VERSION}")

# CMake Config Mode search file. Adds the main Kurento dir to CMAKE_MODULE_PATH.
# https://cmake.org/cmake/help/latest/command/find_package.html#config-mode-search-procedure
install(
  FILES cmake/KurentoUtils/KurentoUtilsConfig.cmake
  DESTINATION ${CMAKE_INSTALL_DATAROOTDIR}/cmake/KurentoUtils
)

# Kurento CMake modules.
install(
  FILES
    cmake/Kurento/DpkgBuildFlags.cmake
    cmake/Kurento/CommonBuildFlags.cmake
    cmake/Kurento/FindGLIB-GENMARSHAL.cmake
    cmake/Kurento/FindGLIB-MKENUMS.cmake
    cmake/Kurento/GLibHelpers.cmake
    cmake/Kurento/GenericFind.cmake
    cmake/Kurento/KurentoGitHelpers.cmake
    cmake/Kurento/TestHelpers.cmake
    cmake/Kurento/VersionHelpers.cmake
  DESTINATION ${CMAKE_INSTALL_DATAROOTDIR}/cmake/Kurento
)

configure_file(FindKurentoUtils.cmake.in
  ${PROJECT_BINARY_DIR}/FindKurentoUtils.cmake
  @ONLY
)
install(
  FILES ${PROJECT_BINARY_DIR}/FindKurentoUtils.cmake
  DESTINATION ${CMAKE_INSTALL_DATAROOTDIR}/cmake/Kurento
)

if(NOT ${DISABLE_TESTS})
  add_subdirectory(test)
endif()
