ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.1"


lazy val `pekko-processors-sagas` = (project in file("pekko-processors-sagas"))
  .settings(
    name := "es-processors-sagas",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test
  )

lazy val `es-processors-sagas` = (project in file("."))
  .settings(
    ThisBuild / organization            := "weemen",
    ThisBuild / dynverSonatypeSnapshots := true,
    publish / skip                      := true
  )
  .aggregate(
    `pekko-processors-sagas`
  )

/*

 */
