# minimum version and module directory
cmake_minimum_required(VERSION 2.8)
set(CMAKE_MODULE_PATH "${CMAKE_SOURCE_DIR}/build/cmake")

# define project
project(alvar)

# set project version
include(version.cmake OPTIONAL)

include(GNUInstallDirs)

# set library type
if(NOT DEFINED ALVAR_STATIC)
    set(ALVAR_STATIC FALSE)
endif(NOT DEFINED ALVAR_STATIC)
set(ALVAR_STATIC ${ALVAR_STATIC} CACHE BOOL "ALVAR library type")

if(ALVAR_STATIC)
    set(BUILD_SHARED_LIBS FALSE)
    add_definitions(-DALVAR_STATIC)
else(ALVAR_STATIC)
    set(BUILD_SHARED_LIBS TRUE)
endif(ALVAR_STATIC)

option(BUILD_DOC "Build API documentation with doxygen" FALSE)

# determine compiler
execute_process(COMMAND ${CMAKE_C_COMPILER} "-dumpversion" RESULT_VARIABLE _result OUTPUT_VARIABLE _output)
if(NOT _result AND _output)
    string(REGEX REPLACE "^\([0-9]+\)\\.\([0-9]+\)\\.[0-9]+[^0-9]*" "\\1\\2" _version "${_output}")
endif(NOT _result AND _output)
set(ALVAR_COMPILER "gcc${_version}")

# find package dependencies
if($ENV{OPENCV_ROOT})
    set(OpenCV_ROOT_DIR $ENV{OPENCV_ROOT} CACHE PATH "The OpenCV root directory")
endif($ENV{OPENCV_ROOT})
find_package(OpenCV REQUIRED COMPONENTS opencv_calib3d opencv_core opencv_features2d opencv_flann opencv_highgui opencv_imgproc opencv_ml opencv_video)
if(NOT OpenCV_FOUND)
    find_package(OpenCV COMPONENTS calib3d core features2d flann highgui imgproc legacy ml video
                 NO_MODULE NAMES OpenCV
                 PATHS ${OpenCV_ROOT_DIR} ${OpenCV_ROOT_DIR}/build ${OpenCV_ROOT_DIR}/opencv/build)
    list(GET OpenCV_LIB_DIR 0 OpenCV_ROOT_DIR)
    string(REGEX REPLACE "/lib$" "" OpenCV_ROOT_DIR ${OpenCV_ROOT_DIR})
    if(OpenCV_ROOT_DIR MATCHES x86)
        string(REGEX REPLACE "x86" "common/tbb/ia32" OpenCV_TBB_ROOT_DIR ${OpenCV_ROOT_DIR})
    else(OpenCV_ROOT_DIR MATCHES x86)
        string(REGEX REPLACE "x64" "common/tbb/intel64" OpenCV_TBB_ROOT_DIR ${OpenCV_ROOT_DIR})
    endif(OpenCV_ROOT_DIR MATCHES x86)
    if(NOT OpenCV_FOUND)
        mark_as_advanced(OpenCV_DIR)
        message(FATAL_ERROR "Could NOT find OpenCV")
    endif(NOT OpenCV_FOUND)
endif(NOT OpenCV_FOUND)


find_package(OpenGL)
if(OPENGL_FOUND)
    message(STATUS "Found OpenGL")
else(OPENGL_FOUND)
    message(STATUS "Could NOT find OpenGL")
    if(NOT ALVAR_NOGLUT)
        message(FATAL_ERROR "Failing since GLUT support was not turned off via ALVAR_NOGLUT")
    endif(NOT ALVAR_NOGLUT)
endif(OPENGL_FOUND)

set(GLUT_ROOT_PATH $ENV{GLUT_ROOT} CACHE PATH "The GLUT root directory")

find_package(GLUT)
if(GLUT_FOUND)
    message(STATUS "Found GLUT: ${GLUT_ROOT_PATH}")
else(GLUT_FOUND)
    message(STATUS "Could NOT find GLUT")
    message(STATUS "  Some samples will not compile!")
    message(STATUS "  Some tests will not compile!")
    if(NOT ALVAR_NOGLUT)
        message(FATAL_ERROR "Failing since GLUT support was not turned off via ALVAR_NOGLUT")
    endif(NOT ALVAR_NOGLUT)
endif(GLUT_FOUND)

set(OSG_ROOT_DIR $ENV{OSG_ROOT} CACHE PATH "The OSG root directory")
set(ENV{OSG_DIR} ${OSG_ROOT_DIR})
set(OpenSceneGraph_MARK_AS_ADVANCED TRUE)
find_package(OpenSceneGraph COMPONENTS osgViewer osgGA osgDB osgUtil osgText)

