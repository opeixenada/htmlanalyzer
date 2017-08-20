name := "htmlanalysis"

version := "1.0"

scalaVersion in ThisBuild := "2.12.3"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  guice,
  ws,
  "org.webjars" % "bootstrap" % "4.0.0-beta",
  "org.jsoup" % "jsoup" % "1.10.3",
  "org.scalatestplus.play" % "scalatestplus-play_2.12" % "3.1.1" % "test")
