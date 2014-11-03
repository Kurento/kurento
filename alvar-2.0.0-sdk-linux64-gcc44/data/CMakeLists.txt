# install files for exported build environment
if(ALVAR_PACKAGE MATCHES sdk)
    install(FILES CMakeLists.txt DESTINATION data)
endif(ALVAR_PACKAGE MATCHES sdk)

# define data files
set(ALVAR_DATA_FILES
    axes.osg
    grid.osg
    markerless.png
    multimarker.xml
)

# install data files
if(ALVAR_PACKAGE MATCHES sdk)
    install(FILES ${ALVAR_DATA_FILES} DESTINATION data)
else(ALVAR_PACKAGE MATCHES sdk)
    install(FILES ${ALVAR_DATA_FILES} DESTINATION bin)
endif(ALVAR_PACKAGE MATCHES sdk)
