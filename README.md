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

### Log files

For each run, two log files are created that contain the results of the type checking:
The first file, `statements.csv` lists all PreparedStatements that are found and if they are supported or not.
The second one, `bindings.csv` lists all the legal or illegal bindings of the parameters and column accesses to the result set.

#### Configuration

By default, the log files are created in the `opslogs` directory withing the project root.
An alternative directory can be specified by setting the `-AopsLogDir' compiler option to the desired path.


#### Log format

The log files are comma-separated files with the following columns:

`statements.csv`:
- `kind`: This is usually either `SUPPORTED_PREPARED_STATMENT` or `UNSUPPORTED_PREPARED_STATEMENT`.
If the statement can only be analysed by the fallback JDBC approach instead of Calcite, it is `USING_FALLBACK`,
and if the `-AenableSqlStringHeuristic` option is used, it can be `USING_SQL_STRING_HEURISTIC`.
- `statementFile`: The file in which the PreparedStatement is declared.
- `statementLine`: The line number of the PreparedStatement declaration.
- `statementColumn`: The column number of the PreparedStatement declaration.
- `details`: Additional information for unsupported statements.

`bindings.csv`:
- `kind`: Either `OK`, `ERROR` or (in the future) `WARNING`.
- `bindingFile`: The file in which the binding (PreparedStatement `setXXX` or ResultSet `getXXX`) call is made.
- `bindingLine`: The line number of the binding.
- `bindingColumn`: The column number of the binding.
- `statementFile`: The file in which the PreparedStatement relating to the binding is declared.
- `statementLine`: The line number of the PreparedStatement declaration.
- `statementColumn`: The column number of the PreparedStatement declaration.
- `key`: The type of binding or error, for example `column.set` or `parameter.type.incompatible`.
- `details`: Additional information for the errors.

In `bindings.csv`, `(statementFile, statementLine, statementColumn)` acts as a foreign key to entries in `statements.csv`.