package com.k2sw.scalanb
package client

import com.typesafe.config.ConfigFactory
import akka.remote.RemoteScope
import akka.actor._
import org.apache.commons.io.IOUtils
import org.apache.commons.exec._
import java.io._
import concurrent.ops
import java.net.{Socket, InetAddress, ServerSocket, URLClassLoader}

/**
 * Given a classpath, runs a kernel process and monitors the result. When it dies, we examine the result to decide
 * whether to restart it or not.
 */

case class SpawnActor(props : Props, name : String)
case class ActorSpawned(id: Int, child: ActorRef)
case class DestroyChild(actor: ActorRef)
case class SpawnComplete(id: Int)

object SpawnerMain {

  def idToPort(id: Int) = 12000 + id

  def main(args: Array[String]) {
  System.setProperty("org.clapper.avsl.config", "client-avsl.conf")

//    system.actorOf(Props[Kernel], name = "kernel")
    if (args.length > 1) {
      val serverPort = args(0).toInt
      val id = args(1).toInt
      val r =  getClass.getResourceAsStream("/kernel.conf")
      val s = IOUtils.toString(r, "UTF-8")
      // TODO: Port selection
      val config = ConfigFactory.parseString(s.format(idToPort(id)))
      val system = ActorSystem("KernelManager", config)
      val addr = InetAddress.getByName("127.0.0.1")
      val socket = new Socket(addr, serverPort)
      val oos = new ObjectOutputStream(socket.getOutputStream)
      oos.writeInt(id)
      oos.flush()
      try {
        // Blocks until parent quits, then throws an exception
        socket.getInputStream.read()
      } finally {
        system.shutdown()
        System.exit(1)
      }
    }
  }
}

class ActorSpawner extends Actor {
  var idCounter = 0
  val javaHome = System.getProperty("java.home")

  lazy val serverPort = {
    val ss = new ServerSocket(0)
    ops.spawn {
      val a = ss.accept()
      val ois = new ObjectInputStream(a.getInputStream)
      val childId = ois.readInt()
      context.self ! SpawnComplete(childId)
    }
    ss.getLocalPort
  }

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
      info.childActor = context.actorOf(info.props.withDeploy(Deploy(scope = RemoteScope(Address("akka", "KernelManager", "127.0.0.1", SpawnerMain.idToPort(id))))), info.name + info.id)
      children(id).originalRequester ! ActorSpawned(info.id, info.childActor)

    case SpawnActor(props, name) =>
      val child = new SpawnedActor(sender, props, name)
      val cmd = new CommandLine(javaHome + "/bin/java.exe").addArgument("-cp")
        .addArgument(classPath.mkString(System.getProperty("path.separator")))
        .addArgument("-Xmx" + memory)
        .addArgument(mainClass)
        .addArgument(serverPort.toString)
        .addArgument(child.id.toString)
//      logger.info(cmd)
      children.put(child.id, child)


      val exec = new DefaultExecutor
      exec.execute(cmd, new ExecuteResultHandler {
        def onProcessFailed(e: ExecuteException) {}
        def onProcessComplete(exitValue: Int) {}
      })
  }
}

