package com.k2sw.scalanb
package client

import java.net.URLClassLoader
import org.clapper.avsl.Logger
import org.apache.commons.codec.binary.Base64
import org.apache.commons.exec.{ExecuteException, ExecuteResultHandler, DefaultExecutor, CommandLine}
import com.typesafe.config.{ConfigFactory, Config}
import java.io.{InputStreamReader, ObjectOutputStream, ByteArrayOutputStream, File}
import akka.remote.RemoteScope
import akka.actor._
import org.apache.commons.io.{IOUtils, FileUtils}
import akka.serialization.{JavaSerializer, Serialization}

/**
 * Given a classpath, runs a kernel process and monitors the result. When it dies, we examine the result to decide
 * whether to restart it or not.
 */

case class SpawnActor(props : Props, name : String)
case class ActorSpawned(id: Int, child: ActorRef)
case class DestroyChild(actor: ActorRef)

case class SpawnCallbackInfo(creator: ActorRef, id: Int)
case class SpawnComplete(id: Int)

object SpawnerMain {

  def idToPort(id: Int) = 12000 + id

  def main(args: Array[String]) {
//    system.actorOf(Props[Kernel], name = "kernel")
    if (args.length > 1) {
      val port = args(0).toInt
      val r =  getClass.getResourceAsStream("/kernel.conf")
      val s = IOUtils.toString(r, "UTF-8")
      // TODO: Port selection
      val config = ConfigFactory.parseString(s.format(port))
      val system = ActorSystem("KernelManager", config)
      system.actorOf(Props(new Responder))
        class Responder extends Actor {
          override def preStart() {
            val info = ObjectEncoder.decode[SpawnCallbackInfo](args(1))
            println("Client sending startup")
            info.creator ! SpawnComplete(info.id)
          }
          def receive = {
            case _ =>
          }
        }

        println("Client is running - press Enter to quit")
        Console.readLine()
        system.shutdown()
      }
    }
}

class ActorSpawner extends Actor {
  var idCounter = 0
  val javaHome = System.getProperty("java.home")

  def classPath: Seq[String] = {
    val loader = getClass().getClassLoader.asInstanceOf[URLClassLoader]
    loader.getURLs map { u => new File(u.getFile).getPath }
  }
  def memory: String = "1200m"
  def mainClass = "com.k2sw.scalanb.client.SpawnerMain"

  class SpawnedActor(val originalRequester: ActorRef, val props: Props, val name: String) {
    var id: Int = {
      idCounter += 1
      idCounter
    }
    var childActor: ActorRef = null
  }

  val children = collection.mutable.Map[Int, SpawnedActor]()

  def receive = {
    case SpawnComplete(id) =>
      val info = children(id)
      info.childActor = context.actorOf(info.props.withDeploy(Deploy(scope = RemoteScope(sender.path.address))), info.name)
      children(id).originalRequester ! ActorSpawned(info.id, info.childActor)

    case SpawnActor(props, name) =>
      val child = new SpawnedActor(sender, props, name)
      val msgToChild = SpawnCallbackInfo(context.self, child.id)
      val encodedMsg = ObjectEncoder.encode(msgToChild)
      val cmd = new CommandLine(javaHome + "/bin/java.exe").addArgument("-cp")
        .addArgument(classPath.mkString(System.getProperty("path.separator")))
        .addArgument("-Xmx" + memory)
        .addArgument(mainClass)
        .addArgument(SpawnerMain.idToPort(child.id).toString)
        .addArgument(encodedMsg)
//      logger.info(cmd)
      children.put(child.id, child)

      val exec = new DefaultExecutor
      exec.execute(cmd, new ExecuteResultHandler {
        def onProcessFailed(e: ExecuteException) {}

        def onProcessComplete(exitValue: Int) {}
      })
  }
}

