ThisBuild / scalaVersion := "2.13.16"

val chiselVersion = "6.7.0"

lazy val root = (project in file("."))
  .settings(
    name := "diplomacy-demo",

    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel" % chiselVersion,
      "com.lihaoyi" %% "sourcecode" % "0.3.1"
    ),

    addCompilerPlugin(
      ("org.chipsalliance" % "chisel-plugin" % chiselVersion)
        .cross(CrossVersion.full)
    ),

    Compile / unmanagedSourceDirectories ++= Seq(
      baseDirectory.value / "deps" / "cde" / "cde" / "src",
      baseDirectory.value / "deps" / "diplomacy" / "diplomacy" / "src"
    ),

    Compile / mainClass := Some("demo.Main")
  )
