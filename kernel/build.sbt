organization := "com.k2sw"

name := "client"

version := "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "net.liftweb" %% "lift-json" % "2.4",
   "net.databinder" %% "unfiltered-json" % "0.6.2",
  "org.clapper" %% "avsl" % "0.3.6",
   "commons-io" % "commons-io" % "1.3.2",
   "org.scalatest" %% "scalatest" % "1.8" % "test"
)


libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.2"

libraryDependencies += "com.typesafe.akka" % "akka-remote" % "2.0.2"

libraryDependencies += "org.apache.commons" % "commons-exec" % "1.1"

libraryDependencies += "org.clapper" %% "grizzled-slf4j" % "0.6.9"

resolvers ++= Seq(
  "java m2" at "http://download.java.net/maven/2"
)

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