find_package(CMU)

# the PointGrey plugin is currently experimental and not included in the build by default
set(ALVAR_USE_FLYCAPTURE2 ${ALVAR_USE_FLYCAPTURE2} CACHE BOOL "To enable PointGrey FlyCapture2 plugin")
if(ALVAR_USE_FLYCAPTURE2)
    set(FLYCAPTURE2_ROOT_PATH $ENV{FLYCAPTURE2_ROOT_PATH} CACHE PATH "The FlyCapture2 root directory")
    set(FLYCAPTURE2_INCLUDE_DIRS "${FLYCAPTURE2_ROOT_PATH}/include")
    set(FLYCAPTURE2_LIBRARIES "${FLYCAPTURE2_ROOT_PATH}/lib/FlyCapture2.lib")
    set(FLYCAPTURE2_LIBRARIES_DEBUG "${FLYCAPTURE2_ROOT_PATH}/lib/FlyCapture2.lib")
endif(ALVAR_USE_FLYCAPTURE2)

# the DSCapture plugin is currently experimental and not included in the build by default
set(ALVAR_USE_DSCAPTURE ${ALVAR_USE_DSCAPTURE} CACHE BOOL "To enable DSCapture plugin")
if(ALVAR_USE_DSCAPTURE)
    set(DSCAPTURE_ROOT_PATH $ENV{DSCAPTURE_ROOT_PATH} CACHE PATH "The DSCapture root directory")
    set(DSCAPTURE_INCLUDE_DIRS "${DSCAPTURE_ROOT_PATH}")
    set(DSCAPTURE_LIBRARIES "${DSCAPTURE_ROOT_PATH}/lib/DSCapture.lib")
    set(DSCAPTURE_LIBRARIES_DEBUG "${DSCAPTURE_ROOT_PATH}/lib/DSCaptured.lib")
endif(ALVAR_USE_DSCAPTURE)

# add debug definition on unix
if(CMAKE_BUILD_TYPE MATCHES Debug)
    add_definitions(-D_DEBUG)
endif(CMAKE_BUILD_TYPE MATCHES Debug)
    
# install files
install(FILES README DESTINATION ${CMAKE_INSTALL_DOCDIR})
install(FILES LICENSE DESTINATION ${CMAKE_INSTALL_DOCDIR})

# hack to setup environment runtime path
set(ALVAR_RUNTIME)
list(APPEND ALVAR_RUNTIME
    ${OpenCV_ROOT_DIR}/bin\;
    ${GLUT_ROOT_PATH}\;
    ${OSG_ROOT_DIR}/bin\;
)
if(OpenCV_TBB_ROOT_DIR)
    list(APPEND ALVAR_RUNTIME
        ${OpenCV_TBB_ROOT_DIR}\;
    )
endif(OpenCV_TBB_ROOT_DIR)

# attempt to find testing framework
find_package(CxxTest)
find_package(Perl)
if(CxxTest_FOUND AND PERL_FOUND)
    enable_testing()
    include(CTest)
else(CxxTest_FOUND AND PERL_FOUND)
    message(STATUS "  Tests will not run!")
endif(CxxTest_FOUND AND PERL_FOUND)

find_package(PkgConfig)

# process sub directories
if(UNIX AND PKG_CONFIG_FOUND)
    pkg_check_modules(TinyXML tinyxml)
    if(NOT TinyXML_FOUND)
        find_file(TINYXML_H tinyxml.h)
        find_library(TINYXML_SO tinyxml)
        if((${TINYXML_H} EQUAL "TINYXML_H-NOTFOUND") OR (${TINYXML_SO} EQUAL "TINYXML_SO-NOTFOUND"))
            message(STATUS "Could not find TinyXML, using bundled version")
            add_subdirectory(3rdparty)
        else()
            get_filename_component(TinyXML_INCLUDE_DIRS ${TINYXML_H} DIRECTORY)
            set(TinyXML_LIBRARIES tinyxml)
            message(STATUS "Found TinyXML at ${TINYXML_H} and ${TINYXML_SO}")
        endif()
    endif(NOT TinyXML_FOUND)
else(UNIX AND PKG_CONFIG_FOUND)
    add_subdirectory(3rdparty)
endif(UNIX AND PKG_CONFIG_FOUND)
add_subdirectory(src)
add_subdirectory(doc)
add_subdirectory(test)
add_subdirectory(sample)
if(OPENSCENEGRAPH_FOUND)
    add_subdirectory(demo)
endif(OPENSCENEGRAPH_FOUND)
add_subdirectory(data)
