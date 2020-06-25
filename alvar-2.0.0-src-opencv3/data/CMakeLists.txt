# define data files
set(ALVAR_DATA_FILES
    axes.osg
    grid.osg
    markerless.png
    multimarker.xml
)

# install data files
install(FILES ${ALVAR_DATA_FILES} DESTINATION ${CMAKE_INSTALL_DATADIR}/alvar)
