package com.k2sw.scalanb
package client

import java.net.URLClassLoader
import org.apache.commons.exec.{DefaultExecutor, CommandLine}
import org.clapper.avsl.Logger
import java.io.File
import akka.actor.{Props, ActorRef, Actor}

/**
 * Given a classpath, runs a kernel process and monitors the result. When it dies, we examine the result to decide
 * whether to restart it or not.
 */
trait RunnerInfo {
  def classPath: Seq[String]
  def memory: String
  def mainClass: String
}

case class CreateChild(props : Props, name : String, sender: ActorRef)
case object ChildCreated
case class DestroyChild(actor: ActorRef)

class ActorSpawner extends Actor {
  var idCounter = 0

  class SpawnedActor {
    var id: Int = {
      idCounter += 1
      idCounter
    }
    var actorRef: ActorRef = null
  }

  val children = collection.mutable.Map[Int, SpawnedActor]()

  def receive = {
    case CreateChild(props, name) =>

  }


  /**
   * Spawn an actor in another process. Uses
   * @tparam T
   * @return
   */
  def spawn[T <: Actor](info: KernelRunnerInfo): ActorRef = {

  }

  def kill(actor: ActorRef)
}
trait KernelRunner extends KernelRunnerInfo {
  val logger = Logger(classOf[KernelRunner])


  /**
   * Starts a subprocess, restarting it so long as the process keeps asking us to. Returns when the subprocess is done
   */
  def run() {
    val javaHome = System.getProperty("java.home")
    var more = true
    while (more) {
      val cmd =new CommandLine(javaHome + "/bin/java.exe").addArgument("-cp")
      .addArgument(classPath.mkString(System.getProperty("path.separator")))
      .addArgument("-Xmx" + memory)
      .addArgument(mainClass)
      logger.info(cmd)
     val exec = new DefaultExecutor
      val retCode = exec.execute(cmd)
      more = retCode == KernelManagerMain.EXIT_RESTART
    }
  }
}


class DefaultRunnerInfo extends RunnerInfo {
  def classPath: Seq[String] = {
    val loader = getClass().getClassLoader.asInstanceOf[URLClassLoader]
    loader.getURLs map { u => new File(u.getFile).getPath }
  }
  def memory: String = "1200m"
  def mainClass = "com.k2sw.scalanb.client.KernelManagerMain$"
}
