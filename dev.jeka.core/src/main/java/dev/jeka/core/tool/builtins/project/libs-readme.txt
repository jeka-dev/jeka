This folder contains libraries (jar files) to be included in project dependencies.

Libraries are added for appropriate usage according the folder they are contained in.

compile+runtime : dependencies used for compilation and for runtime as well. This is the most common place to declare dependencies.
compile : dependencies used for compilation but not for runtime
runtime : dependencies used for runtime but not for compilation.
           Runtime dependencies has impact when creating fat jars and for the definition of published dependencies.
test    : dependencies used only for testing.
sources : jar sources of libs declared above.

If you don't need this folder, feel free to remove it.