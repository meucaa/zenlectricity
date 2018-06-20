name := """zenlectricity"""
organization := "com.meucaa"

version := "0.1-alpha"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.6"

scalacOptions ++= Seq(
  "-Ywarn-unused-import",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:patvars",
  "-Ywarn-unused:privates"
)

routesImport := Seq.empty

evictionWarningOptions in update := EvictionWarningOptions.default
                                                          .withWarnTransitiveEvictions(false)
                                                          .withWarnDirectEvictions(false)

libraryDependencies ++= Seq(
  guice,
  "com.typesafe.play" %% "play-slick" % "3.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.1",
  "mysql" % "mysql-connector-java" % "8.0.11",
  "com.github.t3hnar" %% "scala-bcrypt" % "3.1",
  "com.pauldijou" %% "jwt-play-json" % "0.16.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test,
  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test
)
