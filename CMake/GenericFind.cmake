include (CMakeParseArguments)
include (VersionHelpers)
find_package(PkgConfig)

function(generic_find)
  set (OPTION_PARAMS
    REQUIRED
  )

  set (ONE_VALUE_PARAMS
    LIBNAME
    VERSION
  )
  set (MULTI_VALUE_PARAMS
  )

  set (REQUIRED_PARAMS
    LIBNAME
  )

  cmake_parse_arguments("GF" "${OPTION_PARAMS}" "${ONE_VALUE_PARAMS}" "${MULTI_VALUE_PARAMS}" ${ARGN})

  foreach (REQUIRED_PARAM ${REQUIRED_PARAMS})
    if (NOT DEFINED GF_${REQUIRED_PARAM})
      message (FATAL_ERROR "Required param ${REQUIRED_PARAM} is not set")
    endif()
  endforeach()

  find_package(${GF_LIBNAME} QUIET)
  if (DEFINED ${GF_LIBNAME}_FOUND AND ${${GF_LIBNAME}_FOUND})
    find_package(${GF_LIBNAME})
  else()
    pkg_check_modules(${GF_LIBNAME} ${GF_LIBNAME})
  endif()

  if (NOT DEFINED ${GF_LIBNAME}_FOUND OR NOT ${${GF_LIBNAME}_FOUND} OR ${${GF_LIBNAME}_FOUND} EQUAL "")
    if (DEFINED GF_REQUIRED)
      message (FATAL_ERROR "${GF_LIBNAME} Not Found")
    else ()
      message (WARNING "${GF_LIBNAME} Not Found")
    endif ()
  endif()

  if (DEFINED GF_VERSION)
    message (STATUS "Resolving ${GF_LIBNAME} version ${GF_VERSION} with ${${GF_LIBNAME}_VERSION}")
    check_version(${GF_VERSION} ${${GF_LIBNAME}_VERSION})
  else ()
    message (STATUS "Found ${GF_LIBNAME}")
  endif ()
endfunction()
