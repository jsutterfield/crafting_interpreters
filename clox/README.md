## How do this work?

Clox uses a single-pass compiler which emits bytecode as soon as it's finished parsing a bit of syntax it understands.
All of this bytecode is then later run by a VM which uses a stack to hold the temporary values created and used by expressions.

### Scanner
The scanner is responsible for tokenizing the input. It takes the input string and returns a Token struct for each token
it identifies. This Token struct contains a pointer to the start of the lexeme as well as the length of the lexeme.
Interestingly, it doesn't actually copy the string, it just identifies where a token starts in the source string and
how long that token is. That info is encapsulated in the Token struct which the compiler later uses. The scanner
uses a trie for figuring out if an identifier is a keyword or a user identifier.

### Compiler
The compiler is responsible for taking those tokens and writing byte code which will later be evaluated by the vm.
The byte code consists of 8-bit instructions that are appended to a resizable array which will hold the entire program.
In addition to instructions, the compiler also writes values (e.g., numbers, strings) to a separate "values" resizable array.
The reason these are stored separately is because of their variable-size. E.g., if you stored them in-line, i.e., right
after the opcode, then the vm would have to look up how long the value is, read it, then increment the instruction pointer
by that amount to hit the next instruction. It complicates matters. 

Opcodes which use those values (like the constant op_code, indicating the source code contained a number literal like 5),
are 2 byte long instructions. The first is the opcode which identifies the instruction, and the second is the operand, aka,
argument. The operand in this case is an index into the value array so the VM will know where to look up the constant
when it's evaluating the program.

In addition to smaller constants, like numbers, bools, and nil, which can be stored directly in the constant array, larger
constants like strings are stored indirectly. This is accomplished by allocating space for the string in the heap, copying
the string value to the heap, and then storing an "Obj" struct in the constant array which contains a pointer to that string.
All constant values are encapsulated in a value array. It's a "tagged union" which means the first field indicates what kind
of value it is and then a union which holds the different value types. Based on the type, we use a macro to cast the value
to the proper type.

### VM
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
as space to store these values and results during execution of the program.

This stack is used as temporary space during the programs execution for expressions to write to/read from. A bit unintuitively,
you write the operand bytecode before you write the instruction bytecode because of how the stack will be use. E.g., for
the addition example, `1 + 2` would be written as `<constantValue1><constantValue2><AddOp>` in our bytecode since the VM
will first push value 1 and value 2 on the stack, then add will pop both off, add them together, and push the result back
on.

### Strings
Strings are stored in the heap and referred to via pointers found in StringOjb structs. When we create the string, we
calculate a hash based on its characters which will be used for storing the string in hash maps, as well as for quick
equality comparisons between 2 strings.

To save on memory, and to ensure that 2 separate string objects are considered equal when compared with `==` we "intern"
the strings, meaning all strings are stored in a central hashmap. When the compiler or vm goes to create a new string it
first checks if the string already exists in the internedStrings table. If so, it just returns a reference to that entry
so the string is re-used. Otherwise, it creates the string and stores it in the internedStrings table.

### Global Variables
Global variables are resolved by name at runtime. When they're first defined we add a new string constant (which, again,
involves allocating the string on the heap, storing the StringObject in the constant array, and then returning the offset
of the index in the constant array where it was stored) and then save the constant offset as the operand for the define/get/set
global variable instruction. The actual value of the variable is stored in a separate globalVariables hashmap.

When we get/set the global variable, we look up the variable name using the constant array offset we have, then take that name
and use it as the key for the globalVariables hashmap. In the case of a Get we push the value onto the stack.

This isn't the fastest solution (since we're doing a few steps to figure out the global var name and then look in the 
globalVariables hashmap), but global vars shouldn't be access _too_ much, and it's relatively easy to follow.

These variables are known as "late bound", meaning they're looked up at run time (via all the steps mentioned above). It's
good for keeping the compiler simple, but bad for performance.

