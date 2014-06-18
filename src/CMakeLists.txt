set (SOURCE_FILES JsonRpcHandler.cpp JsonRpcUtils.cpp)
set (HEADER_FILES JsonRpcHandler.hpp JsonRpcException.hpp JsonSerializer.hpp
      JsonRpcUtils.hpp JsonRpcConstants.hpp)

include_directories(${CMAKE_CURRENT_SOURCE_DIR})

add_library(jsonrpc SHARED ${SOURCE_FILES} ${HEADER_FILES})

set_target_properties(jsonrpc PROPERTIES PUBLIC_HEADER "${HEADER_FILES}")

include_directories(jsonrpc ${JSONCPP_INCLUDE_DIRS} ${Boost_INCLUDE_DIRS})
target_link_libraries(jsonrpc ${JSONCPP_LIBRARIES} ${Boost_LIBRARIES})

set_target_properties(jsonrpc PROPERTIES VERSION ${PROJECT_VERSION} SOVERSION ${PROJECT_VERSION_MAJOR})

install(
  TARGETS jsonrpc
  RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
  LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
  ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
  PUBLIC_HEADER DESTINATION ${INCLUDE_PREFIX}
)
