# Optional PreparedStatement Checker (OPSC)

## Usage

To manually run against a file:

````
../checker-framework/checker/bin/javac -classpath ./build/classes/java/main/ -processor io.github.eisop.opsc.OpsChecker tests/opsc/Tiny.java
````

This requires that you built the EISOP Framework in `../checker-framework/`.

## Developer notes

* Prepare the test database by setting up a PostgreSQL database with the schema from the files in the ddl directory.
Currently, dir/invoice.sql is the only file that is required.

* Change the database URL and credential constants in test/java/tests/OpscTest.java to match your database.
  (These constants will be moved out of the test class in the future.)

* Assemble and test with `./gradlew test`.

* Apply formatting with `./gradlew spotlessApply`.

* Run `./gradlew publishToMavenLocal` to publish to your local Maven repository.

* TODO: document how to use a Maven(Local) release.
