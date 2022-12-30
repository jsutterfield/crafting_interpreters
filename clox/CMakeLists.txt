cmake_minimum_required(VERSION 3.21)
project(clox C)

set(CMAKE_C_STANDARD 99)

add_executable(clox main.c common.c common.h chunk.c chunk.h memory.c memory.h debug.c debug.h value.c value.h vm.h vm.c compiler.c compiler.h scanner.c scanner.h object.h object.c)
