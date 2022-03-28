# - Try to find KurentoModuleCreator

#=============================================================================
# Copyright 2014 Kurento
#
#=============================================================================

set(KurentoModuleCreator_VERSION "${project.version}")
set(KurentoModuleCreator_FOUND 1)

include (FindPackageHandleStandardArgs)

find_program(KurentoModuleCreator_EXECUTABLE NAMES kurento-module-creator
  PATH_SUFFIXES
    scripts
)

message (STATUS "Found ${KurentoModuleCreator_EXECUTABLE}")

# handle the QUIETLY and REQUIRED options
find_package_handle_standard_args (KurentoModuleCreator 
  FOUND_VAR KurentoModuleCreator_FOUND
  REQUIRED_VARS KurentoModuleCreator_VERSION KurentoModuleCreator_EXECUTABLE
  VERSION_VAR KurentoModuleCreator_VERSION
)

mark_as_advanced(
  KurentoModuleCreator_FOUND
  KurentoModuleCreator_VERSION
  KurentoModuleCreator_EXECUTABLE
)
