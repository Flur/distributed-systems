name := "master"

version := "0.1"

scalaVersion := "2.13.3"

val AkkaHttpVersion = "10.2.2"

//libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion

lazy val master = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "master",
    libraryDependencies += "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.10",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor"  % "2.6.10",
    libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
)

lazy val secondary = (project in file("secondary"))
  .enablePlugins(JavaAppPackaging)
  .settings(
  name := "secondary",
    libraryDependencies += "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.10",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor"  % "2.6.10",
    libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
)
