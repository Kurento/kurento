# CMake Config Mode search file. Adds the main Kurento dir to CMAKE_MODULE_PATH.
# https://cmake.org/cmake/help/latest/command/find_package.html#config-mode-search-procedure

list(INSERT CMAKE_MODULE_PATH 0 "${CMAKE_CURRENT_LIST_DIR}/../Kurento")
