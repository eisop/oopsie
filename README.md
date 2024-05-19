# Optional PreparedStatement Checker (OPSC)

OPSC is a type-checker for Java using the [Checker Framework](https://checkerframework.org/).
It check if JDBC PreparedStatements are used correctly by
- Checking if the correct number of parameters (`?`s in the query) are set with the correct types
- Checking if the result set columns are read into the correct Java types (by prohibiting the use of the `getString` on an integer column, for example)
- The checker considers further details of the type such as nullability or VARCHAR length (WIP)

As a pluggable type checker for Java, OPSC performs the checks during compile time and can prevent many SQLExceptions
stemming from bugs that would otherwise only be detected at runtime.

## Usage

### Build with tests (using gradle)

* Set up the test postgres database with the chinook schema:
```shell
cd tests
docker-compose up
```

* Assemble and test with `./gradlew test`.


* Apply formatting with `./gradlew spotlessApply`.

* Run `./gradlew publishToMavenLocal` to publish to your local Maven repository.
  The OPSC dependency should now be available to local Gradle projects that have declared the mavenLocal() repository:
  ```groovy
  repositories {
      mavenLocal()
  }

  dependencies {
      compileOnly "io.github.eisop:opsc:0.0.1-SNAPSHOT"
      checkerFramework "io.github.eisop:opsc:0.0.1-SNAPSHOT"
  }
  ```

### To manually run against a file (without gradle):

````
../checker-framework/checker/bin/javac -classpath ./build/classes/java/main/ -processor io.github.eisop.opsc.OpsChecker tests/opsc/Tiny.java
````

This requires that you built the EISOP Framework in `../checker-framework/`:
https://eisop.github.io/cf/manual/manual.html#installation
