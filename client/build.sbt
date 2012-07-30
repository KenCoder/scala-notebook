organization := "com.k2sw"

name := "client"

version := "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "net.liftweb" %% "lift-json" % "2.4",
   "net.databinder" %% "unfiltered-json" % "0.6.2",
   "commons-io" % "commons-io" % "1.3.2",
   "org.scalatest" %% "scalatest" % "1.8" % "test"
)

resolvers ++= Seq(
  "java m2" at "http://download.java.net/maven/2"
)

