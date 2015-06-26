import com.lihaoyi.workbench.Plugin._
import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.less.Import._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes._
import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.SbtWeb
import net.virtualvoid.sbt.graph.Plugin.graphSettings
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin._

object Build extends sbt.Build {
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
      NativePackagerKeys.batScriptExtraDefines += "set APP_HOST=0.0.0.0",
      NativePackagerKeys.batScriptExtraDefines += "set APP_PORT=8080",
      NativePackagerKeys.bashScriptExtraDefines += "export APP_HOST=0.0.0.0",
      NativePackagerKeys.bashScriptExtraDefines += "export APP_PORT=8080",
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

