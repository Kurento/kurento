How to compile the ALVAR library SDK package
--------------------------------------------

Windows

1. Download and install OpenCV 2.4.0.
     Precompiled version from VTT is available in the same location as ALVAR.

2. Download and install GLUT 3.7.6.
     http://www.xmission.com/~nate/glut.html

3. Optionally (for firewire camera support) download and install CMU 6.4.5.
     http://www.cs.cmu.edu/~iwan/1394/downloads/index.html

4. Optionally (for demos) download and install OSG >= 2.8.4.
     http://www.alphapixel.com/osg/downloads/free-openscenegraph-binary-downloads

5. Download and install CMake >= 2.8.3.
     http://www.cmake.org/cmake/resources/software.html

6. Run ./build/generate_[target].bat, where target is one of the following
   supported platforms.
     vs2005: Visual Studio 8 2005
     vs2008: Visual Studio 9 2008
     vs2010: Visual Studio 10 2010

7. If CMake cannot find the required libraries, the cmakegui is launched.
   Configure the following variables according to your development
   environment.
     OpenCV_ROOT_DIR = C:\Program Files\OpenCV
     GLUT_ROOT_PATH = C:\Program Files\glut-3.7.6-bin
     CMU_ROOT_DIR = C:\Program Files\CMU\1394Camera
     OSG_ROOT_DIR = C:\Program Files\OpenSceneGraph
   
   Press 'Configure' and modify the paths until the 'Generate' button is
   enabled. Press 'Generate' and close the cmakegui window.

8. Open ./build/build_[target]_release/ALVAR.sln.bat and build the solution.
   Notice the BAT extension!                     ^^^
   The batch file will ensure that paths to DLLs are properly configured.

9. Optionally build the 'INSTALL' project in the solution to copy the sample
   and demo applications to the ./bin directory.

Linux

* ALVAR binaries are compiled on Kubuntu 10.04. The following instructions are
  specific to (K)Ubuntu distributions. The binaries should work on similar
  GNU/Linux distributions.

1. Setup a build environment.
     apt-get install build-essential cmake

2. Download and install OpenCV 2.4.0.
     Precompiled version from VTT is available in the same location as ALVAR.

3. Install GLUT using distribution package or compile it yourself.
     apt-get install freeglut-dev
     
4. Optionally install OpenSceneGraph using distribution package or compile it
   yourself.
     apt-get install libopenscenegraph-dev

4. Run ./build/generate_[target].sh, where target is one of the following
   supported platforms.
     gcc43: GNU Compiler Collection 4.3
     gcc44: GNU Compiler Collection 4.4
     gcc45: GNU Compiler Collection 4.5 (experimental)

   Example:
     cd ./build
     chmod +x generate*.sh
     ./generate_gcc44.sh

5. If CMake cannot find the required libraries, the cmakegui is launched.
   Configure the following variables accroding to your development
   environment.
     OpenCV_ROOT_DIR = /path/to/opencv
     GLUT_ROOT_PATH = /usr
     OSG_ROOT_DIR = /usr

   Press 'Configure' and modify the paths until the 'Generate' button is
   enabled. Press 'Generate' and close the cmakegui window.

6. Build the project.
     cd ./build/build_gcc44_release
     make

7. Optionally build the 'install' project to copy the sample and demo
   applications to the ./bin directory.
     make install


How to compile the ALVAR library SRC package
--------------------------------------------

When building from the source package, you have the option of using the
precompiled version of OpenCV provided by the OpenCV development team. The
CMake build system should be able to correctly find the dependencies if you
specify the OpenCV_ROOT_DIR variable.
  http://sourceforge.net/projects/opencvlibrary/files/opencv-win/2.4.0

The build instruction are basically the same as when building from the SDK
package.

The source package also contains 'master', 'build' and 'package' scripts in
the build directory. These are used to generate the SDK and BIN packages.
