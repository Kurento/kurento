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
