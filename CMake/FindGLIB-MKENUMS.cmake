# - Try to find GLIB-MKENUMS
# - Copied from one of cmake templates
# -------------------------
# Once done this will define
#
#  GLIB-MKENUMS_FOUND - system has GLIB-MKENUMS
#  GLIB-MKENUMS_EXECUTABLE - the GLIB-MKENUMS executable

#=============================================================================
# Copyright 2014 Kurento
#
#=============================================================================

FIND_PROGRAM(GLIB-MKENUMS_EXECUTABLE NAMES glib-mkenums
        HINTS ENV${GLIB-MKENUMS_ROOT}/glib-mkenums ${GLIB-MKENUMS_ROOT}/glib-mkenums)

# handle the QUIETLY and REQUIRED arguments and set GLIB-MKENUMS_FOUND to TRUE if
# all listed variables are TRUE
INCLUDE(FindPackageHandleStandardArgs)
FIND_PACKAGE_HANDLE_STANDARD_ARGS(GLIB-MKENUMS DEFAULT_MSG GLIB-MKENUMS_EXECUTABLE)

MARK_AS_ADVANCED(GLIB-MKENUMS_EXECUTABLE)