### Local Variables
Unlike global variables, which are resolved during runtime, local variables are resolved during _compile time_ via lexical
scoping. That means we can determine what the value is at compile time and all local vars need to be declared and defined
before they can be used in expressions. Local variables are used often and if they're slow, the whole vm is slow. B/c they can be
resolved via lexical scoping, we can store/use them on the stack, rather than in a separate hash table. Here's how it works:

First, a couple notes on different parts of grammar: An expression produces a _value_ and a statement produces an _effect_. E.g.,
constants like `5` are expressions, they produce the value 5. Binary operations are expressions. `1 + 2` is an expression
which produces the value `3`. In our implementation of clox, "producing a value" means "pushing it onto the stack".
*Each expression leaves one result value on the stack.*

Statements on the other hand result in side effects. These could be binding a value to a variable, calling something, or
printing a value. *Each statement leaves the stack, at the end, unchanged.*

Finally, there's a hybrid called an "expression statement." This is an expression which is used in a place where a statement
is expected. The value which it produces is removed as soon as the statement finishes. A semicolon turns an expression into
an expression statement. `3 + 5;` pushes the values 3 and 5 onto the stack, the binary operator pops those 2 values, adds
them together, and pushes 8 onto the stack, and finally the semicolon results in the value being popped off the stack.

Okay, with that background, here's the grammar for how a variable is created via a "variable declaration":

`varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;`

A variable declaration is composed of an identifier (i.e, a string which is not a reserved keyword), and then optionally
an expression statement for the value. Global variables are any variables defined in the top scope, local variables are
everything else (i.e., basically anything between a set of `{}`).

When we're parsing the source code and come across a block (`i.e.` an open `{`) then we know any variable we see is going
to be a local variable. We keep track of what scope we're in via a field in a global `Compiler` struct. When we're in a local
scope and come across a variable declaration, we first parse the expression which results in a value being pushed onto the
stack. If there's no expression statement, then we push the constant value `nil` onto the stack. For other expression
statements, this value would then be immediately popped off the stack, but for variable declarations we leave it there
(i.e., we don't emit an `OP_POP` instruction like we would for other expression statements). We then add an entry for this
local variable in an array of `Local` structs also found in the `Compiler` struct. Each `Local` reports its name and the
current scope which is what we'll use later when resolving these local variables.

Later, when we need to get or set this local variable, we'll look up where the local variable is on the stack via this
`locals` array. That will give us it's offset on the stack and then we can use that offset to either overwrite the value
(in the case of a set) or copy that value onto the top of a stack (in the case of a get).

Here's the code and stack disassembly for a small code sample with local variables:
```text
{
    var x = 5;
    print x;
    x = 7;
    print x;
}

== code ==
0000    2 OP_CONSTANT         0 '5'
0002    3 OP_GET_LOCAL        0
0004    | OP_PRINT
0005    4 OP_CONSTANT         1 '7'
0007    | OP_SET_LOCAL        0
0009    | OP_POP
0010    5 OP_GET_LOCAL        0
0012    | OP_PRINT
0013    6 OP_POP
0014    | OP_RETURN
          
0000    2 OP_CONSTANT         0 '5'
          [ 5 ]
0002    3 OP_GET_LOCAL        0
          [ 5 ][ 5 ]
0004    | OP_PRINT
5
          [ 5 ]
0005    4 OP_CONSTANT         1 '7'
          [ 5 ][ 7 ]
0007    | OP_SET_LOCAL        0
          [ 7 ][ 7 ]
0009    | OP_POP
          [ 7 ]
0010    5 OP_GET_LOCAL        0
          [ 7 ][ 7 ]
0012    | OP_PRINT
7
          [ 7 ]
0013    6 OP_POP
          
0014    | OP_RETURN


```

When we leave a scope (i.e., we parse a closing `}`) we pop all the local variable declared in that scope. It's like what
the `;` does for normal expression statements.