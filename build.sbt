ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.8.1"

lazy val `pekko-processors-sagas` = (project in file("pekko-processors-sagas"))
  .settings(
    name := "es-processors-sagas",
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % "1.1.2",
      "org.apache.pekko" %% "pekko-actor"       % "1.1.2",
      "org.apache.pekko" %% "pekko-stream"      % "1.1.2",
      "org.apache.pekko" %% "pekko-slf4j"       % "1.1.2",
      "ch.qos.logback"    % "logback-classic"   % "1.5.16",
      "org.apache.pekko" %% "pekko-testkit"             % "1.1.2"  % Test,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % "1.1.2"  % Test,
      "org.scalatest"    %% "scalatest"                 % "3.2.19" % Test,
      "org.typelevel"    %% "cats-core"         % "2.13.0"
    )
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
