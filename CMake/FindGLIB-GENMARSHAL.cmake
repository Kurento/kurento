# - Try to find GLIB-GENMARSHAL
# - Copied from one of cmake templates
# -------------------------
# Once done this will define
#
#  GLIB-GENMARSHAL_FOUND - system has GLIB-GENMARSHAL
#  GLIB-GENMARSHAL_EXECUTABLE - the GLIB-GENMARSHAL executable

#=============================================================================
# Copyright 2014 Kurento
#
#=============================================================================

FIND_PROGRAM(GLIB-GENMARSHAL_EXECUTABLE NAMES glib-genmarshal
        HINTS ENV${GLIB-GENMARSHAL_ROOT}/asn1c ${GLIB-GENMARSHAL_ROOT}/asn1c)

# handle the QUIETLY and REQUIRED arguments and set GLIB-GENMARSHAL_FOUND to TRUE if
# all listed variables are TRUE
INCLUDE(FindPackageHandleStandardArgs)
FIND_PACKAGE_HANDLE_STANDARD_ARGS(GLIB-GENMARSHAL DEFAULT_MSG GLIB-GENMARSHAL_EXECUTABLE)

MARK_AS_ADVANCED(GLIB-GENMARSHAL_EXECUTABLE)
