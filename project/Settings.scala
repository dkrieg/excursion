import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt._

object Settings {
  import Def._

  val name = "excursion"
  val version = "0.1.0-SNAPSHOT"

  val scalacOptions = Seq(
    "-Xlint",
    "-unchecked",
    "-deprecation",
    "-feature"
  )
  val jvmRuntimeOptions = Seq("-Xmx1G")

  object versions {
    val scala = "2.11.6"
    val scalajsReact = "0.9.0"
    val scalaCSS = "0.2.0"
    val react = "0.12.1"
    val jQuery = "1.11.1"
    val bootstrap = "3.3.2"
    val chartjs = "1.0.1"
    val log4js = "1.4.10"
    val akkaHttp = "1.0-RC3"
    val scalatags = "0.5.2"
    val scalajsDom = "0.8.0"
    val scalaRx = "0.2.8"
    val scalaz = "7.1.3"
  }

  val sharedDependencies = setting(Seq(
    "com.lihaoyi" %%% "autowire"     % "0.2.5",
    "me.chrons"   %%% "boopickle"    % "0.1.3",
    "com.lihaoyi" %%% "utest"        % "0.3.1",
    "org.webjars"   % "font-awesome" % "4.3.0-1" % Provided,
    "org.webjars"   % "bootstrap"    % versions.bootstrap % Provided
  ))

  val serverDependencies = setting(Seq(
    "com.typesafe.akka" %% "akka-http-experimental" % versions.akkaHttp,
    "org.scalaz"        %% "scalaz-core"            % versions.scalaz,
    "com.lihaoyi"       %% "scalatags"              % versions.scalatags
  ))

  val clientDependencies = setting(Seq(
    "com.github.japgolly.scalajs-react" %%% "core"        % versions.scalajsReact,
    "com.github.japgolly.scalajs-react" %%% "extra"       % versions.scalajsReact,
    "com.github.japgolly.scalacss"      %%% "ext-react"   % versions.scalaCSS,
    "org.scala-js"                      %%% "scalajs-dom" % versions.scalajsDom,
    "com.lihaoyi"                       %%% "scalarx"     % versions.scalaRx
  ))

  val jsDependencies = setting(Seq(
    "org.webjars" % "react"          % versions.react     / "react-with-addons.js" commonJSName "React",
    "org.webjars" % "jquery"         % versions.jQuery    / "jquery.js",
    "org.webjars" % "bootstrap"      % versions.bootstrap / "bootstrap.js" dependsOn "jquery.js",
    "org.webjars" % "chartjs"        % versions.chartjs   / "Chart.js",
    "org.webjars" % "log4javascript" % versions.log4js    / "js/log4javascript_uncompressed.js"
  ))
  val jsDependenciesMin = setting(Seq(
    "org.webjars" % "react"          % versions.react     / "react-with-addons.min.js" commonJSName "React",
    "org.webjars" % "jquery"         % versions.jQuery    / "jquery.min.js",
    "org.webjars" % "bootstrap"      % versions.bootstrap / "bootstrap.min.js" dependsOn "jquery.min.js",
    "org.webjars" % "chartjs"        % versions.chartjs   / "Chart.min.js",
    "org.webjars" % "log4javascript" % versions.log4js    / "js/log4javascript.js"
  ))

}