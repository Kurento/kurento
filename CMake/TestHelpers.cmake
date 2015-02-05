find_program(VALGRIND valgrind)

set (DISABLE_TESTS FALSE CACHE BOOL "Disable \"make check\" target")

function(create_check_target)
  if (NOT TARGET check)
    MESSAGE (STATUS "Enabling check target")
    add_custom_target(check COMMAND ${CMAKE_CTEST_COMMAND} \${ARGS} WORKING_DIRECTORY ${CMAKE_BINARY_DIR})
  endif()
endfunction()

function(create_valgrind_target)
  if (NOT TARGET valgrind)
    MESSAGE (STATUS "Enabling valgrind target")
    add_custom_target(valgrind)
  endif()
endfunction()

## This function simplifies tests creation
##
## If program TEST_PROPERTIES varible is defined, properties will be
## passed to the test as environment variables
##
## If valgrind is available a valgrind target will be created for the test
## To add suppression files to valgrind, just define SUPPRESSIONS variable
## with the files to add to valgrind as suppressions
##
## Do not forget to enable test on your top build directory calling enable_testing
##
## Name : add_test_program
## Params: test_name, sources
function(add_test_program test_name sources)
  message (STATUS "Adding tests: ${test_name}")

  set (final_sources ${sources})
  foreach (arg ${ARGN})
    set (final_sources "${final_sources};${arg}")
  endforeach()
  add_executable (${test_name} EXCLUDE_FROM_ALL ${final_sources})
  create_check_target()
  add_dependencies(check ${test_name})

  if (NOT ${DISABLE_TESTS})
    add_test (${test_name} ${CMAKE_CURRENT_BINARY_DIR}/${test_name})

    foreach (p ${TEST_PROPERTIES})
      set_property(TEST ${test_name} APPEND PROPERTY ENVIRONMENT ${p})
    endforeach()
  endif ()

  add_custom_target (${test_name}.check
    COMMAND ${TEST_PROPERTIES} ${CMAKE_CURRENT_BINARY_DIR}/${test_name} \${ARGS}
    DEPENDS ${test_name})

  if (EXISTS ${VALGRIND})
    set (SUPPS " ")
    foreach(SUP ${SUPPRESSIONS})
      set (SUPPS "${SUPPS} --suppressions=${SUP}")
    endforeach()
    STRING(REGEX REPLACE "^ " "" SUPPS ${SUPPS})

    add_custom_target (${test_name}.valgrind
      DEPENDS ${test_name})

    set (VALGRIND_NUM_CALLERS 20 CACHE STRING "Number of callers for valgrind")

    file(WRITE ${CMAKE_CURRENT_BINARY_DIR}/${test_name}_valgrind.cmake
"
execute_process(COMMAND valgrind -q
  ${SUPPS}
  --tool=memcheck --leak-check=full --trace-children=yes
  --leak-resolution=high --show-possibly-lost=yes
  --num-callers=${VALGRIND_NUM_CALLERS} --leak-check-heuristics=all
  ${CMAKE_CURRENT_BINARY_DIR}/${test_name} \${ARGS}
  RESULT_VARIABLE res
  OUTPUT_VARIABLE out
  ERROR_VARIABLE err
)

if (NOT \"\${out}\" STREQUAL \"\")
  message (\"Std out:\")
  message (\"\${out}\")
endif()
if (NOT \"\${err}\" STREQUAL \"\")
  message (\"Std err:\")
  message (\"\${err}\")
endif()

if (NOT \${res} EQUAL 0)
  message(FATAL_ERROR \"Test failed\")
endif()

string(REGEX MATCH \"^==\" valgrind_out \"\${err}\")

if (NOT \${valgrind_out} STREQUAL \"\")
  message(FATAL_ERROR \"There are valgrind errors on test\")
endif ()
"
    )

    add_custom_command (TARGET ${test_name}.valgrind
      COMMENT "Running valgrind for ${test_name}"
      COMMAND G_DEBUG=gc-friendly G_SLICE=always-malloc ${TEST_PROPERTIES} ${VALGRING_TEST_PROPERTIES} ${CMAKE_COMMAND} -DARGS=\${ARGS} -P ${CMAKE_CURRENT_BINARY_DIR}/${test_name}_valgrind.cmake
    )

    create_valgrind_target()
    add_dependencies(valgrind ${test_name}.valgrind)
  endif ()
endfunction()
