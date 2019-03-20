#.rst:
# DpkgBuildFlags
# --------------
#
# Get the compiler flags provided by `dpkg-buildflags`.
# The returned flags are those that would be used by the Debian package
# generation tools, such as `dpkg` and `debhelper`. This allows to make
# local builds while closely following the compiler options that will be
# used by automatic builds in the actual generation of Debian packages.
#
# Functions:
# - dpkg_buildflags_get_c(<var>)
# - dpkg_buildflags_get_cxx(<var>)
#
# Arguments:
# - <var>: Variable where to store the compiler flags returned by
#   `dpkg-buildflags`. Created as an internal cache variable.

#=============================================================================
# (C) Copyright 2018 Kurento (http://kurento.org/)
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

find_program(FLAGS_EXECUTABLE dpkg-buildflags DOC "dpkg-buildflags program")
mark_as_advanced(FLAGS_EXECUTABLE)

function(dpkg_buildflags_get_cflags var)
  if(FLAGS_EXECUTABLE)
    execute_process(COMMAND ${FLAGS_EXECUTABLE} --get CPPFLAGS
        OUTPUT_VARIABLE cppflags
        ERROR_QUIET
        OUTPUT_STRIP_TRAILING_WHITESPACE)
    execute_process(COMMAND ${FLAGS_EXECUTABLE} --get CFLAGS
        OUTPUT_VARIABLE cflags
        ERROR_QUIET
        OUTPUT_STRIP_TRAILING_WHITESPACE)

    set(${var} "${cppflags} ${cflags}" CACHE INTERNAL "dpkg-buildflags for C")

    unset(cppflags)
    unset(cflags)
  endif()
endfunction()

function(dpkg_buildflags_get_cxxflags var)
  if(FLAGS_EXECUTABLE)
    execute_process(COMMAND ${FLAGS_EXECUTABLE} --get CPPFLAGS
        OUTPUT_VARIABLE cppflags
        ERROR_QUIET
        OUTPUT_STRIP_TRAILING_WHITESPACE)
    execute_process(COMMAND ${FLAGS_EXECUTABLE} --get CXXFLAGS
        OUTPUT_VARIABLE cxxflags
        ERROR_QUIET
        OUTPUT_STRIP_TRAILING_WHITESPACE)

    set(${var} "${cppflags} ${cxxflags}" CACHE INTERNAL "dpkg-buildflags for C++")

    unset(cppflags)
    unset(cxxflags)
  endif()
endfunction()

function(dpkg_buildflags_get_ldflags var)
  if(FLAGS_EXECUTABLE)
    execute_process(COMMAND ${FLAGS_EXECUTABLE} --get LDFLAGS
        OUTPUT_VARIABLE ldflags
        ERROR_QUIET
        OUTPUT_STRIP_TRAILING_WHITESPACE)

    set(${var} "${ldflags}" CACHE INTERNAL "dpkg-buildflags for linker")

    unset(ldflags)
  endif()
endfunction()
