include (TestHelpers)

add_subdirectory(server)

if (${gstreamer-check-1.0_FOUND})
  add_subdirectory(check)
endif ()
