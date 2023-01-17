# define tinyxml source files to compile
file(GLOB TinyXML_HEADERS "tinyxml/*.h")
file(GLOB TinyXML_SOURCES "tinyxml/*.cpp")

# enable stl interface
add_definitions(-DTIXML_USE_STL)

# add static tinyxml library
add_library(TinyXML STATIC
    ${TinyXML_HEADERS}
    ${TinyXML_SOURCES}
)
if(UNIX)
    set_target_properties(TinyXML PROPERTIES COMPILE_FLAGS -fPIC)
endif(UNIX)

# define tinyxml include directories
set(TinyXML_INCLUDE_DIRS ${CMAKE_CURRENT_SOURCE_DIR}/tinyxml CACHE STRING "TinyXML include directories")
mark_as_advanced(TinyXML_INCLUDE_DIRS)
