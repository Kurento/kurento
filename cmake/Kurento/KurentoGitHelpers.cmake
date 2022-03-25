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
cmake_minimum_required(VERSION 2.8)

find_package(Git)

function(get_git_dir git_dir_output_variable)
  if(EXISTS ${GIT_EXECUTABLE})
    execute_process(COMMAND ${GIT_EXECUTABLE} rev-parse --git-dir
        OUTPUT_VARIABLE git_dir
        ERROR_VARIABLE ignored
        OUTPUT_STRIP_TRAILING_WHITESPACE
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    )
    if(DEFINED git_dir AND NOT ${git_dir} EQUAL "")
      if(NOT IS_ABSOLUTE ${git_dir})
        set(git_dir ${CMAKE_CURRENT_SOURCE_DIR}/${git_dir})
      endif()
      if(EXISTS ${git_dir})
        set(${git_dir_output_variable} ${git_dir} PARENT_SCOPE)
      endif()
    endif()
  endif()
endfunction()

function(install_git_hook hook_type hook_location)
  get_git_dir(GIT_DIR)

  if(DEFINED GIT_DIR)
    execute_process(COMMAND ln -b -s ${hook_location}
                    "${GIT_DIR}/hooks/${hook_type}")
  endif()
endfunction()

include(VersionHelpers)

set (CALCULATE_VERSION_WITH_GIT TRUE CACHE BOOL "Use git (if available) to get project version")

function(get_git_version version_output_variable default_version)
  get_git_dir(GIT_DIR)

  if(EXISTS "${GIT_DIR}" AND ${CALCULATE_VERSION_WITH_GIT})
    execute_process(COMMAND ${GIT_EXECUTABLE} describe --abbrev=0 --tags
      OUTPUT_VARIABLE LAST_TAG
      ERROR_VARIABLE DISCARD_ERROR
      OUTPUT_STRIP_TRAILING_WHITESPACE
      WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    )

    if(NOT LAST_TAG)
      execute_process(COMMAND ${GIT_EXECUTABLE} rev-list --max-parents=0 HEAD
        OUTPUT_VARIABLE LAST_TAG
        ERROR_VARIABLE DISCARD_ERROR
        OUTPUT_STRIP_TRAILING_WHITESPACE
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
      )
    endif()

    execute_process(COMMAND ${GIT_EXECUTABLE} rev-list ${LAST_TAG}..HEAD --count
      OUTPUT_VARIABLE N_COMMITS
      OUTPUT_STRIP_TRAILING_WHITESPACE
      WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    )

    execute_process(COMMAND ${GIT_EXECUTABLE} rev-parse --short HEAD
      OUTPUT_VARIABLE LAST_HASH
      OUTPUT_STRIP_TRAILING_WHITESPACE
      WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    )

    string(REPLACE
      "-dev" "~${N_COMMITS}.g${LAST_HASH}"
      PROJECT_VERSION ${default_version}
    )

    message(STATUS "Version info from git: ${PROJECT_VERSION}")
  else()
    set(PROJECT_VERSION ${default_version})
    message(STATUS "No version info from git. Using default: ${PROJECT_VERSION}")
  endif()

  parse_version(
    VERSION ${PROJECT_VERSION}
    MAJOR PROJECT_VERSION_MAJOR
    MINOR PROJECT_VERSION_MINOR
    PATCH PROJECT_VERSION_PATCH
  )

  set(${version_output_variable}_MAJOR ${PROJECT_VERSION_MAJOR} PARENT_SCOPE)
  set(${version_output_variable}_MINOR ${PROJECT_VERSION_MINOR} PARENT_SCOPE)
  set(${version_output_variable}_PATCH ${PROJECT_VERSION_PATCH} PARENT_SCOPE)

  set(${version_output_variable} ${PROJECT_VERSION} PARENT_SCOPE)
endfunction()
