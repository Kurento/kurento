#We should avoid this fixing the code
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=gnu99")

add_subdirectory(chroma)
add_subdirectory(crowddetector)
add_subdirectory(facedetector)
add_subdirectory(faceoverlay)
add_subdirectory(h264filter)
add_subdirectory(imageoverlay)
add_subdirectory(jackvader)
add_subdirectory(pointerdetector)
add_subdirectory(pointerdetector2)
add_subdirectory(platedetector)
