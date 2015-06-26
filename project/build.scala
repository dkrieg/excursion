import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.less.Import._
import com.typesafe.sbt.packager.archetypes._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.SbtWeb
import net.virtualvoid.sbt.graph.Plugin.graphSettings
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin._
import com.lihaoyi.workbench.Plugin._

object ExcursionBuild extends Build {
  val productionBuild = settingKey[Boolean]("Build for production")
  val elideOptions = settingKey[Seq[String]]("Set limit for elidable functions")
  val scalaJsOutputDir = settingKey[File]("directory for javascript files output by scalajs")
  val copyWebJarResources = taskKey[Unit]("Copy resources from WebJars")
  val sharedSrcDir = "shared"

  lazy val excursion = project.in(file(".")).
    aggregate(client, server).
    settings(
      name := "excursion",
      version := Settings.version,
      commands += ReleaseCmd,
      publish := {},
      publishLocal := {}
    )

  lazy val shared =
    crossProject.
    in(file(".")).
    settings(graphSettings: _*).
    settings(
      name := Settings.name,
      version := Settings.version,
      scalaVersion := Settings.versions.scala,
      scalacOptions ++= Settings.scalacOptions,
      sourceDirectory in Assets := baseDirectory.value / "src" / "main" / "assets",
      LessKeys.compress in Assets := true,
      libraryDependencies ++= Settings.sharedDependencies.value,
      copyWebJarResources := {
        // copy the compiled CSS
        val s = streams.value
        s.log("Copying webjar resources")
        val compiledCss = webTarget.value / "less" / "main" / "stylesheets"
        val targetDir = (classDirectory in Compile).value / "web"
        IO.createDirectory(targetDir / "stylesheets")
        IO.copyDirectory(compiledCss, targetDir / "stylesheets")
        // copy font-awesome fonts from WebJar
        val fonts = (webModuleDirectory in Assets).value / "webjars" / "lib" / "font-awesome" / "fonts"
        IO.createDirectory(targetDir / "fonts")
        IO.copyDirectory(fonts, targetDir / "fonts")
      },
      copyWebJarResources <<= copyWebJarResources dependsOn(compile in Compile, assets in Compile),
      managedResources in Compile <<= (managedResources in Compile) dependsOn copyWebJarResources
    ).
    jvmSettings(Revolver.settings: _*).
    jvmSettings(
      libraryDependencies ++= Settings.serverDependencies.value,
      unmanagedResourceDirectories in Compile += file(".") / sharedSrcDir / "src" / "main" / "resources",
      unmanagedResourceDirectories in Test += file(".") / sharedSrcDir / "src" / "test" / "resources",
      javaOptions in Revolver.reStart ++= Settings.jvmRuntimeOptions,
      Revolver.enableDebugging(port = 5111, suspend = false)
    ).
    jsSettings(workbenchSettings: _*).
    jsSettings(
      name := "excursion-client",
      libraryDependencies ++= Settings.clientDependencies.value,
      productionBuild := false,
      elideOptions := Seq(),
      scalacOptions ++= elideOptions.value,
      jsDependencies ++= {if (!productionBuild.value) Settings.jsDependencies.value else Settings.jsDependenciesMin.value},
      jsDependencies += RuntimeDOM % "test",
      scalacOptions ++= Seq({
        val a = client.base.toURI.toString.replaceFirst("[^/]+/?$", "")
        s"-P:scalajs:mapSourceURI:$a->/srcmaps/"
      }),
      skip in packageJSDependencies := false,
      unmanagedResourceDirectories in Compile += file(".") / sharedSrcDir / "src" / "main" / "resources",
      testFrameworks += new TestFramework("utest.runner.Framework"),
      bootSnippet := "bootstrap()",
      refreshBrowsers <<= refreshBrowsers.triggeredBy(fastOptJS in Compile)
    )

  lazy val js2jvmSettings = Seq(fastOptJS, fullOptJS, packageJSDependencies) map { packageJSKey =>
    crossTarget in(client, Compile, packageJSKey) := scalaJsOutputDir.value
  }

  lazy val client: Project = shared.js.
    settings(
      fastOptJS in Compile := {
        val base = (fastOptJS in Compile).value
        IO.copyFile(base.data, (classDirectory in Compile).value / "web" / "js" / base.data.getName)
        IO.copyFile(base.data, (classDirectory in Compile).value / "web" / "js" / (base.data.getName + ".map"))
        base
      },
      packageJSDependencies in Compile := {
        val base = (packageJSDependencies in Compile).value
        IO.copyFile(base, (classDirectory in Compile).value / "web" / "js" / base.getName)
        base
      }
    ).
    enablePlugins(SbtWeb)

  lazy val server: Project = shared.jvm.
    settings(js2jvmSettings: _*).
    settings(
      scalaJsOutputDir := (classDirectory in Compile).value / "web" / "js",
      NativePackagerKeys.batScriptExtraDefines += "set PRODUCTION_MODE=true",
      NativePackagerKeys.bashScriptExtraDefines += "export PRODUCTION_MODE=true",
      Revolver.reStart <<= Revolver.reStart dependsOn (fastOptJS in(client, Compile)),
      dockerBaseImage := "java:8",
      dockerExposedPorts := Seq(8080),
      compile in Compile <<= (compile in Compile) dependsOn (fastOptJS in(client, Compile)),
      resourceGenerators in Compile += Def.task {
        val files = ((crossTarget in(client, Compile)).value ** "*.js").get
        val mappings: Seq[(File,String)] = files pair rebase((crossTarget in(client, Compile)).value, ((resourceManaged in  Compile).value / "assets/").getAbsolutePath )
        val map: Seq[(File, File)] = mappings.map { case (s, t) => (s, file(t))}
        IO.copy(map).toSeq
      }.taskValue
    ).
    enablePlugins(SbtWeb, JavaAppPackaging)

  val ReleaseCmd = Command.command("release") {
    state =>
      "set productionBuild in client := true" ::
      "set elideOptions in client := Seq(\"-Xelide-below\", \"WARNING\")" ::
      "sharedJS/test" ::
      "sharedJS/fullOptJS" ::
      "sharedJS/packageJSDependencies" ::
      "sharedJVM/test" ::
      "sharedJVM/stage" ::
      "set productionBuild in client := false" ::
      "set elideOptions in client := Seq()" ::
      state
  }
}

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