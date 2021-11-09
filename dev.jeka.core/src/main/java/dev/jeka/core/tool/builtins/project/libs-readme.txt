This folder contains libraries (jar files) that are included in projects when
project.construction#addTextAndLocalDependencies() is invoked.

Libraries are added for appropriate usage according the folder they are contained in.

compile+runtime : stands for dependencies used for compilation and for runtime as well. This is the most common place to declare dependencies.
compile : stands for dependencies used for compilation but not for runtime
runtime : stands for dependencies used for runtime but not for compilation.
           Runtime dependencies has impact when creating fat jars and for the definition of published dependencies.
test    : stands for dependencies used only for testing.
sources : stands for jar sources of libs declared above.

If you don't need this, you can delete safely this folder.