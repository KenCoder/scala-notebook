import sbt._
import Keys._

object NotebookBuild extends Build {
  lazy val root = Project(id = "scala-notebook",
    base = file(".")) aggregate(client, server, sample)

  lazy val client = Project(id = "client",
    base = file("client"))

  lazy val server = Project(id = "server",
    base = file("server")) dependsOn(client)

  lazy val sample = Project(id = "sample",
    base = file("sample")) dependsOn(client)
}
