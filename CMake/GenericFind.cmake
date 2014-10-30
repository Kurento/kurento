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
    COMPONENTS
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

  if (${${GF_LIBNAME}_FOUND})
    set (SEARCH_AGAIN FALSE)
    if (DEFINED GF_VERSION AND NOT "${GF_LIBNAME}_REQ_VERSION" STREQUAL "${GF_VERSION}")
      check_version_internal (${GF_VERSION} ${${GF_LIBNAME}_VERSION} OUTPUT_ERROR)
      if (DEFINED OUTPUT_ERROR AND NOT ${OUTPUT_ERROR} EQUAL "")
          set (SEARCH_AGAIN TRUE)
      endif ()
    endif()

    if (DEFINED GF_COMPONENTS)
      foreach (COMP ${GF_COMPONENTS})
        list(FIND GEN_${GF_LIBNAME}_COMPONENTS ${COMP} COMP_FOUND)
        if (${COMP_FOUND} EQUAL -1)
          set (SEARCH_AGAIN TRUE)
        endif()
      endforeach()
    endif ()

    if (NOT SEARCH_AGAIN)
      message (STATUS "${GF_LIBNAME} Already found")
      return ()
    endif()
  endif ()

  if (DEFINED GF_COMPONENTS)
    foreach (COMP ${GF_COMPONENTS})
      list(FIND GEN_${GF_LIBNAME}_COMPONENTS ${COMP} COMP_FOUND)
      if (${COMP_FOUND} EQUAL -1)

        list (APPEND GEN_${GF_LIBNAME}_COMPONENTS ${COMP})
      endif()
    endforeach ()
    set (GEN_${GF_LIBNAME}_COMPONENTS ${GEN_${GF_LIBNAME}_COMPONENTS} CACHE INTERNAL "" FORCE)
    mark_as_advanced (GEN_${GF_LIBNAME}_COMPONENTS)

    if (DEFINED GF_REQUIRED)
      find_package(${GF_LIBNAME} REQUIRED COMPONENTS ${GEN_${GF_LIBNAME}_COMPONENTS})
    else ()
      find_package(${GF_LIBNAME} COMPONENTS ${GEN_${GF_LIBNAME}_COMPONENTS})
    endif ()
  else ()
    find_package(${GF_LIBNAME} QUIET)
  endif ()

  if (DEFINED ${GF_LIBNAME}_FOUND AND ${${GF_LIBNAME}_FOUND})
    if (NOT DEFINED GF_COMPONENTS)
      if (DEFINED GF_REQUIRED)
        find_package(${GF_LIBNAME})
      else()
        find_package(${GF_LIBNAME} REQUIRED)
      endif()
    endif()
  else()
    if (DEFINED GF_REQUIRED)
      pkg_check_modules(${GF_LIBNAME} ${GF_LIBNAME})
    else()
      pkg_check_modules(${GF_LIBNAME} ${GF_LIBNAME})
    endif()
  endif()

  if (DEFINED GF_VERSION)
    message (STATUS "Resolving ${GF_LIBNAME} version ${GF_VERSION} with ${${GF_LIBNAME}_VERSION}")
    check_version(${GF_VERSION} ${${GF_LIBNAME}_VERSION})
  elseif (DEFINED GF_COMPONENTS)
    message (STATUS "Found ${GF_LIBNAME} COMPONENTS ${GEB_${GF_LIBNAME}_COMPONENTS}")
  else ()
    message (STATUS "Found ${GF_LIBNAME}")
  endif ()

  set (${GF_LIBNAME}_VERSION ${${GF_LIBNAME}_VERSION} CACHE PATH "${GF_LIBNAME} Version")

  set (${GF_LIBNAME}_LIBRARIES ${${GF_LIBNAME}_LIBRARIES} PARENT_SCOPE)
  set (${GF_LIBNAME}_LIBRARIES ${${GF_LIBNAME}_LIBRARIES} CACHE PATH "${GF_LIBNAME}_LIBRARIES")

  set (${GF_LIBNAME}_INCLUDE_DIRS ${${GF_LIBNAME}_INCLUDE_DIRS} PARENT_SCOPE)
  set (${GF_LIBNAME}_INCLUDE_DIRS ${${GF_LIBNAME}_INCLUDE_DIRS} CACHE PATH "${GF_LIBNAME}_INCLUDE_DIRS")

  set (${GF_LIBNAME}_FOUND TRUE CACHE BOOL "${GF_LIBNAME}_FOUND")
  mark_as_advanced (${GF_LIBNAME}_FOUND)
  set (${GF_LIBNAME}_FOUND TRUE)


  if (DEFINED GF_VERSION)
    set (${GF_LIBNAME}_REQ_VERSION ${GF_VERSION} CACHE STRING "${GF_LIBNAME} requested version")
    mark_as_advanced (${GF_LIBNAME}_REQ_VERSION)
  endif()
endfunction()
