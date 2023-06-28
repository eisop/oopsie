# Optional PreparedStatement Checker (OPSC)

## Usage

To manually run against a file:

````
../checker-framework/checker/bin/javac -classpath ./build/classes/java/main/ -processor io.github.eisop.opsc.OpsChecker tests/opsc/Tiny.java
````

This requires that you built the EISOP Framework in `../checker-framework/`.

## Developer notes

Assemble and test with `./gradlew test`.

Apply formatting with `./gradlew spotlessApply`.

Run `./gradlew publishToMavenLocal` to publish to your local Maven repository.

TODO: document how to use a Maven(Local) release.
