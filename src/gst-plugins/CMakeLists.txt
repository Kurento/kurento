#We should avoid this fixing the code
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror -Wall")

add_subdirectory(facedetector)
add_subdirectory(faceoverlay)
add_subdirectory(imageoverlay)
add_subdirectory(movementdetector)
