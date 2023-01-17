# include opencv library
include_directories(${OpenCV_INCLUDE_DIRS})

# include alvar library
include_directories(${ALVAR_INCLUDE_DIRS})

# include openscenegraph library
include_directories(${OPENSCENEGRAPH_INCLUDE_DIRS})

# define demos in alphabetical order
set(ALVAR_DEMOS
    Demo3DMarkerField
    DemoMarkerField
    DemoMarkerHide
    DemoModel2Marker
    DemoSfM
)

# add each demo as a seperate executable
foreach(_demo ${ALVAR_DEMOS})
    # add source files to executable and set name and properties
    add_executable(${_demo} ${_demo}.cpp)
    string(TOLOWER ${_demo} _filename)
    set_target_properties(${_demo} PROPERTIES OUTPUT_NAME ${_filename})
    set_target_properties(${_demo} PROPERTIES DEBUG_POSTFIX d)

    # link executable target to required libraries
    target_link_libraries(${_demo}
        ${OpenCV_LIBRARIES}
        ${ALVAR_LIBRARIES}
        ${OPENSCENEGRAPH_LIBRARIES}
        SharedSamples
    )

    # install executable
    if(NOT ALVAR_PACKAGE OR ALVAR_PACKAGE MATCHES bin)
        install(TARGETS ${_demo} DESTINATION bin)
    endif(NOT ALVAR_PACKAGE OR ALVAR_PACKAGE MATCHES bin)
endforeach(_demo ${ALVAR_DEMOS})

# install files for exported build environment
if(ALVAR_PACKAGE MATCHES sdk)
    install(FILES CMakeLists.txt DESTINATION demo)
    file(GLOB ALVAR_DEMO_HEADERS "*.h")
    install(FILES ${ALVAR_DEMO_HEADERS} DESTINATION demo)
    file(GLOB ALVAR_DEMO_SOURCES "*.cpp")
    install(FILES ${ALVAR_DEMO_SOURCES} DESTINATION demo)
endif(ALVAR_PACKAGE MATCHES sdk)

# copy files to build directory
configure_file(../data/axes.osg ${CMAKE_CURRENT_BINARY_DIR}/axes.osg COPYONLY)
configure_file(../data/grid.osg ${CMAKE_CURRENT_BINARY_DIR}/grid.osg COPYONLY)
configure_file(../data/markerless.png ${CMAKE_CURRENT_BINARY_DIR}/markerless.png COPYONLY)
configure_file(../data/multimarker.xml ${CMAKE_CURRENT_BINARY_DIR}/multimarker.xml COPYONLY)
