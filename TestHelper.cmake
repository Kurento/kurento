
function(create_check_target)
  if (NOT TARGET check)
    MESSAGE (STATUS "Enabling check target")
    add_custom_target(check COMMAND ${CMAKE_CTEST_COMMAND})
  endif()
endfunction()

## This function simplifies tests creation
## Name : add_test_program
## Params: test_name, extra_source (optional)
function(add_test_program test_name)
  message (STATUS "Adding tests: ${test_name}")

  set (extra_source ${ARGV1})

  add_executable (${test_name} ${test_name}.c ${extra_source})
  create_check_target()
  add_dependencies(check ${test_name})

  add_test (${test_name} ${CMAKE_CURRENT_BINARY_DIR}/${test_name})

  foreach (p ${TEST_PROPERTIES})
    set_property(TEST ${test_name} APPEND PROPERTY ENVIRONMENT ${p})
  endforeach()
endfunction()
