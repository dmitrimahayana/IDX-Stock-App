ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "2.12.15"

lazy val root = (project in file("."))
  .settings(
    name := "Scala_Amazon_ETL"
  )

// If you're running from inside Intellij IDEA, and you've marked your spark library as "provided"
// like so: "org.apache.spark" %% "spark-sql" % "3.0.1" % "provided",
// Then you need edit your Run/Debug configuration and check the "Include dependencies with Provided scope" box.

//NOTED!!! YOU NEED TO RUN ML PROCESS VIA spark-submit

// https://mvnrepository.com/artifact/org.apache.spark/spark-core
libraryDependencies += "org.apache.spark" %% "spark-core" % "3.4.0"

// https://mvnrepository.com/artifact/org.apache.spark/spark-sql
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.4.0" % "provided"

// https://mvnrepository.com/artifact/org.apache.spark/spark-mllib
libraryDependencies += "org.apache.spark" %% "spark-mllib" % "3.4.0" % "provided"

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  artifact.name + "_" + sv.binary +  "_" + module.revision + "." + artifact.extension
}