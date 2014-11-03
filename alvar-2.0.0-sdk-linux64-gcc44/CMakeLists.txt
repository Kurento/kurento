# minimum version and module directory
if(WIN32)
    cmake_minimum_required(VERSION 2.8.3)
else(WIN32)
    cmake_minimum_required(VERSION 2.8)
endif(WIN32)
set(CMAKE_MODULE_PATH "${CMAKE_SOURCE_DIR}/build/cmake")

# define project
project(ALVAR)

# set project version
include(version.cmake OPTIONAL)

# set project packaging
if(ALVAR_VERSION) # compiling from src package
    if(ALVAR_PACKAGE) # force package
        set(_package ${ALVAR_PACKAGE})
    else(ALVAR_PACKAGE) # use default src package
        set(_package src)
    endif(ALVAR_PACKAGE)
    set(ALVAR_PACKAGE ${_package} CACHE STRING "ALVAR package type (src: only building source, sdk: building sdk package, bin: building bin package)")
endif(ALVAR_VERSION)

# set library type
if(ALVAR_PACKAGE)
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
endif(ALVAR_PACKAGE)

# determine compiler
if(ALVAR_PACKAGE)
    if(WIN32)
        file(WRITE "${CMAKE_BINARY_DIR}/return0.cpp"
            "#include <iostream>\n"
            "int main() {\n"
            "  std::cout << _MSC_VER << std::endl;\n"
            "  return 0;\n"
            "}\n"
        )
        try_run(_run_result _compile_result
            "${CMAKE_BINARY_DIR}" "${CMAKE_BINARY_DIR}/return0.cpp"
            RUN_OUTPUT_VARIABLE _output)
        file(REMOVE "${CMAKE_BINARY_DIR}/return0.cpp")
        if(NOT _run_result AND _compile_result AND _output)
            string(REGEX REPLACE "^\([0-9][0-9][0-9][0-9]\).*" "\\1" _version "${_output}")
        endif(NOT _run_result AND _compile_result AND _output)
        if(_version MATCHES "^1400$")
            set(ALVAR_COMPILER vs2005)
        elseif(_version MATCHES "^1500$")
            set(ALVAR_COMPILER vs2008)
        elseif(_version MATCHES "^1600$")
            set(ALVAR_COMPILER vs2010)
        endif(_version MATCHES "^1400$")
    else(WIN32)
        execute_process(COMMAND ${CMAKE_C_COMPILER} "-dumpversion" RESULT_VARIABLE _result OUTPUT_VARIABLE _output)
        if(NOT _result AND _output)
            string(REGEX REPLACE "^\([0-9]+\)\\.\([0-9]+\)\\.[0-9]+[^0-9]*" "\\1\\2" _version "${_output}")
        endif(NOT _result AND _output)
        set(ALVAR_COMPILER "gcc${_version}")
    endif(WIN32)
endif(ALVAR_PACKAGE)

# find package dependencies
if($ENV{OPENCV_ROOT})
    set(OpenCV_ROOT_DIR $ENV{OPENCV_ROOT} CACHE PATH "The OpenCV root directory")
endif($ENV{OPENCV_ROOT})
find_package(OpenCV COMPONENTS calib3d core features2d flann highgui imgproc legacy ml video)
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
if(WIN32)
    add_definitions(-DNOMINMAX)
    add_definitions(-D_CRT_SECURE_NO_WARNINGS)
    add_definitions(-D_CRT_SECURE_NO_DEPRECATE)
endif(WIN32)

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
if(WIN32)
    # Overriding versions in FindGLUT
    find_path(GLUT_INCLUDE_DIR NAMES glut.h
        PATHS
        ${GLUT_ROOT_PATH}
        ${GLUT_ROOT_PATH}/GL
        ${GLUT_ROOT_PATH}/include)
    find_library(GLUT_glut_LIBRARY NAMES glut32
        PATHS
        ${GLUT_ROOT_PATH}
        ${GLUT_ROOT_PATH}/lib
        ${GLUT_ROOT_PATH}/Release)
endif(WIN32)
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

if(ALVAR_PACKAGE)
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
endif(ALVAR_PACKAGE)

if(NOT ALVAR_PACKAGE)
    set(ALVAR_ROOT_DIR ${CMAKE_SOURCE_DIR})
    find_package(ALVAR REQUIRED)
endif(NOT ALVAR_PACKAGE)

if(NOT ALVAR_PACKAGE)
    # set install target
    set(CMAKE_INSTALL_PREFIX ${CMAKE_BINARY_DIR}/../../ CACHE STRING "Directory to install files for packaging" FORCE)
endif(NOT ALVAR_PACKAGE)

if(ALVAR_PACKAGE)
    # add debug definition on unix
    if(UNIX)
        if(CMAKE_BUILD_TYPE MATCHES Debug)
            add_definitions(-D_DEBUG)
        endif(CMAKE_BUILD_TYPE MATCHES Debug)
    endif(UNIX)
    
    # do not include manifests for plugins (workaround for windows xp sp2)
    if(MSVC)
        if(MSVC_VERSION GREATER 1310)
            set(CMAKE_MODULE_LINKER_FLAGS "${CMAKE_MODULE_LINKER_FLAGS} /MANIFEST:NO")
        endif(MSVC_VERSION GREATER 1310)
    endif(MSVC)
