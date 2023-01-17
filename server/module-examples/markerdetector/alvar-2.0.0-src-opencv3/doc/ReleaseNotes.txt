ALVAR 2.0.0 (29 May 2012)
-------------------------

Major feature release.

- changed license to LGPL
- merged basic and pro configurations
- removed time-bomb and licensing system
- upgraded to OpenCV 2.4
- merged external OpenSceneGraph samples as demos
- added new logo
- updated SamplePointcloud sample
- added experimental Point Grey camera capture plugin
- added experimental Direct Show camera capture plugin
- refactored build system, build scripts only build one target at a time
- updated and unified CMake modules
- removed Visual Studio 2003 as supported compiler
- speed up cmu camera capture by dropping frames
- added markerless tracking based on a Ferns classifier
- added SampleMarkerlessCreator to train a classifier for an image
- added SampleMarkerlessDetector to track an image using a classifier
- removed deprecated LabelingStefano class
- removed GaussianNoise class and use OpenCV for random numbers
- added ability to set camera capture resolution
- removed a lot of deprecated and unused code
- translated all Finnish to English
- removed old and unused files
- removed external build system and unified CMake scripts
- added support for src, sdk and bin packages
- added optional dependency on OpenSceneGraph for demos
- compile TinyXML as a static library
- moved several classes to their own implementation files
- improved platform abstraction of Mutex, Threads and Timer classes
- added support for Visual Studio 2010 compiler
- documentation updates
- fixed highgui camera enumeration
- fixed DrawBB method

ALVAR 1.5.0 (4 Jan 2011)
------------------------

Major feature release.

- added licensing system to disable time-bomb
- refactored capturing system to actually allow for third party plugins
- added a check that detects the presence of capture plugins and displays an
  appropriate error message in samples
- documentation updates
- fixed build system on Windows Vista
- fixed bug in UKF implementation
- fixed MultiMarker implementation to allow marker sets that do not contain id 0
- fixed bug with distortion initialization in Camera class

ALVAR 1.4.0 (7 Jul 2010)
------------------------

Major feature release.

- new capturing system based on dynamically loaded plugins which allows for the
  enumeration of devices
- updated capture plugins for highgui, cvcam, cmu and file backends
- linux platform is now officialy supported
- added support for streams in Serialization class
- adapted samples to use the new capturing system
- samples can more easily save and load camera calibrations using unique filenames
- added support for multi marker setups to SampleMarkerCreator
- added size specification to SampleMarkerCreator
- documentation updates
- fixed precision handling in FileFormatUtils class
- fixed time counter in SampleCamCalib
- fixed Mutex class bug on linux
- fixed memory leak in Camera class

ALVAR 1.3.0 (5 Feb 2010)
------------------------

Major feature release.

- added Container3d class to handle 3d data structures (pro)
- added external container (EC) versions for some ALVAR classes: CameraEC,
  MarkerDetectorEC and TrackerFeaturesEC (pro)
- added SimpleSfM class that implements structure from motion tracking (pro)
- added SamplePointCloud sample that shows new tracking capabilities of ALVAR (pro)
- added MultiMarkerInitializer class and improved SampleMultiMarkerBundle (pro)
- removed GLUT dependency from ALVARPlatform
- moved GlutViewer class from ALVARPlatform to samples directory
- added support for setting model view matrix in GlutViewer
- documentation updates
- fixed bug with export of Ransac class (pro)
- fixed bug in Optimization class regarding weight handling (pro)
- fixed bug in FilterMedian class when returning measurements
- fixed bug with optimization parameters in SampleMultiMarkerBundle (pro)
- fixed bug with scale of VR view in SampleMarkerHide

ALVAR 1.2.1 (8 Dec 2009)
------------------------

Minor feature and bugfix release.

- added serialization support
- added block size parameter for labeling
- added support for automatic marker resolution detection
- removed sensor library
- documentation updates
- fixed bug in thread implementation
- fixed bug with larger marker resolutions

ALVAR 1.2.0 (19 Oct 2009)
-------------------------

First public release.
