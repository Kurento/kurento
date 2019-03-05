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

find_program(VALGRIND_EXECUTABLE valgrind DOC "valgrind program")

set(GENERATE_TESTS FALSE CACHE BOOL "Always build tests: add `make check_build` to normal `make` calls")
set(DISABLE_TESTS FALSE CACHE BOOL "Enable running `make check` during the building process")
set(VALGRIND_NUM_CALLERS 20 CACHE STRING "Valgrind option: maximum number of entries shown in stack traces")

function(create_check_target)
  if(NOT TARGET check_build)
    MESSAGE(STATUS "Adding custom target: 'check_build'")
    if(${GENERATE_TESTS})
      add_custom_target(check_build ALL)
    else()
      add_custom_target(check_build)
    endif()
  endif()
  if(NOT TARGET check)
    MESSAGE(STATUS "Adding custom target: 'check'")
    add_custom_target(check COMMAND ${CMAKE_CTEST_COMMAND} \${ARGS} WORKING_DIRECTORY ${CMAKE_BINARY_DIR})
    add_dependencies(check check_build)
  endif()
endfunction()


function(create_valgrind_target)
  if(NOT TARGET valgrind)
    MESSAGE(STATUS "Adding custom target: 'valgrind'")
    add_custom_target(valgrind)
  endif()
endfunction()


# This function simplifies tests creation.
#
# Don't forget to enable CMake testing, by calling enable_testing() in the
# source directory root.
#
# Input variables:
# - TEST_VARIABLES: A list of names and values.
#   Passed to the test as environment variables.
#
# If Valgrind is installed in the system, a 'valgrind' target will be created.
#
# Input variables:
# - VALGRIND_TEST_VARIABLES: A list of names and values.
#   Passed to Valgrind as environment variables.
# - SUPPRESSIONS: A list of suppression files for Valgrind.
# - VALGRIND_NUM_CALLERS: Maximum number of entries shown in stack traces
#   (option "--num-callers").
function(add_test_program test_name sources)
  message(STATUS "Adding test: ${test_name}")

  set(final_sources ${sources})
  foreach(arg ${ARGN})
    set(final_sources "${final_sources};${arg}")
  endforeach()
  add_executable(${test_name} EXCLUDE_FROM_ALL ${final_sources})
  if(SANITIZERS_ENABLED)
    add_sanitizers(${test_name})
  endif()

  create_check_target()
  add_dependencies(check_build ${test_name})

  if(NOT ${DISABLE_TESTS})
    add_test(${test_name} ${CMAKE_CURRENT_BINARY_DIR}/${test_name})

    foreach(p ${TEST_VARIABLES})
      set_property(TEST ${test_name} APPEND PROPERTY ENVIRONMENT ${p})
    endforeach()
  endif()

  add_custom_target(${test_name}.check
    COMMAND ${TEST_VARIABLES} ${CMAKE_CURRENT_BINARY_DIR}/${test_name} \${ARGS}
    DEPENDS ${test_name})

  if(VALGRIND_EXECUTABLE)
    set(SUPPS " ")
    foreach(SUPP ${SUPPRESSIONS})
      set(SUPPS "${SUPPS} --suppressions=${SUPP}")
    endforeach()
    STRING(REGEX REPLACE "^ " "" SUPPS ${SUPPS})

    add_custom_target(${test_name}.valgrind
      DEPENDS ${test_name})

    file(WRITE ${CMAKE_CURRENT_BINARY_DIR}/${test_name}_valgrind.cmake
"
execute_process(COMMAND ${VALGRIND_EXECUTABLE}
  --quiet
  ${SUPPS}
  --tool=memcheck --leak-check=full --trace-children=yes
  --leak-resolution=high --show-possibly-lost=yes
  --num-callers=${VALGRIND_NUM_CALLERS} --leak-check-heuristics=all
  ${CMAKE_CURRENT_BINARY_DIR}/${test_name} \${ARGS}
  RESULT_VARIABLE res
  OUTPUT_VARIABLE out
  ERROR_VARIABLE err
)

if(NOT \"\${out}\" STREQUAL \"\")
  message(\"stdout:\")
  message(\"\${out}\")
endif()
if(NOT \"\${err}\" STREQUAL \"\")
  message(\"stderr:\")
  message(\"\${err}\")
endif()

if(NOT \${res} EQUAL 0)
  message(FATAL_ERROR \"Test failed\")
endif()

string(REGEX MATCH \"^==\" valgrind_out \"\${err}\")

if(NOT \${valgrind_out} STREQUAL \"\")
  message(FATAL_ERROR \"There are valgrind errors on test\")
endif()
"
    )

    add_custom_command(TARGET ${test_name}.valgrind
      COMMENT "Running Valgrind for ${test_name}"
      COMMAND VALGRIND=TRUE G_DEBUG=gc-friendly G_SLICE=always-malloc ${TEST_VARIABLES} ${VALGRIND_TEST_VARIABLES} ${CMAKE_COMMAND} -DARGS=\${ARGS} -P ${CMAKE_CURRENT_BINARY_DIR}/${test_name}_valgrind.cmake
    )

    create_valgrind_target()
    add_dependencies(valgrind ${test_name}.valgrind)
  endif()
endfunction()
