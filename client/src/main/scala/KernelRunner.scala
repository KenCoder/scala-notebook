import akka.actor.Props
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

class KernelRunner extends NotebookKernelProvider {
  val EXIT_RESTART = 100

  /**
   * Starts a subprocess, restarting it so long as the process keeps asking us to. Returns when the subprocess is done
   */
  def run() {
    val javaHome = System.getProperty("java.home")
    var more = true
    while (more) {
      val running = new ProcessBuilder(javaHome + "/bin/java.exe", "-cp",
        classPath.mkString(System.getProperty("path.separator")), "-Xmx" + memory, mainClass).start()
      val retCode = running.waitFor()
      more = retCode == EXIT_RESTART
    }
  }
}


class DefaultKernelRunner extends KernelRunner {
  def classPath: Seq[String] = {
    val loader = getClass().getClassLoader.asInstanceOf[URLClassLoader]
    println("Classpath is " + loader.getURLs.mkString(","))
    println("Client is running - press Enter to quit")

  }
  def memory: String

}