scalaVersion in ThisBuild := "2.11.8"

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.bintrayIvyRepo("scalameta", "maven")
addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-beta4" cross CrossVersion.full)
scalacOptions in (Compile, console) := Seq()
sources in (Compile, doc) := Nil

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.6.0-M1",
  "org.scalameta" %% "scalameta" % "1.4.0",
  "org.specs2" %% "specs2-core" % "3.8.8" % Test
)

organization := "com.gregghz"

bintrayOrganization := Some("gregghz")

bintrayPackage := "json-macro"

bintrayRepository := "gregghz"

publishMavenStyle := true

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

version := "0.0.4"