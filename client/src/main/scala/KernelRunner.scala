package com.k2sw.scalanb
package client

import java.net.URLClassLoader

/**
 * Given a classpath, runs a kernel process and monitors the result. When it dies, we examine the result to decide
 * whether to restart it or not.
 */
trait NotebookKernelProvider {
  def classPath: Seq[String]
  def memory: String
  def mainClass: String
}

trait KernelRunner extends NotebookKernelProvider {
  val EXIT_RESTART = 100

  /**
   * Starts a subprocess, restarting it so long as the process keeps asking us to. Returns when the subprocess is done
   */
  def run() {
    val javaHome = System.getProperty("java.home")
    var more = true
    while (more) {
      val pb = new ProcessBuilder(javaHome + "/bin/java.exe", "-cp",
        classPath.mkString(System.getProperty("path.separator")), "-Xmx" + memory, mainClass)
      println(pb.command())
      val running = pb.start()
      val retCode = running.waitFor()
      more = retCode == EXIT_RESTART
    }
  }
}


class DefaultKernelRunner extends KernelRunner {
  def classPath: Seq[String] = {
    val loader = getClass().getClassLoader.asInstanceOf[URLClassLoader]
    loader.getURLs map { _.toExternalForm }
  }
  def memory: String = "1200m"
  def mainClass = classOf[NotebookClient].getName
}