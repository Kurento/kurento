include (TestHelpers)

add_subdirectory(server)

if (${GSTREAMER_CHECK_FOUND})
  add_subdirectory(check)
endif ()

