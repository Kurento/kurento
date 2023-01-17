cmake_minimum_required(VERSION 3.0)

# Call hierarchy.
# Top level (called from Kurento modules, like kurento-module-core)
# |-- generate_code
# |   |-- generate_kurento_libraries
# |   |   |-- generate_sources
# |   |       |-- execute_code_generator
# |   |-- get_values_from_model
# |-- get_values_from_model
#     |-- execute_code_generator

include(GNUInstallDirs) # CMAKE_INSTALL_*
include (KurentoGitHelpers)

include (GenericFind)
generic_find(LIBNAME KurentoModuleCreator VERSION ^7.0.0 REQUIRED)

set (GENERATE_JAVA_CLIENT_PROJECT FALSE CACHE BOOL "Generate java maven client library")
set (GENERATE_JS_CLIENT_PROJECT FALSE CACHE BOOL "Generate js npm client library")
set (DISABLE_LIBRARIES_GENERATION FALSE CACHE BOOL "Disable C/C++ libraries generation, just useful for generating client code")

set (ENABLE_CODE_GENERATION_FORMAT_CHECK FALSE CACHE BOOL "Check if coding style of generated code is correct")
mark_as_advanced(ENABLE_CODE_GENERATION_FORMAT_CHECK)

set (KURENTO_MODULES_DIR /usr/share/kurento/modules CACHE PATH "Directory where kurento module descriptors can be found")
mark_as_advanced(KURENTO_MODULES_DIR)

set (KURENTO_MODULES_DIR_INSTALL_PREFIX kurento/modules CACHE PATH "Directory where kurento module descriptors are installed (relative to \${CMAKE_INSTALL_PREFIX}/${CMAKE_INSTALL_DATAROOTDIR}). Also .so module files are installed using this prefix, but relative to \${CMAKE_INSTALL_PREFIX}/${CMAKE_INSTALL_LIBDIR})")
mark_as_advanced(KURENTO_MODULES_DIR_INSTALL_PREFIX)

