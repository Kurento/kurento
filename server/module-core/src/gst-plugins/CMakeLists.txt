include(GLibHelpers)

add_subdirectory(commons)

set(KMS_CORE_SOURCES
  kmscore.c
  kmsagnosticbin.c kmsagnosticbin.h
  kmsagnosticbin3.c kmsagnosticbin3.h
  kmsfilterelement.c kmsfilterelement.h
  kmsaudiomixer.c kmsaudiomixer.h
  kmsaudiomixerbin.c kmsaudiomixerbin.h
  kmsbitratefilter.c kmsbitratefilter.h
  kmsbufferinjector.c kmsbufferinjector.h
  kmspassthrough.c kmspassthrough.h
  kmsdummysrc.c kmsdummysrc.h
  kmsdummysink.c kmsdummysink.h
  kmsdummyduplex.c kmsdummyduplex.h
  kmsdummysdp.c kmsdummysdp.h
  kmsdummyrtp.c kmsdummyrtp.h
  kmsdummyuri.c kmsdummyuri.h
)

add_library(${LIBRARY_NAME} MODULE ${KMS_CORE_SOURCES})
if(SANITIZERS_ENABLED)
  add_sanitizers(${LIBRARY_NAME})
endif()

add_dependencies(${LIBRARY_NAME} kmsgstcommons)

set_property (TARGET ${LIBRARY_NAME}
  PROPERTY INCLUDE_DIRECTORIES
    ${gstreamer-1.0_INCLUDE_DIRS}
    ${gstreamer-base-1.0_INCLUDE_DIRS}
    ${gstreamer-sdp-1.0_INCLUDE_DIRS}
    ${gstreamer-pbutils-1.0_INCLUDE_DIRS}
    ${CMAKE_CURRENT_BINARY_DIR}/../../
    ${CMAKE_CURRENT_BINARY_DIR}/commons/
    ${CMAKE_CURRENT_SOURCE_DIR}/commons/
)

target_link_libraries(${LIBRARY_NAME}
  kmsgstcommons
  ${gstreamer-1.0_LIBRARIES}
  ${gstreamer-base-1.0_LIBRARIES}
  ${gstreamer-sdp-1.0_LIBRARIES}
  ${gstreamer-pbutils-1.0_LIBRARIES}
)

install(
  TARGETS ${LIBRARY_NAME}
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_GST_PLUGINS_DIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

add_subdirectory (vp8parse)
