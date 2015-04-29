# Overcoming Java shortcomings
One believes that the verbosity and the statically typed nature of Java make it hardly suitable for expressing builds.
Jerkar prove the opposite by :
* compiling java build classes on the fly prior to execute them. This step is quick, and make the subsequent steps even faster.
* heavily relying on convention and sensitive defaults : you only need to specify what is not 'standard'
* featuring fluent APIs allowing to express tasks in a very concise way. Jerkar build script concision is close to Gradle (and even more concise in certain cases) 
* not depending on any 3rd party dependency (except Ivy) to avoid version clashing.
* keeping the runtime simple (no bytecode enhancement) for easier debugging 



    
    
        