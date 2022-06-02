# minijava-typechecking

## Description
The aim of this project is to perform type-checking on a simplified version of Java, called mini java. A parser is created automatically using the grammar, which is supplied in BNF and Java CC form. Using the above, JTB creates a number of default visitors. We expand upon one of them, specifically GJDepthFirst. Two different classes are created, expanding on GJDepthFirst: VisitorPhase1 and VisitorPhase2. These are called in order, one to create the symbol table and perform some basic checking, and the second one to perform the main body of checks. The two visitors and the symbol table will be explained bellow. After the checks are performed, the program is translated to LLVM-IR form, using a third visitor, VisitorPhase3. This visitor, using the Symbol Table as well as the offsets calculated by the first visitor, translates the given program, producing a.ll file, that can be compiled using clang. This visitor and the way it works will be explained further bellow.

### VisitorPhase1
The first custom visitor class aims to visit every node of the tree and collect information regarding the names and identifiers of various items, like classes, methods and variables. The information of these items is stored in the symbol table. In addition, some basic checks are performed, mainly regarding the redeclaration of classes or methods, or the bad use of the "class extends" statement.

### VisitorPhase2
The second custom visitor class aims to perform most of the type-checking tasks, using the symbol table created in by the first visitor. It cross-references the correctness of method calls, declarations, assignment statements etc. by checking the type of various objects, information stored in the symbol table. It also performs checks to establish the use of inherited fields or methods for classes.

### VisitorPhase3
The third visitor will translate any programs that passed all the checks of the first two visitors into LLVM-IR form. In order to do this, the third visitor takes the symbol table calculated by the first visitor as input. This symbol table is used to retrieve information regarding the type of fields and variables, method declarations, maters of inheritance and other information. Furthermore, the symbol table stores information on the size of class objects in the form of offsets of their fields. The offsets of the fields are used to calculate the amount of space to allocate when creating class objects or to access the fields. The offsets of the methods are used when creating the V-Table, or calling a method. Further checks are performed during run time, in order to avoid access to out-of-bounds memory in array operations. In order to translate the input program, the visitor accumulates the result of each node in the form of a string, which is returned to the main function of the program. This output string if formatted in such a way as to make it easier to read, featuring appropriate indentation and spacing. In order to accomplish the above, various helper functions are used, to retrieve and/or calculate the offsets of field or methods, load variables or fields into registers etc.

### The symbol table
The symbol class is symbolized by a MAP of classMap objects. Each objects of this class includes information regarding one class of the program, including the class it inherits from, if any, its name, its fields and its methods. The information regarding methods and fields is stored in a map of Method class and Variable class objects respectively.

- The method class includes information on the name, return type, class, formal parameters and variables included in the method
- The variable class includes information regarding the name and type of the variable (or field, they are treated in the same way)

Both of the above classes feature the functions needed to access or modify their values.

### The main function
The main function loops through the given list of files, checking them one by one. If an error is found, an exception is caught by the main function, and the execution continues with the next file. For each file, the main function prints information regarding its name and the name offset of its methods.

The program is created using `make` and run using `Java Main [inputFiles]`
