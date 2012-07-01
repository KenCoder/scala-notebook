import sbt._
import Keys._

object NotebookBuild extends Build {
  lazy val root = Project(id = "scala-notebook",
    base = file(".")) aggregate(kernel, server)

  lazy val kernel = Project(id = "kernel",
    base = file("kernel"))

  lazy val server = Project(id = "server",
    base = file("server")) dependsOn(kernel)
}
