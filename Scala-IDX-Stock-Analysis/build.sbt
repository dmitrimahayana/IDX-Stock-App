ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.17"

lazy val root = (project in file("."))
  .settings(
    name := "Scala-IDX-Stock-Analysis"
  )

// If you're running from inside Intellij IDEA, and you've marked your spark library as "provided"
// like so: "org.apache.spark" %% "spark-sql" % "3.0.1" % "provided",
// Then you need edit your Run/Debug configuration and check the "Include dependencies with Provided scope" box.
//NOTED!!! YOU NEED TO RUN ML PROCESS VIA spark-submit

// https://mvnrepository.com/artifact/org.apache.spark/spark-core
libraryDependencies += "org.apache.spark" %% "spark-core" % "3.4.1"
//libraryDependencies += "org.apache.spark" %% "spark-core" % "3.3.1"

  // https://mvnrepository.com/artifact/org.apache.spark/spark-sql
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.4.1" % "provided"
//libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.3.1"

// https://mvnrepository.com/artifact/org.apache.spark/spark-mllib
libraryDependencies += "org.apache.spark" %% "spark-mllib" % "3.4.1" % "provided"
//libraryDependencies += "org.apache.spark" %% "spark-mllib" % "3.3.1"

  // https://mvnrepository.com/artifact/org.mongodb.spark/mongo-spark-connector
libraryDependencies += "org.mongodb.spark" %% "mongo-spark-connector" % "10.2.0"

// https://mvnrepository.com/artifact/org.mongodb/mongodb-driver-sync
libraryDependencies += "org.mongodb" % "mongodb-driver-sync" % "4.10.1"

val AkkaVersion = "2.7.0"
val AkkaHttpVersion = "10.5.2"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion
)


val json4sVersion = "3.6.6"
// https://mvnrepository.com/artifact/org.json4s/json4s-native
libraryDependencies += "org.json4s" %% "json4s-native" % json4sVersion
// https://mvnrepository.com/artifact/org.json4s/json4s-ast
libraryDependencies += "org.json4s" %% "json4s-ast" % json4sVersion
// https://mvnrepository.com/artifact/org.json4s/json4s-core
libraryDependencies += "org.json4s" %% "json4s-core" % json4sVersion
// https://mvnrepository.com/artifact/org.json4s/json4s-jackson
libraryDependencies += "org.json4s" %% "json4s-jackson" % json4sVersion
// https://mvnrepository.com/artifact/org.json4s/json4s-scalap
libraryDependencies += "org.json4s" %% "json4s-scalap" % json4sVersion

libraryDependencies += "ch.megard" %% "akka-http-cors" % "1.2.0"

// https://mvnrepository.com/artifact/org.apache.spark/spark-sql-kafka-0-10
libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.4.1" % Test

//artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
//  artifact.name + "_" + sv.binary +  "_" + module.revision + "." + artifact.extension
//}