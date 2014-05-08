#We should avoid this fixing the code
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=gnu99")

add_subdirectory(chroma)
add_subdirectory(crowddetector)
