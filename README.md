# Optional PreparedStatement Checker (OPSC)

## Usage

### Build with tests (using gradle)

* Setup the test postgres database with the chinook schema:
  * Create a postgres docker container with
  ```shell
  sudo docker run --rm --name opsc-postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=chinook -p 5432:5432 -d postgres
  ```
  * Run the schema creation script with
  ```shell
  cat tests/Chinook_Postgresql_schema.sql | sudo docker exec -i -u postgres opsc-postgres psql -d chinook
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

This requires that you built the EISOP Framework in `../checker-framework/`.