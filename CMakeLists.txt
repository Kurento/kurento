cmake_minimum_required(VERSION 2.8)

project ("kurento-module-creator")

include(GNUInstallDirs)

#echo -e 'setns x=http://maven.apache.org/POM/4.0.0\ncat /x:project/x:version/text()' | xmllint --shell pom.xml | grep -v /
execute_process (
  COMMAND echo -e "setns x=http://maven.apache.org/POM/4.0.0\ncat /x:project/x:version/text()"
  COMMAND xmllint --shell ${CMAKE_CURRENT_SOURCE_DIR}/pom.xml
  COMMAND grep -v /
  OUTPUT_VARIABLE PROJECT_VERSION
)

file(GLOB_RECURSE MODULE_CREATOR_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/src/*)

list(APPEND MODULE_CREATOR_SOURCES ${CMAKE_CURRENT_SOURCE_DIR}/pom.xml)

add_custom_command (OUTPUT kurento-module-creator-jar-with-dependencies.jar
  COMMAND mvn package -Dmaven.repo.local=${CMAKE_CURRENT_BINARY_DIR}/m2_repository -DbuildDirectory=${CMAKE_CURRENT_BINARY_DIR}
  DEPENDS ${MODULE_CREATOR_SOURCES}
  WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
  )

add_custom_target (kurento-module-creator-jar
  DEPENDS kurento-module-creator-jar-with-dependencies.jar
  )

configure_file(CMake/FindKurentoModuleCreator.cmake.in ${CMAKE_CURRENT_BINARY_DIR}/FindKurentoModuleCreator.cmake @ONLY)

add_custom_target(kurento-module-creator ALL
  DEPENDS kurento-module-creator kurento-module-creator-jar
  )

execute_process(
  COMMAND cp ${CMAKE_CURRENT_SOURCE_DIR}/scripts/kurento-module-creator ${CMAKE_CURRENT_BINARY_DIR}
)

set (CMAKE_MODULES_INSTALL_DIR
  ${CMAKE_INSTALL_DATAROOTDIR}/cmake-${CMAKE_MAJOR_VERSION}.${CMAKE_MINOR_VERSION}/Modules
  CACHE STRING
  "Destination (relative to CMAKE_INSTALL_PREFIX) for cmake modules files"
  )

install(FILES
  ${CMAKE_CURRENT_BINARY_DIR}/FindKurentoModuleCreator.cmake
  DESTINATION ${CMAKE_MODULES_INSTALL_DIR}
  )

install(FILES
  ${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator
  ${CMAKE_CURRENT_BINARY_DIR}/kurento-module-creator-jar-with-dependencies.jar
  DESTINATION ${CMAKE_INSTALL_BINDIR}
  )
