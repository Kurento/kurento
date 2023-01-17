include (TestHelpers)

if (${gstreamer-check-1.0_FOUND})
  add_subdirectory(check)
endif ()

add_subdirectory(server)