endif(ALVAR_PACKAGE)

# set install rpath to current directory such that applications can find libraries that are located in the same directory
SET(CMAKE_INSTALL_RPATH ".")

# disable warning C4251 and C4275 on windows
if(WIN32)
    set(_build_types
        Debug
        Release
        RelWithDebInfo
        MinSizeRel
    )
    foreach(_type ${_build_types})
        string(TOUPPER ${_type} _type)
        set(CMAKE_C_FLAGS_${_type} "${CMAKE_C_FLAGS_${_type}} /wd4251 /wd4275" CACHE STRING "Force static linking of runtime libraries" FORCE)
        set(CMAKE_CXX_FLAGS_${_type} "${CMAKE_CXX_FLAGS_${_type}} /wd4251 /wd4275" CACHE STRING "Force static linking of runtime libraries" FORCE)
    endforeach(_type ${CMAKE_CONFIGURATION_TYPES})
endif(WIN32)

# install runtime libraries
if(ALVAR_PACKAGE)
    set(CMAKE_INSTALL_SYSTEM_RUNTIME_LIBS_SKIP TRUE)
    include(InstallRequiredSystemLibraries)
    install(PROGRAMS ${CMAKE_INSTALL_SYSTEM_RUNTIME_LIBS} DESTINATION bin)
endif(ALVAR_PACKAGE)

# install files
if(ALVAR_PACKAGE MATCHES sdk)
    install(FILES CMakeLists.txt DESTINATION .)
    install(FILES README DESTINATION .)
endif(ALVAR_PACKAGE MATCHES sdk)
install(FILES LICENSE DESTINATION .)

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
if(NOT ALVAR_PACKAGE)
    list(APPEND ALVAR_RUNTIME
        ${ALVAR_ROOT_DIR}/bin\;
    )
endif(NOT ALVAR_PACKAGE)

# attempt to find testing framework
if(ALVAR_PACKAGE)
    find_package(CxxTest)
    find_package(Perl)
    if(CxxTest_FOUND AND PERL_FOUND)
        enable_testing()
        if(ALVAR_PACKAGE MATCHES sdk|bin)
            include(CTest)
        endif(ALVAR_PACKAGE MATCHES sdk|bin)
    else(CxxTest_FOUND AND PERL_FOUND)
        message(STATUS "  Tests will not run!")
    endif(CxxTest_FOUND AND PERL_FOUND)
endif(ALVAR_PACKAGE)

# attempt to enable packaging support
if(ALVAR_PACKAGE MATCHES sdk|bin)
    include(CPackConfig.cmake)
    include(CPack)
endif(ALVAR_PACKAGE MATCHES sdk|bin)

# process sub directories
if(ALVAR_PACKAGE)
    add_subdirectory(3rdparty)
    add_subdirectory(src)
    add_subdirectory(doc)
    add_subdirectory(test)
endif(ALVAR_PACKAGE)
add_subdirectory(sample)
if(OPENSCENEGRAPH_FOUND)
    add_subdirectory(demo)
endif(OPENSCENEGRAPH_FOUND)
add_subdirectory(build)
add_subdirectory(data)

# hack macro to help copy runtime dependencies
macro(copy_runtime _libraries _runtimes _prefixes)
    set(_type none)
    foreach(_library ${_libraries})
        if(_library MATCHES "^optimized$")
            set(_add FALSE)
            set(_type optimized)
        elseif(_library MATCHES "^debug$")
            set(_add FALSE)
            set(_type debug)
        else(_library MATCHES "^optimized$")
            if(_type MATCHES none|optimized)

                string(REGEX REPLACE "^.*/([a-zA-Z0-9_\\-]+)\\.lib$" "\\1" _name "${_library}")
                set(_runtime _runtime-NOTFOUND)
                find_file(_runtime ${_name}.dll PATHS ${_runtimes} NO_DEFAULT_PATH)
                foreach(_prefix ${_prefixes})
                    find_file(_runtime ${_prefix}${_name}.dll PATHS ${_runtimes} NO_DEFAULT_PATH)
                endforeach(_prefix)

                install(FILES ${_runtime} DESTINATION bin)

            endif(_type MATCHES none|optimized)
            set(_type none)
        endif(_library MATCHES "^optimized$")
    endforeach(_library)
endmacro(copy_runtime)

# hack to copy runtime dependencies
if(ALVAR_PACKAGE MATCHES bin AND WIN32)
    copy_runtime("${OpenCV_LIBRARIES}" "${ALVAR_RUNTIME}" "")
    copy_runtime("${GLUT_LIBRARIES}" "${ALVAR_RUNTIME}" "")
    set(_prefixes osg55- osg74- ot11-)
    copy_runtime("${OPENSCENEGRAPH_LIBRARIES}" "${ALVAR_RUNTIME}" "${_prefixes}")
    find_file(_osgplugin osgdb_osg.dll PATHS ${ALVAR_RUNTIME} PATH_SUFFIXES osgPlugins-${OPENSCENEGRAPH_VERSION} NO_DEFAULT_PATH)
    install(FILES ${_osgplugin} DESTINATION bin)
endif(ALVAR_PACKAGE MATCHES bin AND WIN32)
