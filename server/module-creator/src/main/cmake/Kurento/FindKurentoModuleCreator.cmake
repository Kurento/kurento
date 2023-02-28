# - Try to find KurentoModuleCreator

#=============================================================================
# Copyright 2014 Kurento
#
#=============================================================================

set(KurentoModuleCreator_VERSION "${project.version}")



# Path to program
# ===============

find_program(KurentoModuleCreator_EXECUTABLE
  NAMES
    kurento-module-creator
  PATH_SUFFIXES
    scripts
)



# Output variables
# ================

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(KurentoModuleCreator
  FOUND_VAR
    KurentoModuleCreator_FOUND
  REQUIRED_VARS
    KurentoModuleCreator_VERSION
    KurentoModuleCreator_EXECUTABLE
  VERSION_VAR
    KurentoModuleCreator_VERSION
)

mark_as_advanced(
  KurentoModuleCreator_FOUND
  KurentoModuleCreator_VERSION
  KurentoModuleCreator_EXECUTABLE
)



# Log lookup result
# =================

get_filename_component(CURRENT_FILE ${CMAKE_CURRENT_LIST_FILE} NAME)

if(KurentoModuleCreator_FOUND)
  message(STATUS "[${CURRENT_FILE}] Found: ${KurentoModuleCreator_EXECUTABLE}")
else()
  message(STATUS "[${CURRENT_FILE}] Not found")
endif()
