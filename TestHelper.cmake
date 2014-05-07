
function(create_check_target)
  if (NOT TARGET check)
    MESSAGE (STATUS "Enabling check target")
    add_custom_target(check COMMAND ${CMAKE_CTEST_COMMAND})
  endif()
endfunction()

function(create_valgrind_target)
  if (NOT TARGET valgrind)
    MESSAGE (STATUS "Enabling valgrind target")
    add_custom_target(valgrind)
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

  add_custom_target (${test_name}.check
    COMMAND ${TEST_PROPERTIES} ${CMAKE_CURRENT_BINARY_DIR}/${test_name}
    DEPENDS ${test_name})

  if (EXISTS ${VALGRIND})
    foreach(SUP ${SUPPRESSIONS})
      set (SUPPS "${SUPPS} --suppressions=${SUP}")
    endforeach()

    add_custom_target (${test_name}.valgrind
      DEPENDS ${test_name})

    add_custom_command (TARGET ${test_name}.valgrind
      COMMENT "Running valgrind for ${test_name}"
      COMMAND ${TEST_PROPERTIES} CK_DEFAULT_TIMEOUT=360
        G_SLICE=always-malloc ${VALGRIND} -q
        $(SUPPS)
        --tool=memcheck --leak-check=yes --trace-children=yes
        --leak-resolution=high --show-possibly-lost=no
        --num-callers=20
        ${CMAKE_CURRENT_BINARY_DIR}/${test_name} 2>&1 |
          tee ${test_name}.valgrind.log
        )

     create_valgrind_target()
     add_dependencies(valgrind ${test_name}.valgrind)
  endif ()
endfunction()
