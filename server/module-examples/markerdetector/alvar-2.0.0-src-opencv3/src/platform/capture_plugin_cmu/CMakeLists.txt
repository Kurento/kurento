# include opencv library
include_directories(${OpenCV_INCLUDE_DIRS})

# include alvar library
include_directories(${ALVAR_INCLUDE_DIRS})

# include CMU library
include_directories(${CMU_INCLUDE_DIRS})

# ensure plugin api is exported when building library
add_definitions(-DALVAR_Capture_Plugin_CMU_BUILD)

# define plugin library source files to compile
set(ALVARCapturePluginCMU_HEADERS
    CapturePluginCmu.h
)
set(ALVARCapturePluginCMU_SOURCES
    CapturePluginCmu.cpp
)

# add source files to plugin library and set name and properties
add_library(ALVARCapturePluginCMU MODULE
    ${ALVARCapturePluginCMU_SOURCES}
)
set_target_properties(ALVARCapturePluginCMU PROPERTIES
  OUTPUT_NAME alvarcaptureplugincmu
  VERSION ${ALVAR_VERSION}
  SOVERSION ${ALVAR_VERSION_MAJOR}
  DEBUG_POSTFIX d
)

# link plugin library target to required libraries
target_link_libraries(ALVARCapturePluginCMU
    ALVAR
    ALVARPlatform
    ${OpenCV_LIBRARIES}
    ${CMU_LIBRARIES}
)

# install headers
install(FILES ${ALVARCapturePluginCMU_HEADERS} DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}/alvar/platform/capture_plugin_cmu)

# install target
install(TARGETS ALVARCapturePluginCMU DESTINATION ${CMAKE_INSTALL_LIBDIR})

# hack to setup environment runtime path
list(APPEND ALVAR_RUNTIME
    ${CMAKE_CURRENT_BINARY_DIR}\;
)
set(ALVAR_RUNTIME ${ALVAR_RUNTIME} PARENT_SCOPE)
