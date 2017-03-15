import build._

lazy val msgpack4zNativeJVM = msgpack4zNative.jvm

lazy val msgpack4zNativeJS = msgpack4zNative.js

lazy val msgpack4zNativeNative = msgpack4zNative.native

lazy val nativeTest = project.enablePlugins(
  ScalaNativePlugin
).settings(
  scalaVersion := build.Scala211,
  noPublish
).dependsOn(
  msgpack4zNativeNative
)

lazy val noPublish = Seq(
  PgpKeys.publishSigned := {},
  PgpKeys.publishLocalSigned := {},
  publishLocal := {},
  publishArtifact in Compile := false,
  publish := {}
)

lazy val root = Project(
  "root", file(".")
).settings(
  commonSettings,
  scalaSource in Compile := file("dummy"),
  scalaSource in Test := file("dummy"),
  noPublish
).aggregate(
  msgpack4zNativeJVM, msgpack4zNativeJS
)
