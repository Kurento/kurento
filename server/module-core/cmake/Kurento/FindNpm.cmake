# - Try to find Npm
# - Copied from one of cmake templates
# -------------------------
# Once done this will define
#
#  Npm_FOUND - system has Npm
#  Npm_EXECUTABLE - the Npm executable

#=============================================================================
# Copyright 2014 Kurento
#
#=============================================================================

set (NPM_ROOT /usr/bin CACHE STRING "npm directory")

find_program(Npm_EXECUTABLE NAMES npm
        HINTS ENV${NPM_ROOT}/npm ${NPM_ROOT}/npm)

# handle the QUIETLY and REQUIRED arguments and set Npm_FOUND to TRUE if
# all listed variables are TRUE
include(FindPackageHandleStandardArgs)
find_package_handle_standard_args (Npm
  FOUND_VAR Npm_FOUND
  REQUIRED_VARS Npm_EXECUTABLE
)

mark_as_advanced(Npm_FOUND Npm_EXECUTABLE)
