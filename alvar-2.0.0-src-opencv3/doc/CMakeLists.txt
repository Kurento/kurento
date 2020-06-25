# generate doxygen api documentation
find_package(Doxygen)
if(DOXYGEN_FOUND)
    # configure doxygen configuration file with paths
    set(ALVAR_DOXYGEN_LOGO ${CMAKE_SOURCE_DIR}/doc/logo.png)
    set(ALVAR_DOXYGEN_INPUT "\"${CMAKE_BINARY_DIR}/src\" \"${CMAKE_SOURCE_DIR}\"")
    set(ALVAR_DOXYGEN_EXCLUDE ${CMAKE_SOURCE_DIR}/3rdparty)
    set(ALVAR_DOXYGEN_SAMPLE ${CMAKE_SOURCE_DIR}/sample)
    configure_file(doxygen.config.cmake ${CMAKE_CURRENT_BINARY_DIR}/doxygen.config)
    
    # add custom target to generate api documentation
    add_custom_target(Documentation ALL
        ${DOXYGEN_EXECUTABLE} doxygen.config
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}
    )

    # install api documentation
    file(MAKE_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/html)
    install(DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/html DESTINATION ${CMAKE_INSTALL_DOCDIR})
endif(DOXYGEN_FOUND)

# install additional documentation files
install(FILES ALVAR.pdf DESTINATION ${CMAKE_INSTALL_DOCDIR})
install(FILES ReleaseNotes.txt DESTINATION ${CMAKE_INSTALL_DOCDIR})
install(FILES Compiling.txt DESTINATION ${CMAKE_INSTALL_DOCDIR})
