cmake_minimum_required(VERSION 2.6)
if(POLICY CMP0011)
  cmake_policy(SET CMP0011 NEW)
endif(POLICY CMP0011)

find_program(GLIB_MKENUMS glib-mkenums)
find_program(GLIB_GENMARSHAL glib-genmarshal)

macro(add_glib_marshal outfiles name prefix)
  add_custom_command(
    OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/${name}.h"
    COMMAND ${GLIB_GENMARSHAL} --header "--prefix=${prefix}"
            "${CMAKE_CURRENT_SOURCE_DIR}/${name}.list"
            > "${CMAKE_CURRENT_BINARY_DIR}/${name}.h"
    DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/${name}.list"
  )
  add_custom_command(
    OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/${name}.c"
#    COMMAND echo "\\#include \\\"${otherinclude}\\\"" > "${CMAKE_CURRENT_BINARY_DIR}/${name}.c"
    COMMAND echo "\\#include \\\"glib-object.h\\\"" >> "${CMAKE_CURRENT_BINARY_DIR}/${name}.c"
    COMMAND echo "\\#include \\\"${name}.h\\\"" >> "${CMAKE_CURRENT_BINARY_DIR}/${name}.c"
    COMMAND ${GLIB_GENMARSHAL} --body "--prefix=${prefix}"
            "${CMAKE_CURRENT_SOURCE_DIR}/${name}.list"
            >> "${CMAKE_CURRENT_BINARY_DIR}/${name}.c"
    DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/${name}.list"
            "${CMAKE_CURRENT_BINARY_DIR}/${name}.h"
  )
  list(APPEND ${outfiles} "${CMAKE_CURRENT_BINARY_DIR}/${name}.c")
endmacro(add_glib_marshal)

macro(add_glib_enumtypes outfiles name includeguard)
  set (HEADERS "")
  foreach(header ${ARGN})
    set (HEADERS ${HEADERS}\#include \\\"${header}\\\"\\n)
  endforeach(header)

  add_custom_command(
    OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/${name}.h"
    COMMAND ${GLIB_MKENUMS}
        --fhead \"\#ifndef __${includeguard}_ENUM_TYPES_H__\\n\#define __${includeguard}_ENUM_TYPES_H__\\n\\n\#include <glib-object.h>\\n\\nG_BEGIN_DECLS\\n\"
        --fprod \"\\n/* enumerations from \\\"@filename@\\\" */\\n\"
        --vhead \"GType @enum_name@_get_type \(void\)\;\\n\#define GST_TYPE_@ENUMSHORT@ \(@enum_name@_get_type\(\)\)\\n\"
        --ftail \"\\nG_END_DECLS\\n\\n\#endif /* __${includeguard}_ENUM_TYPES_H__ */\"
        ${ARGN} > "${CMAKE_CURRENT_BINARY_DIR}/${name}.h"
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    DEPENDS ${ARGN}
  )
  add_custom_command(
    OUTPUT "${CMAKE_CURRENT_BINARY_DIR}/${name}.c"
    COMMAND ${GLIB_MKENUMS}
        --fhead \"\#include \\"${name}.h\\"\\n${HEADERS}\"
        --fprod \"\\n/* enumerations from \\"@filename@\\" */\"
        --vhead \"GType\\n@enum_name@_get_type \(void\)\\n{\\n"  "static volatile gsize g_define_type_id__volatile = 0\;\\n"  "if \(g_once_init_enter \(&g_define_type_id__volatile\)\) {\\n"    "static const G@Type@Value values[] = {\"
        --vprod \""      "{ @VALUENAME@, \\"@VALUENAME@\\", \\"@valuenick@\\" },\"
        --vtail \""      "{ 0, NULL, NULL }\\n"    "}\;\\n"    "GType g_define_type_id = g_\@type\@_register_static \(\\"@EnumName@\\", values\)\;\\n"    "g_once_init_leave \(&g_define_type_id__volatile, g_define_type_id\)\;\\n"  "}\\n"  "return g_define_type_id__volatile\;\\n}\\n\"
            ${ARGN} > "${CMAKE_CURRENT_BINARY_DIR}/${name}.c"
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
    DEPENDS ${ARGN}
            "${CMAKE_CURRENT_BINARY_DIR}/${name}.h"
  )
  list(APPEND ${outfiles} "${CMAKE_CURRENT_BINARY_DIR}/${name}.c")
endmacro(add_glib_enumtypes)
