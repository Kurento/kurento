# include opencv library
include_directories(${OpenCV_INCLUDE_DIRS})

# include alvar library
include_directories(${ALVAR_INCLUDE_DIRS})

# ensure plugin api is exported when building library
add_definitions(-DALVAR_Capture_Plugin_Highgui_BUILD)

# define plugin library source files to compile
set(ALVARCapturePluginHighgui_HEADERS
    CapturePluginHighgui.h
)
set(ALVARCapturePluginHighgui_SOURCES
    CapturePluginHighgui.cpp
)

# add source files to plugin library and set name and properties
add_library(ALVARCapturePluginHighgui MODULE
    ${ALVARCapturePluginHighgui_HEADERS}
    ${ALVARCapturePluginHighgui_SOURCES}
)
set_target_properties(ALVARCapturePluginHighgui PROPERTIES
  OUTPUT_NAME alvarcapturepluginhighgui
  VERSION ${ALVAR_VERSION}
  SOVERSION ${ALVAR_VERSION_MAJOR}
  DEBUG_POSTFIX d
)

# link plugin library target to required libraries
target_link_libraries(ALVARCapturePluginHighgui
    ALVAR
    ALVARPlatform
    ${OpenCV_LIBRARIES}
)

# install headers
if(ALVAR_PACKAGE MATCHES sdk)
    install(FILES ${ALVARCapturePluginHighgui_HEADERS} DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}/alvar/platform/capture_plugin_highgui)
endif(ALVAR_PACKAGE MATCHES sdk)

# install target
install(TARGETS ALVARCapturePluginHighgui DESTINATION ${CMAKE_INSTALL_LIBDIR})

# hack to setup environment runtime path
list(APPEND ALVAR_RUNTIME
    ${CMAKE_CURRENT_BINARY_DIR}\;
)
set(ALVAR_RUNTIME ${ALVAR_RUNTIME} PARENT_SCOPE)
