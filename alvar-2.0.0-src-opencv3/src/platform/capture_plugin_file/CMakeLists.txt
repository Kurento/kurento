# include opencv library
include_directories(${OpenCV_INCLUDE_DIRS})

# include alvar library
include_directories(${ALVAR_INCLUDE_DIRS})

# ensure plugin api is exported when building library
add_definitions(-DALVAR_Capture_Plugin_File_BUILD)

# define plugin library source files to compile
set(ALVARCapturePluginFile_HEADERS
    CapturePluginFile.h
)
set(ALVARCapturePluginFile_SOURCES
    CapturePluginFile.cpp
)

# add source files to plugin library and set name and properties
add_library(ALVARCapturePluginFile MODULE
    ${ALVARCapturePluginFile_HEADERS}
    ${ALVARCapturePluginFile_SOURCES}
)
set_target_properties(ALVARCapturePluginFile PROPERTIES
  OUTPUT_NAME alvarcapturepluginfile
  VERSION ${ALVAR_VERSION}
  SOVERSION ${ALVAR_VERSION_MAJOR}
  DEBUG_POSTFIX d
)

# link plugin library target to required libraries
target_link_libraries(ALVARCapturePluginFile
    ALVAR
    ALVARPlatform
    ${OpenCV_LIBRARIES}
)

# install headers
install(FILES ${ALVARCapturePluginFile_HEADERS} DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}/alvar/platform/capture_plugin_file)

# install target
install(TARGETS ALVARCapturePluginFile DESTINATION ${CMAKE_INSTALL_LIBDIR})

# hack to setup environment runtime path
list(APPEND ALVAR_RUNTIME
    ${CMAKE_CURRENT_BINARY_DIR}\;
)

set(ALVAR_RUNTIME ${ALVAR_RUNTIME} PARENT_SCOPE)
