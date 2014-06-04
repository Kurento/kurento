cmake_minimum_required(VERSION 2.8)

set (PROJECT_NAME "kms-plugin-sample")

project (${PROJECT_NAME})

find_program(GIT_EXEC git)

execute_process(COMMAND ${GIT_EXEC} rev-parse --git-dir OUTPUT_VARIABLE GIT_DIR
                OUTPUT_STRIP_TRAILING_WHITESPACE)

if(EXISTS ${GIT_EXEC} AND EXISTS "${GIT_DIR}" AND EXISTS "${TAG}" )
  execute_process(COMMAND ${GIT_EXEC} submodule init WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})
  execute_process(COMMAND ${GIT_EXEC} submodule update WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})
  execute_process(COMMAND ${GIT_EXEC} remote update WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})
  execute_process(COMMAND ${GIT_EXEC} describe --abbrev=6 --tags --dirty --always --match "${TAG}*"
                  OUTPUT_VARIABLE PROJECT_VERSION
                  OUTPUT_STRIP_TRAILING_WHITESPACE
                  WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR})
  string(REGEX REPLACE "^${TAG}(.*)" "\\1" PROJECT_VERSION ${PROJECT_VERSION})
  if (NOT EXISTS ${GIT_DIR}/hooks/pre-commit)
    execute_process(COMMAND ln -s ${CMAKE_CURRENT_SOURCE_DIR}/hooks/pre-commit.hook
                    "${GIT_DIR}/hooks/pre-commit")
  endif ()
else()
  set(PROJECT_MAJOR_VERSION 0)
  set(PROJECT_MINOR_VERSION 0)
  set(PROJECT_PATCH_VERSION 1-dev)
  set(PROJECT_VERSION
      ${PROJECT_MAJOR_VERSION}.${PROJECT_MINOR_VERSION}.${PROJECT_PATCH_VERSION})
endif()

find_package(PkgConfig)

set (GST_REQUIRED 1.2.0)
set (GLIB_REQUIRED 2.38)
set (OPENCV_REQUIRED 2.0.0)

pkg_check_modules(GSTREAMER REQUIRED gstreamer-1.0>=${GST_REQUIRED})
pkg_check_modules(GSTREAMER_BASE REQUIRED gstreamer-base-1.0>=${GST_REQUIRED})
pkg_check_modules(GSTREAMER_VIDEO REQUIRED gstreamer-video-1.0>=${GST_REQUIRED})
pkg_check_modules(GSTREAMER_CHECK REQUIRED gstreamer-check-1.0>=${GST_REQUIRED})
pkg_check_modules(GSTREAMER_PBUTILS REQUIRED gstreamer-pbutils-1.0>=${GST_REQUIRED})
pkg_check_modules(GIO REQUIRED gio-2.0>=${GLIB_REQUIRED})
pkg_check_modules(OPENCV REQUIRED opencv>=${OPENCV_REQUIRED})

set (VERSION ${PROJECT_VERSION})
set (PACKAGE ${PROJECT_NAME})

include(GNUInstallDirs)

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/CMake")

set (CMAKE_INSTALL_MODULESDIR ${CMAKE_INSTALL_LIBDIR}/${PROJECT_NAME})
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/config.h.cmake ${CMAKE_CURRENT_BINARY_DIR}/config.h)

include_directories (${CMAKE_CURRENT_BINARY_DIR})

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -g -DHAVE_CONFIG_H -Werror -Wall -fPIC")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -DHAVE_CONFIG_H -Werror -Wall -fPIC")

include_directories(
  ${GSTREAMER_INCLUDE_DIRS}
  ${GSTREAMER_VIDEO_INCLUDE_DIRS}
  ${CMAKE_CURRENT_SOURCE_DIR}
  ${OPENCV_INCLUDE_DIRS}
  ${SOUP_INCLUDE_DIRS}
)

set(SAMPLEFILTER_SOURCES
  samplefilter.c
  kmssamplefilter.cpp kmssamplefilter.h
)

include(GLibHelpers)

set(ENUM_HEADERS
  kmssamplefilter.h
)

add_glib_enumtypes(SAMPLEFILTER_SOURCES kms-enumtypes KMS ${ENUM_HEADERS})

add_library(samplefilter MODULE ${SAMPLEFILTER_SOURCES})

target_link_libraries(samplefilter
  ${GSTREAMER_LIBRARIES}
  ${GSTREAMER_VIDEO_LIBRARIES}
  ${OPENCV_LIBRARIES}
  ${SOUP_LIBRARIES}
)

install(
  TARGETS samplefilter
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_MODULESDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