set (KURENTO_CLIENT_JS_GIT https://github.com/Kurento/kurento.git CACHE STRING "URL of kurento-client-js git repository to get templates from")
set (KURENTO_CLIENT_JS_BRANCH main CACHE STRING "Branch of kurento-client-js repository to get templates from")

set (MODULE_EVENTS "")
set (MODULE_REMOTE_CLASSES "")
set (MODULE_COMPLEX_TYPES "")
set (MODULE_DIGEST "")
set (MODULE_NAME "")

set (EVENTS_PREFIX "Event:")
set (REMOTE_CLASSES_PREFIX "RemoteClass:")
set (COMPLEX_TYPES_PREFIX "ComplexType:")
set (DIGEST_PREFIX "Digest:")

include (CMakeParseArguments)

set (PROCESSED_PREFIX "Processed file:\t")

function (execute_code_generator)
  set (OPTION_PARAMS
  )

  set (ONE_VALUE_PARAMS
    OUTPUT_VARIABLE
  )

  set (MULTI_VALUE_PARAMS
    EXEC_PARAMS
  )

  set (REQUIRED_PARAMS
    EXEC_PARAMS
  )

  cmake_parse_arguments("PARAM" "${OPTION_PARAMS}" "${ONE_VALUE_PARAMS}" "${MULTI_VALUE_PARAMS}" ${ARGN})

  foreach (REQUIRED_PARAM ${REQUIRED_PARAMS})
    if (NOT DEFINED PARAM_${REQUIRED_PARAM})
      message (FATAL_ERROR "Required param ${REQUIRED_PARAM} is not set")
    endif()
  endforeach()

  message ("Run command: '${KurentoModuleCreator_EXECUTABLE} ${PARAM_EXEC_PARAMS}'")

  execute_process(
    COMMAND ${KurentoModuleCreator_EXECUTABLE} ${PARAM_EXEC_PARAMS}
    OUTPUT_VARIABLE PROCESSOR_OUTPUT
    ERROR_VARIABLE PROCESSOR_ERROR
    RESULT_VARIABLE PROCESSOR_RET
  )

  if(PROCESSOR_RET)
    message(
      FATAL_ERROR
      "Code generator failed!\n"
      "**Command stdout**: ${PROCESSOR_OUTPUT}\n"
      "**Command stderr**: ${PROCESSOR_ERROR}"
    )
  endif()

  if (DEFINED PARAM_OUTPUT_VARIABLE)
    set (${PARAM_OUTPUT_VARIABLE} ${PROCESSOR_OUTPUT} PARENT_SCOPE)
  endif()
endfunction()

# generate_sources (
#  MODELS list of models or directories
#  GEN_FILES_DIR directory to generate files
#  SOURCE_FILES_OUTPUT variable to output generated sources
#  HEADER_FILES_OUTPUT variable to output generated headers
#  [INTERNAL_TEMPLATES_DIR] internal templates directory,
#                       only used if TEMPLATES_DIR is not set
#
#)
function (generate_sources)

  set (OPTION_PARAMS
    NO_OVERWRITE
  )

  set (ONE_VALUE_PARAMS
    GEN_FILES_DIR
    SOURCE_FILES_OUTPUT
    HEADER_FILES_OUTPUT
    INTERNAL_TEMPLATES_DIR
  )
  set (MULTI_VALUE_PARAMS
    MODELS
  )

  set (REQUIRED_PARAMS
    MODELS
    GEN_FILES_DIR
    SOURCE_FILES_OUTPUT
    HEADER_FILES_OUTPUT
    INTERNAL_TEMPLATES_DIR
  )

  cmake_parse_arguments("PARAM" "${OPTION_PARAMS}" "${ONE_VALUE_PARAMS}" "${MULTI_VALUE_PARAMS}" ${ARGN})

  foreach (REQUIRED_PARAM ${REQUIRED_PARAMS})
    if (NOT DEFINED PARAM_${REQUIRED_PARAM})
      message (FATAL_ERROR "Required param ${REQUIRED_PARAM} is not set")
    endif()
  endforeach()

  foreach (MODULES_DIR ${KURENTO_MODULES_DIR})
    set (KURENTO_MODULES_DIR_LINE "${KURENTO_MODULES_DIR_LINE};-dr;${MODULES_DIR}")
  endforeach()

  set (COMMAND_LINE -c ${PARAM_GEN_FILES_DIR} -r ${PARAM_MODELS} ${KURENTO_MODULES_DIR_LINE} -it ${PARAM_INTERNAL_TEMPLATES_DIR})

  if (PARAM_NO_OVERWRITE)
    set (COMMAND_LINE ${COMMAND_LINE} -n)
  endif()

  set (MODEL_FILES "")
  foreach (MODEL ${PARAM_MODELS})
    if (IS_DIRECTORY ${MODEL})
      file (GLOB_RECURSE MODELS ${MODEL}/*kmd.json)
      list (APPEND MODEL_FILES ${MODELS})
    elseif (EXISTS ${MODEL} AND ${MODEL} MATCHES ".*kmd.json")
      list (APPEND MODEL_FILES ${MODEL})
    endif()
  endforeach()

  if ("cpp_interface" STREQUAL ${PARAM_INTERNAL_TEMPLATES_DIR})
    foreach (REMOTE_CLASS ${MODULE_REMOTE_CLASSES})
      list (APPEND GENERATED_SOURCE_FILES ${PARAM_GEN_FILES_DIR}/${REMOTE_CLASS}.cpp)
      list (APPEND GENERATED_HEADER_FILES ${PARAM_GEN_FILES_DIR}/${REMOTE_CLASS}.hpp)
    endforeach()
    foreach (EVENT ${MODULE_EVENTS})
      list (APPEND GENERATED_SOURCE_FILES ${PARAM_GEN_FILES_DIR}/${EVENT}.cpp)
      list (APPEND GENERATED_HEADER_FILES ${PARAM_GEN_FILES_DIR}/${EVENT}.hpp)
    endforeach()
    foreach (COMPLEX_TYPE ${MODULE_COMPLEX_TYPES})
      list (APPEND GENERATED_SOURCE_FILES ${PARAM_GEN_FILES_DIR}/${COMPLEX_TYPE}.cpp)
      list (APPEND GENERATED_HEADER_FILES ${PARAM_GEN_FILES_DIR}/${COMPLEX_TYPE}.hpp)
    endforeach()
  elseif ("cpp_interface_internal" STREQUAL ${PARAM_INTERNAL_TEMPLATES_DIR})
    foreach (REMOTE_CLASS ${MODULE_REMOTE_CLASSES})
      list (APPEND GENERATED_SOURCE_FILES ${PARAM_GEN_FILES_DIR}/${REMOTE_CLASS}Internal.cpp)
      list (APPEND GENERATED_HEADER_FILES ${PARAM_GEN_FILES_DIR}/${REMOTE_CLASS}Internal.hpp)
    endforeach()
  elseif ("cpp_server_internal" STREQUAL ${PARAM_INTERNAL_TEMPLATES_DIR})
    #Module name to camel case
    string(SUBSTRING ${MODULE_NAME} 0 1 FIRST_LETTER)
    string(TOUPPER ${FIRST_LETTER} FIRST_LETTER)
    string(REGEX REPLACE "^.(.*)" "${FIRST_LETTER}\\1" CAMEL_CASE_MODULE_NAME "${MODULE_NAME}")

    list (APPEND GENERATED_SOURCE_FILES ${PARAM_GEN_FILES_DIR}/SerializerExpander${CAMEL_CASE_MODULE_NAME}.cpp)

    foreach (REMOTE_CLASS ${MODULE_REMOTE_CLASSES})
      list (APPEND GENERATED_SOURCE_FILES ${PARAM_GEN_FILES_DIR}/${REMOTE_CLASS}ImplInternal.cpp)
      list (APPEND GENERATED_HEADER_FILES ${PARAM_GEN_FILES_DIR}/${REMOTE_CLASS}ImplFactory.hpp)
    endforeach()
  elseif ("cpp_server" STREQUAL ${PARAM_INTERNAL_TEMPLATES_DIR})
    # Generated directly
  elseif ("cpp_module" STREQUAL ${PARAM_INTERNAL_TEMPLATES_DIR})
    list (APPEND GENERATED_SOURCE_FILES ${PARAM_GEN_FILES_DIR}/Module.cpp)
  else ()
    message (FATAL_ERROR "Templates ${PARAM_INTERNAL_TEMPLATES_DIR} not managed")
  endif()

  if ("cpp_server" STREQUAL ${PARAM_INTERNAL_TEMPLATES_DIR})
    if (NOT DEFINED MODEL_FILES)
      message (FATAL_ERROR "No model files found")
    endif ()

    execute_code_generator (
      OUTPUT_VARIABLE PROCESSOR_OUTPUT
      EXEC_PARAMS ${COMMAND_LINE} -lf
    )

    if ("${PROCESSOR_OUTPUT}" STREQUAL "")
      message (FATAL_ERROR "No code generated")
    else()
      string(REPLACE "\n" ";" PROCESSOR_OUTPUT ${PROCESSOR_OUTPUT})
    endif()

    foreach (_FILE ${PROCESSOR_OUTPUT})
      if (${_FILE} MATCHES "${PROCESSED_PREFIX}.*")
        string(REPLACE ${PROCESSED_PREFIX} "" _FILE ${_FILE})
        string(REGEX REPLACE "\t.*" "" _FILE ${_FILE})
        if (${_FILE} MATCHES ".*cpp")
          list (APPEND GENERATED_SOURCE_FILES ${PARAM_GEN_FILES_DIR}/${_FILE})
          message (STATUS "Generated: ${_FILE}")
        elseif(${_FILE} MATCHES ".*hpp")
          list (APPEND GENERATED_HEADER_FILES ${PARAM_GEN_FILES_DIR}/${_FILE})
          message (STATUS "Generated: ${_FILE}")
        endif ()
        if (ENABLE_CODE_GENERATION_FORMAT_CHECK)
          if (EXISTS ${PARAM_GEN_FILES_DIR}/${_FILE}.orig)
            file (REMOVE ${PARAM_GEN_FILES_DIR}/${_FILE}.orig)
          endif()
          execute_process (
            COMMAND astyle
            --style=linux
            --indent=spaces=2
            --indent-preprocessor
            --min-conditional-indent=2
            --break-blocks
            --pad-oper
            --pad-paren-out
            --convert-tabs
            --align-pointer=name
            --lineend=linux
            --break-blocks
            -q
            ${PARAM_GEN_FILES_DIR}/${_FILE}
          )
          if (EXISTS ${PARAM_GEN_FILES_DIR}/${_FILE}.orig)
            message (WARNING "Style incorrect for file: ${PARAM_GEN_FILES_DIR}/${_FILE}")
          endif()
        endif()
      endif()
    endforeach()
  else ()

    if (TARGET kurento-module-creator)
      add_custom_command(
        OUTPUT  ${PARAM_INTERNAL_TEMPLATES_DIR}.generated ${GENERATED_SOURCE_FILES} ${GENERATED_HEADER_FILES}
        COMMAND ${CMAKE_COMMAND} -E touch ${PARAM_INTERNAL_TEMPLATES_DIR}.generated
        COMMAND ${KurentoModuleCreator_EXECUTABLE} ${COMMAND_LINE}
        DEPENDS ${MODEL_FILES} kurento-module-creator ${KurentoModuleCreator_EXECUTABLE}
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
      )
    else()
      add_custom_command(
        OUTPUT  ${PARAM_INTERNAL_TEMPLATES_DIR}.generated ${GENERATED_SOURCE_FILES} ${GENERATED_HEADER_FILES}
        COMMAND ${CMAKE_COMMAND} -E touch ${PARAM_INTERNAL_TEMPLATES_DIR}.generated
        COMMAND ${KurentoModuleCreator_EXECUTABLE} ${COMMAND_LINE}
        DEPENDS ${MODEL_FILES}
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
      )
    endif()
  endif()

  set (${PARAM_SOURCE_FILES_OUTPUT} ${${PARAM_SOURCE_FILES_OUTPUT}} ${GENERATED_SOURCE_FILES} PARENT_SCOPE)
  set (${PARAM_HEADER_FILES_OUTPUT} ${${PARAM_HEADER_FILES_OUTPUT}} ${GENERATED_HEADER_FILES} PARENT_SCOPE)
endfunction()

function (generate_code)
  set (OPTION_PARAMS
  )

  set (ONE_VALUE_PARAMS
  )
  set (MULTI_VALUE_PARAMS
    MODELS
  )

  set (REQUIRED_PARAMS
    MODELS
  )

  cmake_parse_arguments("PARAM" "${OPTION_PARAMS}" "${ONE_VALUE_PARAMS}" "${MULTI_VALUE_PARAMS}" ${ARGN})

  foreach (REQUIRED_PARAM ${REQUIRED_PARAMS})
    if (NOT DEFINED PARAM_${REQUIRED_PARAM})
      message (FATAL_ERROR "Required param ${REQUIRED_PARAM} is not set")
    endif()
  endforeach()

  foreach (MODEL ${PARAM_MODELS})
    if (IS_DIRECTORY ${MODEL})
      file (GLOB_RECURSE MODELS ${MODEL}/*kmd.json)
      list (APPEND MODEL_FILES ${MODELS})
    elseif (EXISTS ${MODEL})
      list (APPEND MODEL_FILES ${MODEL})
    endif()
  endforeach()

  if (NOT DEFINED MODEL_FILES)
    message (FATAL_ERROR "No model files found")
  endif ()

  generate_kurento_libraries (${ARGN})

endfunction()

function (generate_kurento_libraries)
  set (OPTION_PARAMS
  )

  set (ONE_VALUE_PARAMS
    SERVER_STUB_DESTINATION

    # Extra requirements added to auto-generated pkg-config files.
    # Final users of the module will need to have those dependencies installed.
    SERVER_IMPL_LIB_PKGCONFIG_EXTRA_REQUIRES
  )

  set (MULTI_VALUE_PARAMS
    MODELS
    INTERFACE_LIB_EXTRA_SOURCES
    INTERFACE_LIB_EXTRA_HEADERS
    INTERFACE_LIB_EXTRA_INCLUDE_DIRS
    INTERFACE_LIB_EXTRA_LIBRARIES
    SERVER_IMPL_LIB_EXTRA_SOURCES
    SERVER_IMPL_LIB_EXTRA_HEADERS
    SERVER_IMPL_LIB_EXTRA_INCLUDE_DIRS
    SERVER_IMPL_LIB_EXTRA_LIBRARIES
    MODULE_EXTRA_INCLUDE_DIRS
    MODULE_EXTRA_LIBRARIES
    MODULE_CONFIG_FILES_DIRS

    # Extra requirements added to auto-generated CMake files.
    # Final users of the module will need to have those dependencies installed.
    SERVER_IMPL_LIB_FIND_CMAKE_EXTRA_LIBRARIES
  )

  set (REQUIRED_PARAMS
    MODELS
    SERVER_STUB_DESTINATION
  )

  cmake_parse_arguments("PARAM" "${OPTION_PARAMS}" "${ONE_VALUE_PARAMS}" "${MULTI_VALUE_PARAMS}" ${ARGN})

  foreach (REQUIRED_PARAM ${REQUIRED_PARAMS})
    if (NOT DEFINED PARAM_${REQUIRED_PARAM})
      message (FATAL_ERROR "Required param ${REQUIRED_PARAM} is not set")
    endif()
  endforeach()

  get_values_from_model(PREFIX VALUE MODELS ${PARAM_MODELS} KEYS code.implementation.lib name)
  string (REGEX REPLACE "^lib" "" VALUE_CODE_IMPLEMENTATION_LIB ${VALUE_CODE_IMPLEMENTATION_LIB})
  set (LIBRARY_NAME ${VALUE_CODE_IMPLEMENTATION_LIB} CACHE INTERNAL "Library name got from model")

  set(CUSTOM_PREFIX "kurento")

  foreach (MODULES_DIR ${KURENTO_MODULES_DIR})
    set (KURENTO_MODULES_DIR_LINE "${KURENTO_MODULES_DIR_LINE};-dr;${MODULES_DIR}")
  endforeach()

  set (KTOOL_PROCESSOR_LINE -r ${PARAM_MODELS} ${KURENTO_MODULES_DIR_LINE})

  string(TOUPPER ${VALUE_CODE_IMPLEMENTATION_LIB} VALUE_CODE_IMPLEMENTATION_LIB_UPPER)

  ###############################################################
  # Get reduced kmd
  ###############################################################

  execute_code_generator (OUTPUT_VARIABLE PROCESSOR_OUTPUT
    EXEC_PARAMS ${KTOOL_PROCESSOR_LINE} -p)

  if ("${PROCESSOR_OUTPUT}" STREQUAL "")
    message (FATAL_ERROR "No cmake dependencies generated")
  else()
    string(REPLACE "\n" ";" PROCESSOR_OUTPUT ${PROCESSOR_OUTPUT})
  endif()

  foreach (_FILE ${PROCESSOR_OUTPUT})
    if (${_FILE} MATCHES "${EVENTS_PREFIX}.*")
      string(REPLACE ${EVENTS_PREFIX} "" _FILE ${_FILE})
      string(REGEX REPLACE "\t+" "" _FILE ${_FILE})
      string(REGEX REPLACE " +" "" _FILE ${_FILE})
      list (APPEND MODULE_EVENTS ${_FILE})
    elseif (${_FILE} MATCHES "${REMOTE_CLASSES_PREFIX}.*")
      string(REPLACE ${REMOTE_CLASSES_PREFIX} "" _FILE ${_FILE})
      string(REGEX REPLACE "\t+" "" _FILE ${_FILE})
      string(REGEX REPLACE " +" "" _FILE ${_FILE})
      list (APPEND MODULE_REMOTE_CLASSES ${_FILE})
    elseif (${_FILE} MATCHES "${COMPLEX_TYPES_PREFIX}.*")
      string(REPLACE ${COMPLEX_TYPES_PREFIX} "" _FILE ${_FILE})
      string(REGEX REPLACE "\t+" "" _FILE ${_FILE})
      string(REGEX REPLACE " +" "" _FILE ${_FILE})
      list (APPEND MODULE_COMPLEX_TYPES ${_FILE})
    elseif (${_FILE} MATCHES "${DIGEST_PREFIX}.*")
      string(REPLACE ${DIGEST_PREFIX} "" _FILE ${_FILE})
      string(REGEX REPLACE "\t+" "" _FILE ${_FILE})
      string(REGEX REPLACE " +" "" _FILE ${_FILE})
      set (MODULE_DIGEST ${_FILE})
    endif()
  endforeach()

  set (MODULE_NAME ${VALUE_NAME})

  ###############################################################
  # Relaunch cmake if digest changes
  ###############################################################



  if (NOT ${DISABLE_LIBRARIES_GENERATION})
  ###############################################################
  # Calculate modules dependencies
  ###############################################################

  execute_code_generator (OUTPUT_VARIABLE PROCESSOR_OUTPUT
      EXEC_PARAMS ${KTOOL_PROCESSOR_LINE}
        -it cpp_cmake_dependencies -c ${CMAKE_CURRENT_BINARY_DIR} -lf)

  if ("${PROCESSOR_OUTPUT}" STREQUAL "")
    message (FATAL_ERROR "No cmake dependencies generated")
  else()
    string(REPLACE "\n" ";" PROCESSOR_OUTPUT ${PROCESSOR_OUTPUT})
  endif()

  foreach (_FILE ${PROCESSOR_OUTPUT})
    if (${_FILE} MATCHES "${PROCESSED_PREFIX}.*")
      string(REPLACE ${PROCESSED_PREFIX} "" _FILE ${_FILE})
      string(REGEX REPLACE "\t.*" "" _FILE ${_FILE})
      if (${_FILE} MATCHES ".*cmake")
        message (STATUS "Including generated cmake ${_FILE}")
        include (${CMAKE_CURRENT_BINARY_DIR}/${_FILE})
      else()
        message (WARNING "Unexpected file generated ${_FILE}")
      endif ()
    endif()
  endforeach()

  ###############################################################
  # Interface library
  ###############################################################

  set(GEN_FILES_DIR ${CMAKE_CURRENT_BINARY_DIR}/interface/generated-cpp)

  #Generate source for public interface files
  generate_sources (
    MODELS ${PARAM_MODELS}
    GEN_FILES_DIR ${GEN_FILES_DIR}
    INTERNAL_TEMPLATES_DIR cpp_interface
    SOURCE_FILES_OUTPUT INTERFACE_GENERATED_SOURCES
    HEADER_FILES_OUTPUT INTERFACE_GENERATED_HEADERS
  )

  #Generate source for internal interface files
  generate_sources (
    MODELS ${PARAM_MODELS}
    GEN_FILES_DIR ${GEN_FILES_DIR}
    INTERNAL_TEMPLATES_DIR cpp_interface_internal
    SOURCE_FILES_OUTPUT INTERFACE_INTERNAL_GENERATED_SOURCES
    HEADER_FILES_OUTPUT INTERFACE_INTERNAL_GENERATED_HEADERS
  )

  #Generate source for internal interface files
  add_library (${VALUE_CODE_IMPLEMENTATION_LIB}interface
    ${PARAM_INTERFACE_LIB_EXTRA_SOURCES}
    ${PARAM_INTERFACE_LIB_EXTRA_HEADERS}
    ${INTERFACE_INTERNAL_GENERATED_SOURCES}
    ${INTERFACE_INTERNAL_GENERATED_HEADERS}
    ${INTERFACE_GENERATED_SOURCES}
    ${INTERFACE_GENERATED_HEADERS}
  )
  if(SANITIZERS_ENABLED)
    add_sanitizers(${VALUE_CODE_IMPLEMENTATION_LIB}interface)
  endif()

  target_link_libraries (${VALUE_CODE_IMPLEMENTATION_LIB}interface
    ${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_DEPENDENCIES_LIBRARIES}
    ${PARAM_INTERFACE_LIB_EXTRA_LIBRARIES}
  )

  set_property (TARGET ${VALUE_CODE_IMPLEMENTATION_LIB}interface
    PROPERTY INCLUDE_DIRECTORIES
      ${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_DEPENDENCIES_INCLUDE_DIRS}
      ${PARAM_INTERFACE_LIB_EXTRA_INCLUDE_DIRS}
  )

  set(INCLUDE_PREFIX "${CMAKE_INSTALL_INCLUDEDIR}/${CUSTOM_PREFIX}/modules/${VALUE_NAME}")

  set_property (TARGET ${VALUE_CODE_IMPLEMENTATION_LIB}interface
    PROPERTY PUBLIC_HEADER
      ${PARAM_INTERFACE_LIB_EXTRA_HEADERS}
      ${INTERFACE_GENERATED_HEADERS}
  )

  set_target_properties(${VALUE_CODE_IMPLEMENTATION_LIB}interface PROPERTIES
    VERSION ${PROJECT_VERSION}
    SOVERSION ${PROJECT_VERSION_MAJOR}
  )

  install(TARGETS ${VALUE_CODE_IMPLEMENTATION_LIB}interface
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
    PUBLIC_HEADER DESTINATION ${INCLUDE_PREFIX}
  )

  ###############################################################
  # Create pkgconfig files
  ###############################################################

  set(prefix ${CMAKE_INSTALL_PREFIX})
  set(exec_prefix "\${prefix}")
  set(libdir "\${exec_prefix}/${CMAKE_INSTALL_LIBDIR}")
  set(includedir "\${prefix}/${INCLUDE_PREFIX}")
  set (requires ${requires} ${PARAM_SERVER_IMPL_LIB_PKGCONFIG_EXTRA_REQUIRES})

  execute_code_generator (OUTPUT_VARIABLE PROCESSOR_OUTPUT
    EXEC_PARAMS ${KTOOL_PROCESSOR_LINE}
      -it cpp_pkgconfig -c ${CMAKE_CURRENT_BINARY_DIR} -lf
  )

  if ("${PROCESSOR_OUTPUT}" STREQUAL "")
    message (FATAL_ERROR "No cmake pkg-config files generated")
  else()
    string(REPLACE "\n" ";" PROCESSOR_OUTPUT ${PROCESSOR_OUTPUT})
  endif()

  foreach (_FILE ${PROCESSOR_OUTPUT})
    if (${_FILE} MATCHES "${PROCESSED_PREFIX}.*")
      string(REPLACE ${PROCESSED_PREFIX} "" _FILE ${_FILE})
      string(REGEX REPLACE "\t.*" "" _FILE ${_FILE})
      string(REPLACE ".pc.in" ".pc" _OUT_FILE ${_FILE})
      if (${_FILE} MATCHES ".*pc.in")
        configure_file(${CMAKE_CURRENT_BINARY_DIR}/${_FILE} ${CMAKE_CURRENT_BINARY_DIR}/${_OUT_FILE} @ONLY)
        install(FILES
          ${CMAKE_CURRENT_BINARY_DIR}/${_OUT_FILE}
          DESTINATION ${CMAKE_INSTALL_LIBDIR}/pkgconfig
        )
        message (STATUS "Generated: ${_FILE}")
      else()
        message (WARNING "Unexpected file generated ${_FILE}")
      endif ()
    endif()
  endforeach()

  ###############################################################
  # Server implementation library
  ###############################################################

  set(SERVER_INTERNAL_GEN_FILES_DIR ${CMAKE_CURRENT_BINARY_DIR}/implementation/generated-cpp)

  # Generate internal server files
  generate_sources (
    MODELS ${PARAM_MODELS}
    GEN_FILES_DIR ${SERVER_INTERNAL_GEN_FILES_DIR}
    INTERNAL_TEMPLATES_DIR cpp_server_internal
    SOURCE_FILES_OUTPUT SERVER_INTERNAL_GENERATED_SOURCES
    HEADER_FILES_OUTPUT SERVER_INTERNAL_GENERATED_HEADERS
  )

  set(SERVER_GEN_FILES_DIR ${PARAM_SERVER_STUB_DESTINATION})

  # Generate stub files
  generate_sources (
    MODELS ${PARAM_MODELS}
    GEN_FILES_DIR ${SERVER_GEN_FILES_DIR}
    INTERNAL_TEMPLATES_DIR cpp_server
    SOURCE_FILES_OUTPUT SERVER_GENERATED_SOURCES
    HEADER_FILES_OUTPUT SERVER_GENERATED_HEADERS
    NO_OVERWRITE
  )

  add_library (${VALUE_CODE_IMPLEMENTATION_LIB}impl SHARED
    ${PARAM_SERVER_IMPL_LIB_EXTRA_SOURCES}
    ${PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS}
    ${SERVER_GENERATED_SOURCES}
    ${SERVER_GENERATED_HEADERS}
    ${SERVER_INTERNAL_GENERATED_SOURCES}
    ${SERVER_INTERNAL_GENERATED_HEADERS}
  )
  if(SANITIZERS_ENABLED)
    add_sanitizers(${VALUE_CODE_IMPLEMENTATION_LIB}impl)
  endif()

  add_dependencies(${VALUE_CODE_IMPLEMENTATION_LIB}impl
    ${VALUE_CODE_IMPLEMENTATION_LIB}interface
  )

  target_link_libraries (${VALUE_CODE_IMPLEMENTATION_LIB}impl
    ${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_DEPENDENCIES_LIBRARIES}
    ${PARAM_SERVER_IMPL_LIB_EXTRA_LIBRARIES}
    ${VALUE_CODE_IMPLEMENTATION_LIB}interface
  )

  set_property (TARGET ${VALUE_CODE_IMPLEMENTATION_LIB}impl
    PROPERTY PUBLIC_HEADER
      ${PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS}
      ${SERVER_GENERATED_HEADERS}
      ${SERVER_INTERNAL_GENERATED_HEADERS}
  )

  set_target_properties(${VALUE_CODE_IMPLEMENTATION_LIB}impl PROPERTIES
    VERSION ${PROJECT_VERSION}
    SOVERSION ${PROJECT_VERSION_MAJOR}
  )

  install(
    TARGETS ${VALUE_CODE_IMPLEMENTATION_LIB}impl
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
    PUBLIC_HEADER DESTINATION ${INCLUDE_PREFIX}
  )

  set (PARAM_SERVER_IMPL_LIB_EXTRA_INCLUDE_DIRS
    ${PARAM_SERVER_IMPL_LIB_EXTRA_INCLUDE_DIRS}
    ${PARAM_INTERFACE_LIB_EXTRA_INCLUDE_DIRS}
  )

  set_property (TARGET ${VALUE_CODE_IMPLEMENTATION_LIB}impl
    PROPERTY INCLUDE_DIRECTORIES
      ${PARAM_SERVER_IMPL_LIB_EXTRA_INCLUDE_DIRS}
      ${CMAKE_BINARY_DIR}
      ${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_DEPENDENCIES_INCLUDE_DIRS}
      ${SERVER_GEN_FILES_DIR}
      ${CMAKE_CURRENT_BINARY_DIR}/interface/generated-cpp
      ${CMAKE_CURRENT_BINARY_DIR}/implementation/generated-cpp
  )

  ###############################################################
  # Server module
  ###############################################################

  set(MODULE_GEN_FILES_DIR ${CMAKE_CURRENT_BINARY_DIR}/implementation/generated-cpp)

  generate_sources (
    MODELS ${PARAM_MODELS}
    GEN_FILES_DIR ${MODULE_GEN_FILES_DIR}
    INTERNAL_TEMPLATES_DIR cpp_module
    SOURCE_FILES_OUTPUT MODULE_GENERATED_SOURCES
    HEADER_FILES_OUTPUT MODULE_GENERATED_HEADERS
  )

  file (WRITE ${CMAKE_CURRENT_BINARY_DIR}/module_version.cpp
    "
    extern \"C\" {const char * getModuleVersion ();}
    const char * getModuleVersion () {return \"${PROJECT_VERSION}\";}"
  )

  file (WRITE ${CMAKE_CURRENT_BINARY_DIR}/module_name.cpp
    "
    extern \"C\" {const char * getModuleName ();}
    const char * getModuleName () {return \"${VALUE_NAME}\";}"
  )

  file (WRITE ${CMAKE_CURRENT_BINARY_DIR}/module_generation_time.cpp
    "
    extern \"C\" {const char * getGenerationTime ();}
    const char * getGenerationTime () {return __DATE__ \" \" __TIME__;}"
  )

  file (WRITE ${CMAKE_CURRENT_BINARY_DIR}/generate_kmd_include.cmake
"  execute_process (COMMAND ${KurentoModuleCreator_EXECUTABLE} -r ${PARAM_MODELS} ${KURENTO_MODULES_DIR_LINE} -o ${CMAKE_CURRENT_BINARY_DIR}/)

  file (READ ${CMAKE_CURRENT_BINARY_DIR}/${VALUE_NAME}.kmd.json KMD_DATA)

  string (REGEX REPLACE \"\\n *\" \"\" KMD_DATA \${KMD_DATA})
  string (REPLACE \"\\\"\" \"\\\\\\\"\" KMD_DATA \${KMD_DATA})
  string (REPLACE \"\\\\n\" \"\\\\\\\\n\" KMD_DATA \${KMD_DATA})
  set (KMD_DATA \"\\\"\${KMD_DATA}\\\"\")

  file (WRITE ${CMAKE_CURRENT_BINARY_DIR}/${VALUE_NAME}.kmd.json \${KMD_DATA})
"
  )

  add_custom_command(
    OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/${VALUE_NAME}.kmd.json
    COMMAND ${CMAKE_COMMAND} -P ${CMAKE_CURRENT_BINARY_DIR}/generate_kmd_include.cmake
    DEPENDS ${MODEL_FILES}
    WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
  )

  file (WRITE ${CMAKE_CURRENT_BINARY_DIR}/module_descriptor.cpp
    "
    #include <string>
    extern \"C\" {const char * getModuleDescriptor ();}

    const char * getModuleDescriptor () {
    return
    #include \"${VALUE_NAME}.kmd.json\"
;}"
  )

  add_library (${VALUE_CODE_IMPLEMENTATION_LIB}module MODULE
    ${MODULE_GENERATED_SOURCES}
    ${MODULE_GENERATED_HEADERS}
    ${CMAKE_CURRENT_BINARY_DIR}/module_version.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/module_name.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/module_descriptor.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/module_generation_time.cpp
    ${CMAKE_CURRENT_BINARY_DIR}/${VALUE_NAME}.kmd.json
  )
  if(SANITIZERS_ENABLED)
    add_sanitizers(${VALUE_CODE_IMPLEMENTATION_LIB}module)
  endif()

  add_dependencies(${VALUE_CODE_IMPLEMENTATION_LIB}module
    ${VALUE_CODE_IMPLEMENTATION_LIB}impl
  )

  target_link_libraries (${VALUE_CODE_IMPLEMENTATION_LIB}module
    ${VALUE_CODE_IMPLEMENTATION_LIB}impl
    ${VALUE_CODE_IMPLEMENTATION_LIB}interface
    ${PARAM_MODULE_EXTRA_LIBRARIES}
  )

  set_property (TARGET ${VALUE_CODE_IMPLEMENTATION_LIB}module
    PROPERTY INCLUDE_DIRECTORIES
      ${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_DEPENDENCIES_INCLUDE_DIRS}
      ${CMAKE_CURRENT_SOURCE_DIR}/implementation
      ${SERVER_GEN_FILES_DIR}
      ${CMAKE_CURRENT_BINARY_DIR}/interface/generated-cpp
      ${CMAKE_CURRENT_BINARY_DIR}/implementation/generated-cpp
      ${PARAM_MODULE_EXTRA_INCLUDE_DIRS}
      ${CMAKE_CURRENT_BINARY_DIR}
  )

  install(
    TARGETS ${VALUE_CODE_IMPLEMENTATION_LIB}module
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}/${KURENTO_MODULES_DIR_INSTALL_PREFIX}
    ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}/${KURENTO_MODULES_DIR_INSTALL_PREFIX}
  )

  ###############################################################
  # Create Findmodule.cmake file
  ###############################################################

  foreach (HEADER ${INTERFACE_GENERATED_HEADERS})
    string (REGEX REPLACE ".*/" "" HEADER ${HEADER})
    set (_INTERFACE_GENERATED_HEADERS "${_INTERFACE_GENERATED_HEADERS} ${HEADER}")
  endforeach ()
  if (DEFINED _INTERFACE_GENERATED_HEADERS)
    string (STRIP ${_INTERFACE_GENERATED_HEADERS} _INTERFACE_GENERATED_HEADERS)
  endif ()

  foreach (HEADER ${INTERFACE_INTERNAL_GENERATED_HEADERS})
    string (REGEX REPLACE ".*/" "" HEADER ${HEADER})
    set (_INTERFACE_INTERNAL_GENERATED_HEADERS "${_INTERFACE_INTERNAL_GENERATED_HEADERS} ${HEADER}")
  endforeach ()
  if (DEFINED _INTERFACE_INTERNAL_GENERATED_HEADERS)
    string (STRIP ${_INTERFACE_INTERNAL_GENERATED_HEADERS} _INTERFACE_INTERNAL_GENERATED_HEADERS)
  endif ()

  set (_INTERFACE_HEADERS_DIR ${GEN_FILES_DIR})
  string (REPLACE "${CMAKE_BINARY_DIR}/" "\${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_BINARY_DIR_PREFIX}/" _INTERFACE_HEADERS_DIR ${_INTERFACE_HEADERS_DIR})
  string (REPLACE "${CMAKE_BINARY_DIR}" "\${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_BINARY_DIR_PREFIX}" _INTERFACE_HEADERS_DIR ${_INTERFACE_HEADERS_DIR})
  string (REPLACE "${CMAKE_SOURCE_DIR}/" "" _INTERFACE_HEADERS_DIR ${_INTERFACE_HEADERS_DIR})
  string (REPLACE "${CMAKE_SOURCE_DIR}" "" _INTERFACE_HEADERS_DIR ${_INTERFACE_HEADERS_DIR})

  foreach (HEADER ${SERVER_INTERNAL_GENERATED_HEADERS})
    string (REGEX REPLACE ".*/" "" HEADER ${HEADER})
    set (_SERVER_INTERNAL_GENERATED_HEADERS "${_SERVER_INTERNAL_GENERATED_HEADERS} ${HEADER}")
  endforeach ()
  if (DEFINED _SERVER_INTERNAL_GENERATED_HEADERS)
    string (STRIP ${_SERVER_INTERNAL_GENERATED_HEADERS} _SERVER_INTERNAL_GENERATED_HEADERS)
  endif ()

  set (_SERVER_INTERNAL_GENERATED_HEADERS_DIR ${SERVER_INTERNAL_GEN_FILES_DIR})
  string (REPLACE "${CMAKE_BINARY_DIR}/" "\${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_BINARY_DIR_PREFIX}/" _SERVER_INTERNAL_GENERATED_HEADERS_DIR ${_SERVER_INTERNAL_GENERATED_HEADERS_DIR})
  string (REPLACE "${CMAKE_BINARY_DIR}" "\${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_BINARY_DIR_PREFIX}" _SERVER_INTERNAL_GENERATED_HEADERS_DIR ${_SERVER_INTERNAL_GENERATED_HEADERS_DIR})
  string (REPLACE "${CMAKE_SOURCE_DIR}/" "" _SERVER_INTERNAL_GENERATED_HEADERS_DIR ${_SERVER_INTERNAL_GENERATED_HEADERS_DIR})
  string (REPLACE "${CMAKE_SOURCE_DIR}" "" _SERVER_INTERNAL_GENERATED_HEADERS_DIR ${_SERVER_INTERNAL_GENERATED_HEADERS_DIR})

  foreach (HEADER ${SERVER_GENERATED_HEADERS})
    string (REGEX REPLACE ".*/" "" HEADER ${HEADER})
    set (_SERVER_GENERATED_HEADERS "${_SERVER_GENERATED_HEADERS} ${HEADER}")
  endforeach ()
  if (DEFINED _SERVER_GENERATED_HEADERS)
    string (STRIP ${_SERVER_GENERATED_HEADERS} _SERVER_GENERATED_HEADERS)
  endif ()

  set (_PARAM_SERVER_STUB_DESTINATION ${PARAM_SERVER_STUB_DESTINATION})
  string (REPLACE "${CMAKE_BINARY_DIR}/" "\${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_BINARY_DIR_PREFIX}/" _PARAM_SERVER_STUB_DESTINATION ${_PARAM_SERVER_STUB_DESTINATION})
  string (REPLACE "${CMAKE_BINARY_DIR}" "\${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_BINARY_DIR_PREFIX}" _PARAM_SERVER_STUB_DESTINATION ${_PARAM_SERVER_STUB_DESTINATION})
  string (REPLACE "${CMAKE_SOURCE_DIR}/" "" _PARAM_SERVER_STUB_DESTINATION ${_PARAM_SERVER_STUB_DESTINATION})
  string (REPLACE "${CMAKE_SOURCE_DIR}" "" _PARAM_SERVER_STUB_DESTINATION ${_PARAM_SERVER_STUB_DESTINATION})

  foreach (HEADER ${PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS})
    string (REGEX REPLACE ".*/" "" HEADER ${HEADER})
    set (_PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS "${_PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS} ${HEADER}")
  endforeach ()
  if (DEFINED _PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS)
    string (STRIP ${_PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS} _PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS)
  endif ()

  foreach (HEADER ${PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS})
    string (REGEX REPLACE "/.*$" "" HEADER ${HEADER})
    set (HEADER "${CMAKE_CURRENT_SOURCE_DIR}/${HEADER}")
    string (REPLACE "${CMAKE_BINARY_DIR}/" "\${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_BINARY_DIR_PREFIX}/" HEADER ${HEADER})
    string (REPLACE "${CMAKE_BINARY_DIR}" "\${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_BINARY_DIR_PREFIX}" HEADER ${HEADER})
    string (REPLACE "${CMAKE_SOURCE_DIR}/" "" HEADER ${HEADER})
    string (REPLACE "${CMAKE_SOURCE_DIR}" "" HEADER ${HEADER})
    unset (CONTAINS)
    if (DEFINED _PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS_PREFIX)
      list(FIND _PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS_PREFIX ${HEADER} CONTAINS)
    endif ()
    if (NOT DEFINED CONTAINS)
      list (APPEND _PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS_PREFIX ${HEADER})
    endif ()
  endforeach()

  foreach (HEADER ${_PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS_PREFIX})
    set (_NEW_LIST "${_NEW_LIST} ${HEADER}")
  endforeach()
  set (_PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS_PREFIX ${_NEW_LIST})
  if (DEFINED _PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS_PREFIX)
    string (STRIP ${_PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS_PREFIX} _PARAM_SERVER_IMPL_LIB_EXTRA_HEADERS_PREFIX)
  endif ()

  foreach (HEADER ${PARAM_INTERFACE_LIB_EXTRA_HEADERS})
    string (REGEX REPLACE ".*/" "" HEADER ${HEADER})
    set (_PARAM_INTERFACE_LIB_EXTRA_HEADERS "${_PARAM_INTERFACE_LIB_EXTRA_HEADERS} ${HEADER}")
  endforeach ()
  if (DEFINED _PARAM_INTERFACE_LIB_EXTRA_HEADERS)
    string (STRIP ${_PARAM_INTERFACE_LIB_EXTRA_HEADERS} _PARAM_INTERFACE_LIB_EXTRA_HEADERS)
  endif ()

  foreach (HEADER ${PARAM_INTERFACE_LIB_EXTRA_HEADERS})
    string (REGEX REPLACE "/.*$" "" HEADER ${HEADER})
    set (HEADER "${CMAKE_CURRENT_SOURCE_DIR}/${HEADER}")
    string (REPLACE "${CMAKE_BINARY_DIR}/" "\${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_BINARY_DIR_PREFIX}/" HEADER ${HEADER})
    string (REPLACE "${CMAKE_BINARY_DIR}" "\${${VALUE_CODE_IMPLEMENTATION_LIB_UPPER}_BINARY_DIR_PREFIX}" HEADER ${HEADER})
    string (REPLACE "${CMAKE_SOURCE_DIR}/" "" HEADER ${HEADER})
    string (REPLACE "${CMAKE_SOURCE_DIR}" "" HEADER ${HEADER})
    unset (CONTAINS)
    if (DEFINED _PARAM_INTERFACE_LIB_EXTRA_HEADERS_PREFIX)
      list(FIND _PARAM_INTERFACE_LIB_EXTRA_HEADERS_PREFIX ${HEADER} CONTAINS)
    endif ()
    if (NOT DEFINED CONTAINS)
      list (APPEND _PARAM_INTERFACE_LIB_EXTRA_HEADERS_PREFIX ${HEADER})
    endif ()
  endforeach()

  foreach (HEADER ${_PARAM_INTERFACE_LIB_EXTRA_HEADERS_PREFIX})
    set (_INTERFACE_NEW_LIST "${_INTERFACE_NEW_LIST} ${HEADER}")
  endforeach()
  set (_PARAM_INTERFACE_LIB_EXTRA_HEADERS_PREFIX ${_INTERFACE_NEW_LIST})
  if (DEFINED _PARAM_INTERFACE_LIB_EXTRA_HEADERS_PREFIX)
    string (STRIP ${_PARAM_INTERFACE_LIB_EXTRA_HEADERS_PREFIX} _PARAM_INTERFACE_LIB_EXTRA_HEADERS_PREFIX)
  endif ()

  execute_code_generator (OUTPUT_VARIABLE PROCESSOR_OUTPUT
    EXEC_PARAMS ${KTOOL_PROCESSOR_LINE}
      -it cpp_find_cmake -c ${CMAKE_CURRENT_BINARY_DIR} -lf
  )

  if ("${PROCESSOR_OUTPUT}" STREQUAL "")
    message (FATAL_ERROR "No cmake pkg-config files generated")
  else()
    string(REPLACE "\n" ";" PROCESSOR_OUTPUT ${PROCESSOR_OUTPUT})
  endif()

  set (REQUIRED_LIBS ${PARAM_SERVER_IMPL_LIB_FIND_CMAKE_EXTRA_LIBRARIES})

  foreach (_FILE ${PROCESSOR_OUTPUT})
    if (${_FILE} MATCHES "${PROCESSED_PREFIX}.*")
      string(REPLACE ${PROCESSED_PREFIX} "" _FILE ${_FILE})
      string(REGEX REPLACE "\t.*" "" _FILE ${_FILE})
      string(REPLACE ".cmake.in" ".cmake" _OUT_FILE ${_FILE})
      if (${_FILE} MATCHES ".*cmake.in")
        configure_file(${CMAKE_CURRENT_BINARY_DIR}/${_FILE} ${CMAKE_CURRENT_BINARY_DIR}/${_OUT_FILE} @ONLY)
        install(FILES
          ${CMAKE_CURRENT_BINARY_DIR}/${_OUT_FILE}
          DESTINATION ${CMAKE_INSTALL_DATAROOTDIR}/cmake/Kurento
        )
        message (STATUS "Generated: ${_FILE}")
      else()
        message (WARNING "Unexpected file generated ${_FILE}")
      endif ()
    endif()
  endforeach()

  endif (NOT ${DISABLE_LIBRARIES_GENERATION})
  ###############################################################
  # Generate output kmd file
  ###############################################################

  execute_code_generator (
    EXEC_PARAMS -r ${PARAM_MODELS} ${KURENTO_MODULES_DIR_LINE} -o ${CMAKE_CURRENT_BINARY_DIR}/kmd
  )

  file (GLOB_RECURSE FINAL_MODELS ${CMAKE_CURRENT_BINARY_DIR}/kmd/*kmd.json)

  install(FILES
    ${FINAL_MODELS}
    DESTINATION ${CMAKE_INSTALL_PREFIX}/${CMAKE_INSTALL_DATAROOTDIR}/${KURENTO_MODULES_DIR_INSTALL_PREFIX}
  )

  ###############################################################
  # Install configuration files
  ###############################################################

  if (NOT DEFINED PARAM_MODULE_CONFIG_FILES_DIRS)
    set (PARAM_MODULE_CONFIG_FILES_DIRS ${CMAKE_CURRENT_SOURCE_DIR}/config)
  endif()

  message (STATUS "Getting config files from ${PARAM_MODULE_CONFIG_FILES_DIRS}")

  foreach (DIR ${PARAM_MODULE_CONFIG_FILES_DIRS})
    file (GLOB_RECURSE TMP_FILES ${DIR}/*)
    list (APPEND CONFIG_FILES ${TMP_FILES})
  endforeach()

  if (CONFIG_FILES)
    message (STATUS "Found config files: ${CONFIG_FILES}")
    if (${VALUE_NAME} STREQUAL "core" OR ${VALUE_NAME} STREQUAL "elements" OR ${VALUE_NAME} STREQUAL "filters")
      set (CONFIG_FILES_DIR "kurento")
    else ()
      set (CONFIG_FILES_DIR ${VALUE_NAME})
    endif()
    install (FILES
      ${CONFIG_FILES}
      DESTINATION ${CMAKE_INSTALL_SYSCONFDIR}/kurento/modules/${CONFIG_FILES_DIR}
    )

    file(MAKE_DIRECTORY ${CMAKE_BINARY_DIR}/config/${CONFIG_FILES_DIR})
    foreach (CONFIG_FILE ${CONFIG_FILES})
      execute_process (
          COMMAND ln -fs ${CONFIG_FILE} ${CMAKE_BINARY_DIR}/config/${CONFIG_FILES_DIR})
    endforeach()
  else()
    message (STATUS "No config files found")
  endif()

  ###############################################################
  # Generate Java Client Project
  ###############################################################

  if (${GENERATE_JAVA_CLIENT_PROJECT})
    find_package(Maven)

    execute_code_generator (
      EXEC_PARAMS
        -r ${PARAM_MODELS} ${KURENTO_MODULES_DIR_LINE} -c ${CMAKE_BINARY_DIR}/java -maven
    )

    find_program(xmllint_EXECUTABLE NAMES xmllint)
    mark_as_advanced(xmllint_EXECUTABLE)

    if (xmllint_EXECUTABLE)
      execute_process(
        COMMAND xmllint --format -
        INPUT_FILE ${CMAKE_BINARY_DIR}/java/pom.xml
        OUTPUT_VARIABLE POM_CONTENT
        WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/java
      )

      file(WRITE ${CMAKE_BINARY_DIR}/java/pom.xml ${POM_CONTENT})
    else()
      message(STATUS "'pom.xml' won't be indented unless you intall `xmllint` (libxml2-utils)")
    endif()

    execute_code_generator (
      EXEC_PARAMS
        -r ${PARAM_MODELS} ${KURENTO_MODULES_DIR_LINE} -o ${CMAKE_BINARY_DIR}/java/src/main/kmd
    )

    if (${Maven_FOUND})
      add_custom_target(java
        COMMAND ${Maven_EXECUTABLE} clean package
        WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/java
      )

      add_custom_target(java_install
        COMMAND ${Maven_EXECUTABLE} clean install
        WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/java
      )
    else()
      message (WARNING "Maven not found, build targets for java are disabled")
    endif()
  endif()

  ###############################################################
  # Generate JavaScript Client Project
  ###############################################################

  if (${GENERATE_JS_CLIENT_PROJECT})
    find_package(Npm)

    get_values_from_model(PREFIX VALUE MODELS ${PARAM_MODELS} KEYS code.api.js.nodeName)

    file(WRITE ${CMAKE_BINARY_DIR}/js_project_name "${VALUE_CODE_API_JS_NODENAME}")

    #Download kurento-client-js
    set (KURENTO_CLIENT_JS_DIR ${CMAKE_BINARY_DIR}/kurento-client-js)
    file(REMOVE_RECURSE ${KURENTO_CLIENT_JS_DIR})
    file(MAKE_DIRECTORY ${KURENTO_CLIENT_JS_DIR})

    execute_process(
      COMMAND ${GIT_EXECUTABLE} clone ${KURENTO_CLIENT_JS_GIT} ${KURENTO_CLIENT_JS_DIR}
      WORKING_DIRECTORY ${CMAKE_BINARY_DIR}
    )

    execute_process(
      COMMAND ${GIT_EXECUTABLE} checkout ${KURENTO_CLIENT_JS_BRANCH}
      WORKING_DIRECTORY ${KURENTO_CLIENT_JS_DIR}
    )

    execute_code_generator (
      EXEC_PARAMS
        -r ${PARAM_MODELS} ${KURENTO_MODULES_DIR_LINE} -c ${CMAKE_BINARY_DIR}/js -npm
    )

    execute_code_generator (
      EXEC_PARAMS
        -r ${PARAM_MODELS} ${KURENTO_MODULES_DIR_LINE} -c ${CMAKE_BINARY_DIR}/js/lib -t ${KURENTO_CLIENT_JS_DIR}/templates
    )

    execute_code_generator (
      EXEC_PARAMS
        -r ${PARAM_MODELS} ${KURENTO_MODULES_DIR_LINE} -o ${CMAKE_BINARY_DIR}/js/src
    )

    if (EXISTS ${CMAKE_SOURCE_DIR}/LICENSE)
      file (READ ${CMAKE_SOURCE_DIR}/LICENSE LICENSE)
      file (WRITE ${CMAKE_BINARY_DIR}/js/LICENSE ${LICENSE})
    else()
      message (WARNING "LICENSE file on project root directory is missing")
    endif()

    if (${Npm_FOUND})
      add_custom_target(js
        COMMAND ${Npm_EXECUTABLE} pack
        WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/js
      )

      add_custom_target(js_install
        COMMAND ${Npm_EXECUTABLE} install -g
        WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/js
      )
    else()
      message (WARNING "Npm not found, build targets for js are disabled")
    endif()
  endif()

endfunction()

function (get_values_from_model)
  set (OPTION_PARAMS
  )

  set (ONE_VALUE_PARAMS
    PREFIX
  )

  set (MULTI_VALUE_PARAMS
    MODELS
    KEYS
  )

  set (REQUIRED_PARAMS
    MODELS
    KEYS
  )

  cmake_parse_arguments("PARAM" "${OPTION_PARAMS}" "${ONE_VALUE_PARAMS}" "${MULTI_VALUE_PARAMS}" ${ARGN})

  foreach (REQUIRED_PARAM ${REQUIRED_PARAMS})
    if (NOT DEFINED PARAM_${REQUIRED_PARAM})
      message (FATAL_ERROR "Required param ${REQUIRED_PARAM} is not set")
    endif()
  endforeach()

  foreach (MODULES_DIR ${KURENTO_MODULES_DIR})
    set (KURENTO_MODULES_DIR_LINE "${KURENTO_MODULES_DIR_LINE};-dr;${MODULES_DIR}")
  endforeach()

  execute_code_generator (
    EXEC_PARAMS -r ${PARAM_MODELS} ${KURENTO_MODULES_DIR_LINE} -s ${PARAM_KEYS}
    OUTPUT_VARIABLE PROCESSOR_OUTPUT
  )

  if ("${PROCESSOR_OUTPUT}" STREQUAL "")
    message (FATAL_ERROR "No values found")
  else()
    string(REPLACE "\n" ";" PROCESSOR_OUTPUT ${PROCESSOR_OUTPUT})
  endif()

  set (VALUE_PREFIX "Value: ")

  foreach (_LINE ${PROCESSOR_OUTPUT})
    if (${_LINE} MATCHES "${VALUE_PREFIX}.*")
      string(REPLACE ${VALUE_PREFIX} "" _LINE ${_LINE})
      foreach (_KEY ${PARAM_KEYS})
          if (${_LINE} MATCHES "${_KEY} = ")
            string(REPLACE "${_KEY} = " "" _LINE ${_LINE})
            string(REPLACE "." "_" _KEY ${_KEY})
            string(TOUPPER ${_KEY} _KEY)
            if (DEFINED PARAM_PREFIX)
              set (${PARAM_PREFIX}_${_KEY} ${_LINE} PARENT_SCOPE)
            else()
              set (${_KEY} ${_LINE} PARENT_SCOPE)
            endif()
          endif()
      endforeach ()
    endif()
  endforeach()

endfunction ()
