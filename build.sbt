import sbtassembly.AssemblyPlugin.autoImport.assembly

ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version      := "0.1.0-SNAPSHOT"




lazy val root = (project in file("."))
  .settings(
    assembly / mainClass := Some("mainSc.MainApp"),
      assembly / assemblyJarName := "1lab.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "versions", "11", "module-info.class") => MergeStrategy.discard
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    },
    name := "1lab",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.1.21",
      "dev.zio" %% "zio-http" % "3.5.1",
      "dev.zio" %% "zio-json" % "0.7.44"
    )
  )