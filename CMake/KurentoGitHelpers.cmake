cmake_minimum_required(VERSION 2.8)

find_package(Git)

function (get_git_dir git_dir_output_variable)
  if(EXISTS ${GIT_EXECUTABLE})
    execute_process(COMMAND ${GIT_EXECUTABLE} rev-parse --git-dir
        OUTPUT_VARIABLE git_dir
        OUTPUT_STRIP_TRAILING_WHITESPACE
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    )
    if (DEFINED git_dir AND NOT ${git_dir} EQUAL "")
      if (NOT IS_ABSOLUTE ${git_dir})
        set (git_dir ${CMAKE_CURRENT_SOURCE_DIR}/${git_dir})
      endif ()
      if (EXISTS ${git_dir})
        set(${git_dir_output_variable} ${git_dir} PARENT_SCOPE)
      endif ()
    endif ()
  endif ()
endfunction()

function (install_git_hook hook_type hook_location)
  get_git_dir (GIT_DIR)

  if (DEFINED GIT_DIR)
    execute_process(COMMAND ln -b -s ${hook_location}
                    "${GIT_DIR}/hooks/${hook_type}")
  endif ()
endfunction()

set (CALCULATE_VERSION_WITH_GIT TRUE CACHE BOOL "Use git (if available) to get project version")

include (CMakeParseArguments)
include (VersionHelpers)

# get_git_version (version_output_variable default_version [TAG_PREFIX tag_prefix])
# Default tag_prefix ${PROJECT_NAME}
function (get_git_version version_output_variable default_version)
  get_git_dir (GIT_DIR)

  cmake_parse_arguments(GIT_VERSION "" "TAG_PREFIX" "" ${ARGN})

  if (NOT DEFINED GIT_VERSION_TAG_PREFIX)
    set (GIT_VERSION_TAG_PREFIX ${PROJECT_NAME})
  endif ()

  if(EXISTS "${GIT_DIR}" AND ${CALCULATE_VERSION_WITH_GIT})
    execute_process(COMMAND ${GIT_EXECUTABLE} rev-list origin/master..HEAD --count
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

    message (STATUS "Version got from git is ${PROJECT_VERSION}")

  else()
    set(PROJECT_VERSION ${default_version})
  endif()

  parse_version (
    VERSION ${PROJECT_VERSION}
    MAJOR PROJECT_VERSION_MAJOR
    MINOR PROJECT_VERSION_MINOR
    PATCH PROJECT_VERSION_PATCH
  )

  set(PROJECT_VERSION_MAJOR ${PROJECT_VERSION_MAJOR} PARENT_SCOPE)
  set(PROJECT_VERSION_MINOR ${PROJECT_VERSION_MINOR} PARENT_SCOPE)
  set(PROJECT_VERSION_PATCH ${PROJECT_VERSION_PATCH} PARENT_SCOPE)

  set(${version_output_variable} ${PROJECT_VERSION} PARENT_SCOPE)
endfunction ()
