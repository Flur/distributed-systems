name := "master"

version := "0.1"

scalaVersion := "2.13.3"

lazy val master = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "master",
    libraryDependencies += "com.twitter" %% "finagle-http" % "20.10.0",
  )

lazy val secondary = (project in file("secondary"))
  .enablePlugins(JavaAppPackaging)
  .settings(
  name := "secondary",
  libraryDependencies += "com.twitter" %% "finagle-http" % "20.10.0",
)
