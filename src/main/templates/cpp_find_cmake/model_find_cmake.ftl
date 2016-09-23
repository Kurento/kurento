Find${module.code.implementation.lib?replace("lib", "")?upper_case}.cmake.in
<#assign name_lower>${module.code.implementation.lib?replace("lib", "")}</#assign>
<#assign name>${name_lower?upper_case}</#assign>
# - Try to find ${name} library

#=============================================================================
# Copyright 2014 Kurento
#
#=============================================================================

set(PACKAGE_VERSION "@PROJECT_VERSION@")
set(${name}_VERSION <#noparse>${PACKAGE_VERSION}</#noparse>)

message (STATUS "Looking for ${name}: @PROJECT_VERSION@")

include (GenericFind)
<#list module.imports as import>

generic_find (
  REQUIRED
  LIBNAME ${import.module.code.implementation.lib?replace("lib", "")?upper_case}
  VERSION ${import.version}
)
</#list>

set (REQUIRED_VARS
  ${name}_VERSION
  ${name}_INCLUDE_DIRS
  ${name}_LIBRARY
  ${name}_LIBRARIES
)

set (${name}_BINARY_DIR_PREFIX "build" CACHE PATH "Path prefix used to look for binary files")
set (${name}_SOURCE_DIR_PREFIX "" CACHE PATH "Path prefix used to look for source files")

set(${name}_INCLUDE_DIRS
<#list module.imports as import>
  <#noparse>${</#noparse>${import.module.code.implementation.lib?replace("lib", "")?upper_case}<#noparse>_INCLUDE_DIRS}</#noparse>
</#list>
)

if (NOT "@_INTERFACE_GENERATED_HEADERS@ @_INTERFACE_INTERNAL_GENERATED_HEADERS@" STREQUAL " ")
  if (TARGET ${name_lower}interface)
    set (${name}_INTERFACE_INCLUDE_DIR "@_INTERFACE_HEADERS_DIR@")
  else ()
    find_path(${name}_INTERFACE_INCLUDE_DIR
      NAMES
        @_INTERFACE_GENERATED_HEADERS@
        @_INTERFACE_INTERNAL_GENERATED_HEADERS@
      PATH_SUFFIXES
        @_INTERFACE_HEADERS_DIR@
        kurento/modules/${module.name}
    )
  endif ()

  list (APPEND ${name}_INCLUDE_DIRS <#noparse>${</#noparse>${name}<#noparse>_INTERFACE_INCLUDE_DIR}</#noparse>)
  list (APPEND REQUIRED_VARS ${name}_INTERFACE_INCLUDE_DIR)
endif ()

if (NOT "@_SERVER_INTERNAL_GENERATED_HEADERS@" STREQUAL "")
  if (TARGET ${name_lower}impl)
    set (${name}_IMPLEMENTATION_INTERNAL_INCLUDE_DIR "@_SERVER_INTERNAL_GENERATED_HEADERS_DIR@")
  else ()
    find_path(${name}_IMPLEMENTATION_INTERNAL_INCLUDE_DIR
      NAMES
        @_SERVER_INTERNAL_GENERATED_HEADERS@
      PATH_SUFFIXES
        @_SERVER_INTERNAL_GENERATED_HEADERS_DIR@
        kurento/modules/${module.name}
    )
  endif ()

  list (APPEND ${name}_INCLUDE_DIRS <#noparse>${</#noparse>${name}<#noparse>_IMPLEMENTATION_INTERNAL_INCLUDE_DIR}</#noparse>)
  list (APPEND REQUIRED_VARS ${name}_IMPLEMENTATION_INTERNAL_INCLUDE_DIR)
endif ()

if (NOT "@_SERVER_GENERATED_HEADERS@" STREQUAL "")
  if (TARGET ${name_lower}impl)
    set (${name}_IMPLEMENTATION_GENERATED_INCLUDE_DIR "<#noparse>$</#noparse>{${name}_SOURCE_DIR_PREFIX}/@_PARAM_SERVER_STUB_DESTINATION@")
  else ()
    find_path(${name}_IMPLEMENTATION_GENERATED_INCLUDE_DIR
      NAMES
        @_SERVER_GENERATED_HEADERS@
      PATH_SUFFIXES
        @_PARAM_SERVER_STUB_DESTINATION@
        kurento/modules/${module.name}
    )
  endif ()

  list (APPEND ${name}_INCLUDE_DIRS <#noparse>${</#noparse>${name}<#noparse>_IMPLEMENTATION_GENERATED_INCLUDE_DIR}</#noparse>)
  list (APPEND REQUIRED_VARS ${name}_IMPLEMENTATION_GENERATED_INCLUDE_DIR)
endif()

if (NOT "@_PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS@" STREQUAL "")
  if (TARGET ${name_lower}impl)
    set (${name}_IMPLEMENTATION_EXTRA_INCLUDE_DIR "<#noparse>$</#noparse>{${name}_SOURCE_DIR_PREFIX}/@_PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS_PREFIX@")
  else ()
    find_path(${name}_IMPLEMENTATION_EXTRA_INCLUDE_DIR
      NAMES
        @_PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS@
      PATH_SUFFIXES
        @_PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS_PREFIX@
        kurento/modules/${module.name}
    )
  endif ()

  list (APPEND ${name}_INCLUDE_DIRS <#noparse>${</#noparse>${name}<#noparse>_IMPLEMENTATION_EXTRA_INCLUDE_DIR}</#noparse>)
  list (APPEND REQUIRED_VARS ${name}_IMPLEMENTATION_EXTRA_INCLUDE_DIR)
endif ()

if (NOT "@_PARAM_INTERFACE_LIB_EXTRA_HEADERS@" STREQUAL "")
  if (TARGET ${name_lower}interface)
    set (${name}_INTERFACE_EXTRA_INCLUDE_DIR "<#noparse>$</#noparse>{${name}_SOURCE_DIR_PREFIX}/@_PARAM_INTERFACE_LIB_EXTRA_HEADERS_PREFIX@")
  else ()
    find_path(${name}_INTERFACE_EXTRA_INCLUDE_DIR
      NAMES
        @_PARAM_INTERFACE_LIB_EXTRA_HEADERS@
      PATH_SUFFIXES
        @_PARAM_INTERFACE_LIB_EXTRA_HEADERS_PREFIX@
        kurento/modules/${module.name}
    )
  endif()

  list (APPEND ${name}_INCLUDE_DIRS <#noparse>${</#noparse>${name}<#noparse>_INTERFACE_EXTRA_INCLUDE_DIR}</#noparse>)
  list (APPEND REQUIRED_VARS ${name}_INTERFACE_EXTRA_INCLUDE_DIR)
endif ()

if (TARGET ${name_lower}impl)
  set (${name}_LIBRARY ${name_lower}impl)
else ()
  find_library (${name}_LIBRARY
    NAMES
      ${name_lower}impl
    PATH_SUFFIXES
      <#noparse>${</#noparse>${name}_BINARY_DIR_PREFIX<#noparse>}</#noparse>/src/server
  )
endif()

<#noparse>
set (REQUIRED_LIBS "@REQUIRED_LIBS@")
foreach (LIB ${REQUIRED_LIBS})
  string(FIND ${LIB} " " POS)

  if (${POS} GREATER 0)
    string(REPLACE " " ";" REQUIRED_LIB_LIST ${LIB})
    include (CMakeParseArguments)
    cmake_parse_arguments("PARAM" "" "LIBNAME" "" ${REQUIRED_LIB_LIST})

    if (DEFINED PARAM_LIBNAME)
      generic_find (${REQUIRED_LIB_LIST} REQUIRED)
      set (LIB_NAME ${PARAM_LIBNAME})
    else()
      string (SUBSTRING ${LIB} 0 ${POS} LIB_NAME)
      string (SUBSTRING ${LIB} ${POS} -1 LIB_VERSION)
      string (STRIP ${LIB_NAME} LIB_NAME)
      string (STRIP ${LIB_VERSION} LIB_VERSION)
      generic_find (LIBNAME ${LIB_NAME} REQUIRED VERSION "${LIB_VERSION}")
    endif()
  else ()
    string (STRIP ${LIB} LIB_NAME)
    generic_find (LIBNAME ${LIB_NAME} REQUIRED)
  endif ()
  list (APPEND REQUIRED_LIBRARIES ${${LIB_NAME}_LIBRARIES})
  list (APPEND </#noparse>${name}<#noparse>_INCLUDE_DIRS ${${LIB_NAME}_INCLUDE_DIRS})

endforeach()
</#noparse>

set(${name}_INCLUDE_DIRS
  <#noparse>${</#noparse>${name}<#noparse>_INCLUDE_DIRS}</#noparse>
  CACHE INTERNAL "Include directories for ${name} library" FORCE
)

set (${name}_LIBRARIES
  <#noparse>${</#noparse>${name}<#noparse>_LIBRARY}</#noparse>
<#list module.imports as import>
  <#noparse>${</#noparse>${import.module.code.implementation.lib?replace("lib", "")?upper_case}<#noparse>_LIBRARIES}</#noparse>
</#list>
  <#noparse>${REQUIRED_LIBRARIES}</#noparse>
  CACHE INTERNAL "Libraries for ${name}" FORCE
)

include (FindPackageHandleStandardArgs)

find_package_handle_standard_args(${name}
  FOUND_VAR
    ${name}_FOUND
  REQUIRED_VARS
    <#noparse>${REQUIRED_VARS}</#noparse>
  VERSION_VAR
    ${name}_VERSION
)

mark_as_advanced(
  ${name}_FOUND
  ${name}_VERSION
  ${name}_INTERFACE_INCLUDE_DIR
  ${name}_IMPLEMENTATION_INTERNAL_INCLUDE_DIR
  ${name}_IMPLEMENTATION_GENERATED_INCLUDE_DIR
  ${name}_IMPLEMENTATION_EXTRA_INCLUDE_DIR
  ${name}_INTERFACE_EXTRA_INCLUDE_DIR
  ${name}_INCLUDE_DIRS
  ${name}_LIBRARY
  ${name}_LIBRARIES
)
