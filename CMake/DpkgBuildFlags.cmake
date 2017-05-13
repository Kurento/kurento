#.rst:
# DpkgBuildFlags
# --------------
#
# Get the build flags provided by "dpkg-buildflags" during package generation
#
# get_dpkg_buildflags_c(<var>)
# get_dpkg_buildflags_cxx(<var>)
#
# <var> - Variable to store the compiler flags used by the package
#         creation tools (dpkg and debhelper).
#         Will be created as an internal cache variable.

#=============================================================================
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
#=============================================================================

find_program(DBF_EXECUTABLE dpkg-buildflags DOC "dpkg-buildflags program")
mark_as_advanced(DBF_EXECUTABLE)

function(get_dpkg_buildflags_c var)
  if(DBF_EXECUTABLE)
    execute_process(COMMAND ${DBF_EXECUTABLE} --get CFLAGS
        OUTPUT_VARIABLE dbf_flags
        ERROR_QUIET
        OUTPUT_STRIP_TRAILING_WHITESPACE)
    execute_process(COMMAND ${DBF_EXECUTABLE} --get CPPFLAGS
        OUTPUT_VARIABLE dbf_cppflags
        ERROR_QUIET
        OUTPUT_STRIP_TRAILING_WHITESPACE)

    set(${var} "${dbf_flags} ${dbf_cppflags}" CACHE INTERNAL "dpkg-buildflags for C")

    unset(dbf_flags)
    unset(dbf_cppflags)
  endif()
endfunction()

function(get_dpkg_buildflags_cxx var)
  if(DBF_EXECUTABLE)
    execute_process(COMMAND ${DBF_EXECUTABLE} --get CXXFLAGS
        OUTPUT_VARIABLE dbf_flags
        ERROR_QUIET
        OUTPUT_STRIP_TRAILING_WHITESPACE)
    execute_process(COMMAND ${DBF_EXECUTABLE} --get CPPFLAGS
        OUTPUT_VARIABLE dbf_cppflags
        ERROR_QUIET
        OUTPUT_STRIP_TRAILING_WHITESPACE)

    set(${var} "${dbf_flags} ${dbf_cppflags}" CACHE INTERNAL "dpkg-buildflags for C++")

    unset(dbf_flags)
    unset(dbf_cppflags)
  endif()
endfunction()
