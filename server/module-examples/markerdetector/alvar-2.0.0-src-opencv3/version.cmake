# define internal release
# - when set to true, the revision from the version control system will be appended to the version string
# - always ensure that this is set to false when commiting this file to the version control system
set(ALVAR_INTERNAL_RELEASE FALSE)

# set project version
set(ALVAR_VERSION_MAJOR 2)
set(ALVAR_VERSION_MINOR 0)
set(ALVAR_VERSION_PATCH 0)

# denote special version tag
# examples: a1, a2, a3, b1, b2, b3, rc1, rc2, rc3
# legend: a = alpha, b = beta, rc = release candidate
set(ALVAR_VERSION_TAG "")

# extract project revision from version control system
if(ALVAR_INTERNAL_RELEASE)
    set(_git_executable "git")
    if(WIN32)
        set(_git_executable "git.cmd")
    endif(WIN32)
    execute_process(COMMAND ${_git_executable} rev-parse HEAD WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR} OUTPUT_VARIABLE _githead)
    set(ALVAR_VERSION_REVISION internal)
    if(${_githead} MATCHES "^.+$")
        string(SUBSTRING ${_githead} 0 7 ALVAR_VERSION_REVISION)
    endif(${_githead} MATCHES "^.+$")
    set(ALVAR_VERSION_REVISION git${ALVAR_VERSION_REVISION})
endif(ALVAR_INTERNAL_RELEASE)

# set project version strings
set(ALVAR_VERSION ${ALVAR_VERSION_MAJOR}.${ALVAR_VERSION_MINOR}.${ALVAR_VERSION_PATCH})
if(ALVAR_VERSION_TAG MATCHES "^.+$")
    set(ALVAR_VERSION ${ALVAR_VERSION}.${ALVAR_VERSION_TAG})
endif(ALVAR_VERSION_TAG MATCHES "^.+$")
if(ALVAR_INTERNAL_RELEASE)
    set(ALVAR_VERSION ${ALVAR_VERSION}.${ALVAR_VERSION_REVISION})
endif(ALVAR_INTERNAL_RELEASE)
string(REGEX REPLACE "\\." "" ALVAR_VERSION_NODOTS ${ALVAR_VERSION})
