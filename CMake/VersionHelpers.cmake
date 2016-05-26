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

function (parse_version)
  set (OPTION_PARAMS
  )

  set (ONE_VALUE_PARAMS
    VERSION
    MAJOR
    MINOR
    PATCH
    EXTRA
  )

  set (MULTI_VALUE_PARAMS
  )

  set (REQUIRED_PARAMS
    VERSION
    MAJOR
    MINOR
    PATCH
  )

  cmake_parse_arguments("PARAM" "${OPTION_PARAMS}" "${ONE_VALUE_PARAMS}" "${MULTI_VALUE_PARAMS}" ${ARGN})

  foreach (REQUIRED_PARAM ${REQUIRED_PARAMS})
    if (NOT DEFINED PARAM_${REQUIRED_PARAM})
      message (FATAL_ERROR "Required param ${REQUIRED_PARAM} is not set")
    endif()
  endforeach()

  string(REPLACE "." ";" VERSION_LIST ${PARAM_VERSION})

  if (DEFINED PARAM_MAJOR)
    list(GET VERSION_LIST 0 MAJOR_VERSION)
    set (${PARAM_MAJOR} ${MAJOR_VERSION} PARENT_SCOPE)
  else ()
    return()
  endif ()

  list (LENGTH VERSION_LIST _VERSION_LEN)

  if (DEFINED PARAM_MINOR)
    if (${_VERSION_LEN} GREATER 1)
      list(GET VERSION_LIST 1 ${PARAM_MINOR})
      set (${PARAM_MINOR} ${${PARAM_MINOR}} PARENT_SCOPE)
    endif()
  else ()
    return()
  endif ()

  if (DEFINED PARAM_PATCH)
    if (${_VERSION_LEN} GREATER 2)
      list(GET VERSION_LIST 2 ${PARAM_PATCH})
      string(REPLACE "~" ";" PARAM_PATCH_LIST ${${PARAM_PATCH}})
      list(GET PARAM_PATCH_LIST 0 ${PARAM_PATCH})
      set (${PARAM_PATCH} ${${PARAM_PATCH}} PARENT_SCOPE)

      if (DEFINED PARAM_EXTRA)
        list (LENGTH PARAM_PATCH_LIST PATCH_LEN)

        if (${PATCH_LEN} GREATER 1)
          foreach (IT RANGE 1 ${PATCH_LEN})
            list (GET PARAM_PATCH_LIST ${IT} TMP)
            set (${PARAM_EXTRA} "~${TMP}")
          endforeach ()
        endif()

        if (${_VERSION_LEN} GREATER 3)
          foreach (IT RANGE 3 ${_VERSION_LEN})
            list (GET VERSION_LIST ${IT} TMP)
            set (${PARAM_EXTRA} ".${TMP}")
          endforeach ()

          set (${PARAM_EXTRA} ${${PARAM_EXTRA}} PARENT_SCOPE)
        endif ()
      endif ()
    endif ()
  endif ()
endfunction()


function (next_major_version version next)
  parse_version (
    VERSION ${version}
    MAJOR major
    MINOR minor
    PATCH patch
  )

  math (EXPR major "${major} + 1")
  set (${next} ${major})
  if (DEFINED minor)
    set (${next} "${${next}}.0")
  endif ()

  if (DEFINED patch)
    set (${next} "${${next}}.0")
  endif ()

  set (${next} ${${next}} PARENT_SCOPE)
endfunction ()

function (next_minor_version version next)
  parse_version (
    VERSION ${version}
    MAJOR major
    MINOR minor
    PATCH patch
  )

  if (DEFINED minor)
    set (patch 0)
    math (EXPR minor "${minor} + 1")
  else ()
    set (patch 0)
    set (minor 1)
  endif ()

  set (${next} ${major})
  if (DEFINED minor)
    set (${next} "${${next}}.${minor}")
  endif ()

  if (DEFINED patch)
    set (${next} "${${next}}.${patch}")
  endif ()

  set (${next} ${${next}} PARENT_SCOPE)
endfunction ()

function (next_patch_version version next)
  parse_version (
    VERSION ${version}
    MAJOR major
    MINOR minor
    PATCH patch
  )

  if (NOT DEFINED patch)
    set (patch 1)
  else()
    math (EXPR patch "${patch} + 1")
  endif ()

  set (${next} ${major})
  if (NOT DEFINED minor)
    set (minor 0)
  endif ()
  set (${next} "${${next}}.${minor}")

  if (DEFINED patch)
    set (${next} "${${next}}.${patch}")
  endif ()

  set (${next} ${${next}} PARENT_SCOPE)
endfunction ()

function (next_version version next)
  parse_version (
    VERSION ${version}
    MAJOR major
    MINOR minor
    PATCH patch
  )

  if (DEFINED minor)
    set (patch 0)
    math (EXPR minor "${minor} + 1")
  else ()
    set (patch 0)
    set (minor 0)
    math (EXPR major "${major} + 1")
  endif ()

  set (${next} ${major})
  if (DEFINED minor)
    set (${next} "${${next}}.${minor}")
  endif ()

  if (DEFINED patch)
    set (${next} "${${next}}.${patch}")
  endif ()

  set (${next} ${${next}} PARENT_SCOPE)
endfunction ()

