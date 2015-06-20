val commonSettings = Seq(
  organization := "com.excursion",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.6",
  fork in run := true,
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-encoding",
    "utf8"
  )
)

lazy val client =
  project.
  enablePlugins(ScalaJSPlugin).
  settings(commonSettings: _*).
  settings(
    persistLauncher in Compile := true,
    persistLauncher in Test := false,
    testFrameworks += new TestFramework("utest.runner.Framework"),
    jsDependencies += "org.webjars" % "react" % "0.12.2" / "react-with-addons.js" commonJSName "React",
    libraryDependencies ++= Seq(
      "org.scala-js"                      %%% "scalajs-dom"   % "0.8.0",
      "me.chrons"                         %%% "boopickle"     % "0.1.4",
      "com.github.japgolly.scalajs-react" %%% "core"          % "0.9.0",
      "com.github.japgolly.scalajs-react" %%% "extra"         % "0.9.0",
      "com.lihaoyi"                       %%% "utest"         % "0.3.0"  % "test"
    ),
    addCompilerPlugin(compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full))
  )

lazy val server =
  project.
  dependsOn(client).
  settings(commonSettings: _*).
//  settings(workbenchSettings: _*).
  settings(Revolver.settings: _*).
  settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http-experimental" % "1.0-RC3",
      "com.lihaoyi"       %% "scalatags"              % "0.5.2"
    )
  ).
  aggregate(client)
