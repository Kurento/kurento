# - Try to find LibTesseract
# Once done this will define
#
#  TESSERACT_FOUND - system has LibTesseract
#  TESSERACT_INCLUDE_DIRS - the LibTesseract include directory
#  TESSERACT_LIBRARIES - the libraries needed to use LibTesseract

find_path(TESSERACT_INCLUDE_DIRS
    NAMES tesseract/capi.h
)

find_library(TESSERACT_LIBRARIES
    NAMES tesseract
)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LIBTESSERACT REQUIRED_VARS TESSERACT_INCLUDE_DIRS TESSERACT_LIBRARIES)

mark_as_advanced(TESSERACT_INCLUDE_DIRS TESSERACT_LIBRARIES)
