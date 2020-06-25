# include opencv library
include_directories(${OpenCV_INCLUDE_DIRS})

# include alvar library
include_directories(${ALVAR_INCLUDE_DIRS})

# include DSCapture library
include_directories(${DSCAPTURE_INCLUDE_DIRS})

# ensure plugin api is exported when building library
add_definitions(-DALVAR_Capture_Plugin_DSCapture_BUILD)

# define plugin library source files to compile
set(ALVARCapturePluginDSCapture_HEADERS
    CapturePluginDSCapture.h
)
set(ALVARCapturePluginDSCapture_SOURCES
    CapturePluginDSCapture.cpp
)

# add source files to plugin library and set name and properties
add_library(ALVARCapturePluginDSCapture MODULE
    ${ALVARCapturePluginDSCapture_SOURCES}
)
set_target_properties(ALVARCapturePluginDSCapture PROPERTIES
  OUTPUT_NAME alvarcaptureplugindscapture
  VERSION ${ALVAR_VERSION}
  SOVERSION ${ALVAR_VERSION_MAJOR}
  DEBUG_POSTFIX d
)

# link plugin library target to required libraries
target_link_libraries(ALVARCapturePluginDSCapture
    ALVAR
    ALVARPlatform
    ${OpenCV_LIBRARIES}
    optimized ${DSCAPTURE_LIBRARIES}
    debug ${DSCAPTURE_LIBRARIES_DEBUG}
)

# install headers
install(FILES ${ALVARCapturePluginDSCapture_HEADERS} DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}/alvar/platform/capture_plugin_dscapture)

# install target
install(TARGETS ALVARCapturePluginDSCapture DESTINATION ${CMAKE_INSTALL_LIBDIR})

# hack to setup environment runtime path
list(APPEND ALVAR_RUNTIME
    ${CMAKE_CURRENT_BINARY_DIR}\;
)
set(ALVAR_RUNTIME ${ALVAR_RUNTIME} PARENT_SCOPE)
