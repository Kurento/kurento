Find${module.code.implementation.lib?replace("lib", "")?upper_case}.cmake.in
<#assign name>${module.code.implementation.lib?replace("lib", "")?upper_case}</#assign>
# - Try to find ${name} library

#=============================================================================
# Copyright 2014 Kurento
#
#=============================================================================

set(PACKAGE_VERSION "@PROJECT_VERSION@")
set(${name}_VERSION <#noparse>${PACKAGE_VERSION}</#noparse>)

include (GenericFind)
<#list module.imports as import>

generic_find (
  REQUIRED
  LIBNAME ${import.module.code.implementation.lib?replace("lib", "")?upper_case}
  VERSION ${import.version}
)
</#list>

find_path(${name}_INTERFACE_INCLUDE_DIR
  NAMES
    @_INTERFACE_GENERATED_HEADERS@
    @_INTERFACE_INTERNAL_GENERATED_HEADERS@
  PATH_SUFFIXES
    @_INTERFACE_HEADERS_DIR@
    kurento/modules/${module.name}
)

find_path(${name}_IMPLEMENTTION_INTERNAL_INCLUDE_DIR
  NAMES
    @_SERVER_INTERNAL_GENERATED_HEADERS@
  PATH_SUFFIXES
    @_SERVER_INTERNAL_GENERATED_HEADERS_DIR@
    kurento/modules/${module.name}
)

find_path(${name}_IMPLEMENTTION_GENERATED_INCLUDE_DIR
  NAMES
    @_SERVER_GENERATED_HEADERS@
  PATH_SUFFIXES
    @_PARAM_SERVER_STUB_DESTINATION@
    kurento/modules/${module.name}
)

find_path(${name}_IMPLEMENTTION_EXTRA_INCLUDE_DIR
  NAMES
    @_PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS@
  PATH_SUFFIXES
    @_PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS_PREFIX@
    kurento/modules/${module.name}
)

find_library (${name}_LIBRARY
  NAMES
    ${module.code.implementation.lib?replace("lib", "")}impl
  PATH_SUFFIXES
    build/src/server
)

<#noparse>
set (REQUIRED_LIBS "@REQUIRED_LIBS@")
foreach (LIB ${REQUIRED_LIBS})
  string(FIND ${LIB} " " POS)

  if (${POS} GREATER 0)
    string (SUBSTRING ${LIB} 0 ${POS} LIB_NAME)
    string (SUBSTRING ${LIB} ${POS} -1 LIB_VERSION)
    string (STRIP ${LIB_NAME} LIB_NAME)
    string (STRIP ${LIB_VERSION} LIB_VERSION)
    generic_find (LIBNAME ${LIB_NAME} REQUIRED VERSION "${LIB_VERSION}")
  else ()
    generic_find (LIBNAME ${LIB_NAME} REQUIRED)
  endif ()
  list (APPEND REQUIRED_LIBRARIES ${${LIB_NAME}_LIBRARIES})
  list (APPEND REQUIRED_INCLUDE_DIRS ${${LIB_NAME}_INCLUDE_DIRS})

endforeach()
</#noparse>

set(${name}_INCLUDE_DIRS
  <#noparse>${</#noparse>${name}<#noparse>_INTERFACE_INCLUDE_DIR}</#noparse>
  <#noparse>${</#noparse>${name}<#noparse>_IMPLEMENTTION_INTERNAL_INCLUDE_DIR}</#noparse>
  <#noparse>${</#noparse>${name}<#noparse>_IMPLEMENTTION_GENERATED_INCLUDE_DIR}</#noparse>
  <#noparse>${</#noparse>${name}<#noparse>_IMPLEMENTTION_EXTRA_INCLUDE_DIR}</#noparse>
<#list module.imports as import>
  <#noparse>${</#noparse>${import.module.code.implementation.lib?replace("lib", "")?upper_case}<#noparse>_INCLUDE_DIRS}</#noparse>
</#list>
  <#noparse>${REQUIRED_INCLUDE_DIRS}</#noparse>
  CACHE INTERNAL "Include directories for ${name} library"
)

set (${name}_LIBRARIES
  <#noparse>${</#noparse>${name}<#noparse>_LIBRARY}</#noparse>
<#list module.imports as import>
  <#noparse>${</#noparse>${import.module.code.implementation.lib?replace("lib", "")?upper_case}<#noparse>_LIBRARIES}</#noparse>
</#list>
  <#noparse>${REQUIRED_LIBRARIES}</#noparse>
  CACHE INTERNAL "Libraries for ${name}"
)

include (FindPackageHandleStandardArgs)

find_package_handle_standard_args(${name}
  FOUND_VAR
    ${name}_FOUND
  REQUIRED_VARS
    ${name}_VERSION
    ${name}_INTERFACE_INCLUDE_DIR
    ${name}_IMPLEMENTTION_INTERNAL_INCLUDE_DIR
    ${name}_IMPLEMENTTION_GENERATED_INCLUDE_DIR
    ${name}_IMPLEMENTTION_EXTRA_INCLUDE_DIR
    ${name}_INCLUDE_DIRS
    ${name}_LIBRARY
    ${name}_LIBRARIES
  VERSION_VAR
    ${name}_VERSION
)

mark_as_advanced(
  ${name}_FOUND
  ${name}_VERSION
  ${name}_INTERFACE_INCLUDE_DIR
  ${name}_IMPLEMENTTION_INTERNAL_INCLUDE_DIR
  ${name}_IMPLEMENTTION_GENERATED_INCLUDE_DIR
  ${name}_IMPLEMENTTION_EXTRA_INCLUDE_DIR
  ${name}_INCLUDE_DIRS
  ${name}_LIBRARY
  ${name}_LIBRARIES
)
