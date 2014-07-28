# - Try to find LibSctp
# Once done this will define
#
#  SCTP_FOUND - system has LibSctp
#  SCTP_INCLUDE_DIRS - the LibSctp include directory
#  SCTP_LIBRARIES - the libraries needed to use LibSctp

find_path(SCTP_INCLUDE_DIRS
    NAMES netinet/sctp.h
)

find_library(SCTP_LIBRARIES
    NAMES sctp
)

include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(LIBSCTP REQUIRED_VARS SCTP_INCLUDE_DIRS SCTP_LIBRARIES)

mark_as_advanced(SCTP_INCLUDE_DIRS SCTP_LIBRARIES)
