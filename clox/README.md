## How do this work?

The scanner is responsible for tokenizing the input. It takes the input string and returns a Token struct for each token
it identifies. This Token struct contains a pointer to the start of the lexeme as well as the length of the lexeme.
Interestingly, it doesn't actually copy the string, it just identifies where a token starts in the source string and
how long that token is. That info is encapsulated in the Token struct which the compiler later uses. The scanner
uses a trie for figuring out if an identifier is a keyword or a user identifer.

The compiler is responsible for taking those tokens and writing byte code which will later be evaluated by the vm.
The byte code consists of 8-bit instructions that are appended to a resizable array which will hold the entire program.
In addition to instructions, the compiler also writes values (e.g., numbers, strings) to a separate "numbers" resizable array.
The reason these are stored separately is because of their variable-size. E.g., if you stored them in-line, i.e., right
after the opcode, then the vm would have to look up how long the value is, read it, then increment the instruction pointer
by that amount to hit the next instruction. It complicates matters. 

Opcodes which use those values (like the constant op_code, indicating the source code contained a number literal like 5),
are 2 byte long instructions. The first is the opcode which identifies the instruction, and the second is the operand, aka,
argument. The argument in this case is an index into the value array so the VM will know where to look up the constant
when it's evaluating the program.

The VM is responsible for executing the code. It runs through the byte code and decodes (aka, dispatches) to C code
which is responsible for executing that instruction. E.g., for a constant instruction, the VM reads the offset for
that value from the adjacent operand byte (remember, a constant instruction takes up 2 bytes, one which identifies the
type of instruction, and the second which includes the value array offset), reads the value, and converts it to the proper
C type and pushes that value on onto the stack. The way it knows how to convert it is based on the "type" field of the
Value struct. It's a tagged union. It  tells us whether we're storing a number, string, or something else, in the "value"
union.

Or, as another example, the VM may read an "addition" instruction. In that case, it pops 2 values off the stack, adds them,
and pushes the result back onto the stack. This stack is used to store temporary values while the program is executing.
An expression may, say, add 2 numbers, and then the following expressions may need to divide it. The stack is used
as space to store these values and results during execution.

## Why Use a VM?
It's _almost_ as fast as native code, but still allows you to be far more portable since you can write the VM in C
(which has a compiler already written for it on most platforms). If you decided to write a compiler which executed native
code you'd need to know the exact semantics and instruction codes for a specific ISA (instruction set architecture), aka,
a specific CPU. You could do that, but you would spend a long time optimizing for a single platform. C compilers are ubiquitous
and the bytecode a VMs runs resembles machine code. It’s a dense, linear sequence of binary instructions. That keeps
overhead low and plays nice with the cache. Compare that to the tree-walk-interpreter which was written in Java. That
implementation added lots more layers of abstraction and indirection before the program was run which slowed it down
considerably. E.g., all elements of the AST were distinct Java classes which had references to their children. Storing
those classes, dereferencing the children, and the various calls required by the visitor pattern means that approach
was far, far slower than lower level C code which basically just iterates over an array and calls a function based on
a lookup table for each step.

That emulation layer adds overhead, which is a key reason bytecode is slower than native code. But in return, it gives us
portability. Write our VM in a language like C that is already supported on all the machines we care about, and we can run
our emulator on top of any hardware we like.

This is the path we’ll take with our new interpreter, clox. We’ll follow in the footsteps of the main implementations of
Python, Ruby, Lua, OCaml, Erlang, and others.