# (C) Copyright 2016 Kurento (http://kurento.org/)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
include (CMakeParseArguments)
include (VersionHelpers)
find_package(PkgConfig)

function(generic_find)
  set (OPTION_PARAMS
    REQUIRED
  )

  set (ONE_VALUE_PARAMS
    LIBNAME
    VERSION
  )

  set (MULTI_VALUE_PARAMS
    COMPONENTS
  )

  set (REQUIRED_PARAMS
    LIBNAME
  )

  cmake_parse_arguments("GF" "${OPTION_PARAMS}" "${ONE_VALUE_PARAMS}" "${MULTI_VALUE_PARAMS}" ${ARGN})

  foreach (REQUIRED_PARAM ${REQUIRED_PARAMS})
    if (NOT DEFINED GF_${REQUIRED_PARAM})
      message (FATAL_ERROR "Required param ${REQUIRED_PARAM} is not set")
    endif()
  endforeach()

  if (${${GF_LIBNAME}_FOUND})
    set (SEARCH_AGAIN FALSE)

    if (DEFINED ${GF_LIBNAME}_PREVIOUS_BUILD_DIR)
      if (NOT "${${GF_LIBNAME}_PREVIOUS_BUILD_DIR}" STREQUAL "${CMAKE_BINARY_DIR}")
        message (WARNING "Library ${GF_LIBNAME} has to be searched again, previous location: ${${GF_LIBNAME}_PREVIOUS_BUILD_DIR}, current: ${CMAKE_BINARY_DIR}")
        set (SEARCH_AGAIN TRUE)
      endif()
    endif()

    if (NOT ${SEARCH_AGAIN})
      if (DEFINED GF_VERSION AND NOT ${GF_LIBNAME}_REQ_VERSION STREQUAL ${GF_VERSION}"")
        check_version_internal (${GF_VERSION} ${${GF_LIBNAME}_VERSION} OUTPUT_ERROR)
        if (DEFINED OUTPUT_ERROR AND NOT ${OUTPUT_ERROR} EQUAL "")
            set (SEARCH_AGAIN TRUE)
        endif ()
      endif()

      if (DEFINED GF_COMPONENTS)
        foreach (COMP ${GF_COMPONENTS})
          list(FIND GEN_${GF_LIBNAME}_COMPONENTS ${COMP} COMP_FOUND)
          if (${COMP_FOUND} EQUAL -1)
            set (SEARCH_AGAIN TRUE)
          endif()
        endforeach()
      endif ()
    endif ()

    set (FORCE_GENERIC_FIND FALSE CACHE BOOLEAN "Force search of libraries each time cmake is executed")

    if (NOT SEARCH_AGAIN AND NOT FORCE_GENERIC_FIND)
      message (STATUS "${GF_LIBNAME} Already found")
      return ()
    endif()
  endif ()

  if (DEFINED ${GF_LIBNAME}_FOUND)
    unset (${GF_LIBNAME}_FOUND CACHE)
  endif()
  if (DEFINED ${GF_LIBNAME}_INCLUDE_DIRS)
    unset (${GF_LIBNAME}_INCLUDE_DIRS CACHE)
  endif()
  if (DEFINED ${GF_LIBNAME}_LIBRARIES)
    unset (${GF_LIBNAME}_LIBRARIES CACHE)
  endif()
  if (DEFINED ${GF_LIBNAME}_EXECUTABLE)
    unset (${GF_LIBNAME}_EXECUTABLE CACHE)
  endif()

  if (DEFINED GF_COMPONENTS)
    foreach (COMP ${GF_COMPONENTS})
      list(FIND GEN_${GF_LIBNAME}_COMPONENTS ${COMP} COMP_FOUND)
      if (${COMP_FOUND} EQUAL -1)

        list (APPEND GEN_${GF_LIBNAME}_COMPONENTS ${COMP})
      endif()
    endforeach ()
    set (GEN_${GF_LIBNAME}_COMPONENTS ${GEN_${GF_LIBNAME}_COMPONENTS} CACHE INTERNAL "" FORCE)
    mark_as_advanced (GEN_${GF_LIBNAME}_COMPONENTS)

    if (DEFINED GF_REQUIRED)
      find_package(${GF_LIBNAME} REQUIRED COMPONENTS ${GEN_${GF_LIBNAME}_COMPONENTS})
    else ()
      find_package(${GF_LIBNAME} COMPONENTS ${GEN_${GF_LIBNAME}_COMPONENTS})
    endif ()
  else ()
    find_package(${GF_LIBNAME} QUIET)
  endif ()

  if (DEFINED ${GF_LIBNAME}_FOUND AND ${${GF_LIBNAME}_FOUND})
    if (NOT DEFINED GF_COMPONENTS)
      if (DEFINED GF_REQUIRED)
        find_package(${GF_LIBNAME})
      else()
        find_package(${GF_LIBNAME} REQUIRED)
      endif()
    endif()
  else()
    if (DEFINED GF_REQUIRED)
      pkg_check_modules(${GF_LIBNAME} ${GF_LIBNAME})
    else()
      pkg_check_modules(${GF_LIBNAME} ${GF_LIBNAME})
    endif()
  endif()

  if (NOT "${${GF_LIBNAME}_FOUND}")
    if ("${GF_REQUIRED}")
      message (FATAL_ERROR "Library ${GF_LIBNAME} not found")
    else ()
      message (STATUS "Library ${GF_LIBNAME} not found")
    endif ()
    return()
  endif ()

  if (DEFINED GF_VERSION)
    message (STATUS "Resolving ${GF_LIBNAME} version ${GF_VERSION} with ${${GF_LIBNAME}_VERSION}")
    check_version(${GF_VERSION} ${${GF_LIBNAME}_VERSION})
  elseif (DEFINED GF_COMPONENTS)
    message (STATUS "Found ${GF_LIBNAME} COMPONENTS ${GEN_${GF_LIBNAME}_COMPONENTS}")
  else ()
    message (STATUS "Found ${GF_LIBNAME}")
  endif ()

  set (${GF_LIBNAME}_VERSION ${${GF_LIBNAME}_VERSION} CACHE PATH "${GF_LIBNAME} Version" FORCE)

  set (${GF_LIBNAME}_LIBRARIES ${${GF_LIBNAME}_LIBRARIES} PARENT_SCOPE)
  set (${GF_LIBNAME}_LIBRARIES ${${GF_LIBNAME}_LIBRARIES} CACHE PATH "${GF_LIBNAME}_LIBRARIES" FORCE)

  set (${GF_LIBNAME}_INCLUDE_DIRS ${${GF_LIBNAME}_INCLUDE_DIRS} PARENT_SCOPE)
  set (${GF_LIBNAME}_INCLUDE_DIRS ${${GF_LIBNAME}_INCLUDE_DIRS} CACHE PATH "${GF_LIBNAME}_INCLUDE_DIRS" FORCE)

  if (DEFINED ${GF_LIBNAME}_EXECUTABLE)
    set (${GF_LIBNAME}_EXECUTABLE ${${GF_LIBNAME}_EXECUTABLE} PARENT_SCOPE)
    set (${GF_LIBNAME}_EXECUTABLE ${${GF_LIBNAME}_EXECUTABLE} CACHE PATH "${GF_LIBNAME}_EXECUTABLE" FORCE)
    if ("${${GF_LIBNAME}_EXECUTABLE}" MATCHES ".*${CMAKE_BINARY_DIR}.*")
      set (${GF_LIBNAME}_PREVIOUS_BUILD_DIR ${CMAKE_BINARY_DIR} CACHE INTERNAL "Library previous search" FORCE)
    endif()
  endif ()

  set (${GF_LIBNAME}_FOUND TRUE CACHE BOOL "${GF_LIBNAME}_FOUND")
  mark_as_advanced (${GF_LIBNAME}_FOUND)
  set (${GF_LIBNAME}_FOUND TRUE)

  if ("${${GF_LIBNAME}_LIBRARIES}" MATCHES ".*${CMAKE_BINARY_DIR}.*" OR "${${GF_LIBNAME}_INCLUDE_DIRS}" MATCHES ".*${CMAKE_BINARY_DIR}.*")
    set (${GF_LIBNAME}_PREVIOUS_BUILD_DIR ${CMAKE_BINARY_DIR} CACHE INTERNAL "Library previous search" FORCE)
  endif()


  if (DEFINED GF_VERSION)
    set (${GF_LIBNAME}_REQ_VERSION ${GF_VERSION} CACHE STRING "${GF_LIBNAME} requested version" FORCE)
    mark_as_advanced (${GF_LIBNAME}_REQ_VERSION)
  endif()
endfunction()
