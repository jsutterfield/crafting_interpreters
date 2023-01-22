#ifndef clox_debug_h
#define clox_debug_h

#include "chunk.h"

void printValues(ValueArray values, const char *name);
void disassembleChunk(Chunk* chunk, const char* name, const uint8_t* ip);
int disassembleInstruction(Chunk* chunk, int offset, const uint8_t* ip);

#endif