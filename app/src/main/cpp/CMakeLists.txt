cmake_minimum_required(VERSION 3.22.1)
project("wirelessmic_native")

add_library(
        wirelessmic_native
        SHARED
        native-gain.cpp
)

find_library(log-lib log)

target_link_libraries(
        wirelessmic_native
        ${log-lib}
)

target_compile_options(wirelessmic_native PRIVATE -O2 -ffast-math)
