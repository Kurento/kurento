# CMake Config Mode search file. Adds the main Kurento dir to CMAKE_MODULE_PATH.
# https://cmake.org/cmake/help/latest/command/find_package.html#config-mode-search-procedure

get_filename_component(KURENTO_CMAKE_DIR "${CMAKE_CURRENT_LIST_DIR}/../Kurento" ABSOLUTE)
list(INSERT CMAKE_MODULE_PATH 0 "${KURENTO_CMAKE_DIR}")
list(REMOVE_DUPLICATES CMAKE_MODULE_PATH)
