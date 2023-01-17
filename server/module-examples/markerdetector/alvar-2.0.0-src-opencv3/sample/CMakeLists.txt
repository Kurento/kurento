# include opencv library
include_directories(${OpenCV_INCLUDE_DIRS})

# include opengl and glut library if found
if(GLUT_FOUND)
    include_directories(${OPENGL_INCLUDE_DIR})
    include_directories(${GLUT_INCLUDE_DIR})
endif(GLUT_FOUND)

# include alvar library
include_directories(${ALVAR_INCLUDE_DIRS})

# define samples in alphabetical order
set(ALVAR_SAMPLES
    SampleCamCalib
    SampleCvTestbed
    SampleFilter
    SampleIntegralImage
    SampleLabeling
    SampleMarkerCreator
    SampleMultiMarker
    SampleMultiMarkerBundle
    SampleOptimization
    SampleTrack
)

# define samples that require GlutViewer in alphabetical order
set(ALVAR_SAMPLES_GLUTVIEWER
    SampleMarkerDetector
    SampleMarkerHide
    SamplePointcloud
)

# add shared samples library
add_library(SharedSamples STATIC
    CvTestbed.h
    CvTestbed.cpp
    Shared.h
)

# add GlutViewer library and samples if glut library is found
if(GLUT_FOUND)
    add_library(SharedGlutViewer STATIC
        GlutViewer.h
        GlutViewer.cpp
    )
    target_link_libraries(SharedGlutViewer
        ${OPENGL_LIBRARIES}
        ${GLUT_glut_LIBRARY}
    )
    list(APPEND ALVAR_SAMPLES ${ALVAR_SAMPLES_GLUTVIEWER})
endif(GLUT_FOUND)

# add each sample as a seperate executable
foreach(_sample ${ALVAR_SAMPLES})
    # determine if glut common sources need to be included
    list(FIND ALVAR_SAMPLES_GLUTVIEWER ${_sample} _glutviewer)

    # add source files to executable and set name and properties
    add_executable(${_sample} ${_sample}.cpp)
    string(TOLOWER ${_sample} _filename)
    set_target_properties(${_sample} PROPERTIES OUTPUT_NAME ${_filename})
    set_target_properties(${_sample} PROPERTIES DEBUG_POSTFIX d)

    # link executable target to required libraries
    target_link_libraries(${_sample}
        ${OpenCV_LIBRARIES}
        ${ALVAR_LIBRARIES}
        SharedSamples
    )
    if(NOT _glutviewer EQUAL -1)
        target_link_libraries(${_sample} SharedGlutViewer)
    endif(NOT _glutviewer EQUAL -1)

endforeach(_sample ${ALVAR_SAMPLES})

file(GLOB ALVAR_SAMPLE_HEADERS "*.h")
install(FILES ${ALVAR_SAMPLE_HEADERS} DESTINATION ${CMAKE_INSTALL_DOCDIR}/sample)
file(GLOB ALVAR_SAMPLE_SOURCES "*.cpp")
install(FILES ${ALVAR_SAMPLE_SOURCES} DESTINATION ${CMAKE_INSTALL_DOCDIR}/sample)
