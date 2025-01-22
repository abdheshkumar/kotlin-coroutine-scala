import Dependencies.*

ThisBuild / scalaVersion := "3.6.3"
ThisBuild / version := Option(System.getProperty("version")).getOrElse("0.1-SNAPSHOT")
ThisBuild / scalacOptions ++= Seq("-Xmax-inlines", "100")

lazy val `kotlin-coroutine-scala` = (project in file("."))
  .settings(
    name := "kotlin-coroutine-scala",
    scalacOptions += "-Wnonunit-statement",
    // Add BOM as a dependency override
    dependencyOverrides ++= Dependencies.Overrides.all,
    // Add managed dependencies
    libraryDependencies ++= Dependencies.all,
  )
  .aggregate(`kotlin-api`)
  .dependsOn(`kotlin-api`)

lazy val `kotlin-api` = project.in(file("kotlin-api"))
  .settings(name := "kotlin-api")
  .settings(publishArtifact := false)
  .enablePlugins(KotlinPlugin)
  .settings(
    libraryDependencies ++= Dependencies.all,
    kotlin.Keys.kotlinLib("stdlib"),
    kotlin.Keys.kotlinVersion := "2.1.0",
    kotlin.Keys.kotlincJvmTarget := "17",
  )