function (process_version VERSION OUTPUT_VERSION)
  string(FIND ${VERSION} "." FOUND)
  string(LENGTH ${VERSION} LEN)

  #Boost style version
  if (FOUND EQUAL -1 AND ${LEN} EQUAL 6)
    string(SUBSTRING ${VERSION} 0 1 MAJOR)
    string(SUBSTRING ${VERSION} 2 2 MINOR)
    string(SUBSTRING ${VERSION} 4 2 PATCH)

    set (VERSION "${MAJOR}")
    if (NOT "${MINOR}" EQUAL "")
      set (VERSION "${VERSION}.${MINOR}")
    endif ()
    if (NOT "${PATCH}" EQUAL "")
      set (VERSION "${VERSION}.${PATCH}")
    endif ()

  endif()

  set (${OUTPUT_VERSION} "${VERSION}" PARENT_SCOPE)
endfunction ()

function (check_version_internal TARGET_VERSION FOUND_VERSION OUTPUT_ERROR_VAR)
  process_version (${FOUND_VERSION} FOUND_VERSION)
  if (${TARGET_VERSION} MATCHES ".*AND.*")
    string (REPLACE "AND" ";" VESION_LIST ${TARGET_VERSION})

    foreach (VER ${VESION_LIST})
      string(STRIP ${VER} VER)
      check_version (${VER} ${FOUND_VERSION})
    endforeach()
  elseif (${TARGET_VERSION} MATCHES ".*OR.*")
    string (REPLACE "OR" ";" VESION_LIST ${TARGET_VERSION})

    foreach (VER ${VESION_LIST})
      string(STRIP ${VER} VER)
      check_version_internal (${VER} ${FOUND_VERSION} INTERNAL_OUPUT_ERROR)
      if (NOT DEFINED INTERNAL_OUPUT_ERROR OR "${INTERNAL_OUPUT_ERROR}" EQUAL "")
        return()
      endif ()
      unset (INTERNAL_OUPUT_ERROR)
    endforeach()
    set (${OUTPUT_ERROR_VAR} "Version does not match ${TARGET_VERSION} with ${FOUND_VERSION}" PARENT_SCOPE)
  elseif (${TARGET_VERSION} MATCHES "^~.*")
    string (REPLACE "~" "" INIT_TARGET ${TARGET_VERSION})

    next_version (${INIT_TARGET} END_TARGET)

    check_version ("<${END_TARGET}" ${FOUND_VERSION})
    check_version (">=${INIT_TARGET}" ${FOUND_VERSION})
  elseif (${TARGET_VERSION} MATCHES "^\\^.*")
    string (REPLACE "^" "" INIT_TARGET ${TARGET_VERSION})

    next_major_version (${INIT_TARGET} END_TARGET)

    check_version ("<${END_TARGET}" ${FOUND_VERSION})
    check_version (">=${INIT_TARGET}" ${FOUND_VERSION})
  elseif (${TARGET_VERSION} MATCHES "^>=.*")
    string (REPLACE ">=" "" CMP_VERSION ${TARGET_VERSION})
    if (${FOUND_VERSION} VERSION_GREATER ${CMP_VERSION})
    elseif (${FOUND_VERSION} VERSION_EQUAL ${CMP_VERSION})
      return ()
    else ()
      set (${OUTPUT_ERROR_VAR} "Version does not match ${TARGET_VERSION} with ${FOUND_VERSION}" PARENT_SCOPE)
    endif ()
  elseif (${TARGET_VERSION} MATCHES "^>.*")
    string (REPLACE ">" "" CMP_VERSION ${TARGET_VERSION})
    if (${FOUND_VERSION} VERSION_GREATER ${CMP_VERSION})
      return()
    else ()
      set (${OUTPUT_ERROR_VAR} "Version does not match ${TARGET_VERSION} with ${FOUND_VERSION}" PARENT_SCOPE)
    endif ()
  elseif (${TARGET_VERSION} MATCHES "^<=.*")
    string (REPLACE "<=" "" CMP_VERSION ${TARGET_VERSION})
    if (${FOUND_VERSION} VERSION_LESS ${CMP_VERSION})
      return ()
    elseif (${FOUND_VERSION} VERSION_EQUAL ${CMP_VERSION})
      return()
    else ()
      set (${OUTPUT_ERROR_VAR} "Version does not match ${TARGET_VERSION} with ${FOUND_VERSION}" PARENT_SCOPE)
    endif ()
  elseif (${TARGET_VERSION} MATCHES "^<.*")
    string (REPLACE "<" "" CMP_VERSION ${TARGET_VERSION})
    if (${FOUND_VERSION} VERSION_LESS ${CMP_VERSION})
      return ()
    else ()
      set (${OUTPUT_ERROR_VAR} "Version does not match ${TARGET_VERSION} with ${FOUND_VERSION}" PARENT_SCOPE)
    endif ()
  elseif (${TARGET_VERSION} VERSION_EQUAL ${FOUND_VERSION})
    return ()
  else ()
    set (${OUTPUT_ERROR_VAR} "Version does not match ${TARGET_VERSION} with ${FOUND_VERSION}" PARENT_SCOPE)
  endif ()
endfunction ()

function (check_version TARGET_VERSION FOUND_VERSION)
  check_version_internal (${TARGET_VERSION} ${FOUND_VERSION} OUTPUT_ERROR)
  if (DEFINED OUTPUT_ERROR AND NOT ${OUTPUT_ERROR} EQUAL "")
    message (FATAL_ERROR ${OUTPUT_ERROR})
  endif ()
endfunction()
