# Cleanroom-API
This is a simple gradle project which builds and packages a Java API into a docker container

## Vert.x API for RxJava2
This project uses the [Vert.x](https://vertx.io/docs/) event driven application server library.

It uses the [RxJava2 API](https://vertx.io/docs/vertx-rx/java2/) over the callback based one to improve readability.

## Google Cloud Command Line Utils
You can install on macOS with `brew install google-cloud-sdk`

## Google Credentials
1. Run `gcloud auth login` - This will open a browser where you can login to google cloud
2. Run `gcloud auth application-default login` - This will also open a browser where you can login and will set your application default credentials

Once you have completed the above you will now have a credential file at:
`~/.config/gcloud/application_default_credentials.json`

## Creating BQ resources
Creating a dataset:
```shell script
bq mk -d test_dataset
```

Creating a simple table:
```shell script
bq mk --schema=source/cleanroom-api/src/test/resources/simple_table.json test_dataset.simple_table
```

## Building the Docker Image
In the root folder of this repository run: `./gradlew docker`

Gradle will build both the Application Jar and the Docker image

## Running the API Locally
In the root folder of this repository run: `./gradlew dockerRun`

This will run the API against the GCP project defined in gradle.properties using the application default credentials you created above.
* The gradle build sets the `GOOGLE_CLOUD_PROJECT` environment variable
* Your `~/.config/gcloud` directory is mounted as a volume in the docker container
* The gradle build sets the `GOOGLE_APPLICATION_CREDENTIALS` environment variable to point at the json file in the mounted volume

## Validating a query against a BQ Table
The following curl command will populate a catalog from the bigquery API and resolve a query against it.

```shell script
curl -XPOST -d @source/cleanroom-api/src/test/resources/payload.json localhost:8080/cleanroom/query
```

The next curl command fails:

```shell script
curl -XPOST -d @source/cleanroom-api/src/test/resources/payload2.json localhost:8080/cleanroom/query
```

With the following error:

```text
Caused by: com.google.zetasql.io.grpc.StatusRuntimeException: INVALID_ARGUMENT: Syntax error: Unexpected keyword SELECT [at 1:33]
        at com.google.zetasql.io.grpc.stub.ClientCalls.toStatusRuntimeException(ClientCalls.java:233)
        at com.google.zetasql.io.grpc.stub.ClientCalls.getUnchecked(ClientCalls.java:214)
        at com.google.zetasql.io.grpc.stub.ClientCalls.blockingUnaryCall(ClientCalls.java:139)
        at com.google.zetasql.ZetaSqlLocalServiceGrpc$ZetaSqlLocalServiceBlockingStub.extractTableNamesFromStatement(ZetaSqlLocalServiceGrpc.java:1077)
        at com.google.zetasql.Analyzer.extractTableNamesFromStatement(Analyzer.java:117)
        ... 45 more
```

## The Problem
[Analyzer.extractTableNamesFromStatement](src/main/java/com/videoamp/cleanroom/queryanalyzer/HelloWorld.java#L50) does not handle multiple statements.

However [Analyzer.analyzeNextStatement](https://github.com/google/zetasql/blob/cc1e20d6c800daf3270b5dee07fbf9315409e3fa/java/com/google/zetasql/Analyzer.java#L147-L177) is capable of analyzing multiple statements.

Can you extend the analyzer so it supports multiple statements and get the second curl command to work?