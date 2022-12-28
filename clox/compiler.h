//
// Created by James Sutterfield on 20/12/2022.
//

#ifndef CLOX_COMPILER_H
#define CLOX_COMPILER_H

#include "chunk.h"

bool compile(const char* source, Chunk* chunk);

#endif //CLOX_COMPILER_H
