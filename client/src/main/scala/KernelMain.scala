package com.k2sw.scalanb
package client

import java.net.URLClassLoader
import akka.actor.{ActorRef, Actor, Props, ActorSystem}
import java.io.{PrintWriter, ByteArrayOutputStream}
import tools.nsc.Settings
import tools.nsc.interpreter.IMain
import akka.actor.Status.Success
import org.clapper.avsl.Logger


/**
 * Author: Ken
 *
 * Server                 Client
 *    KernelConnector
 *           creates
 *    KernelRunner
 *           spawns
 *                          KernelMain
 *           creates
 *                          Kernel
 *           creates
 *   KernelServerNotifier
 *           sends ptr back to
 *   KernelConnector
 *           sends request to
 *                          Kernel
 *            creates
 *                          ScalaKernel
 */

object KernelMain {
  lazy val system = ActorSystem("NotebookClient")
  val EXIT_RESTART = 100

  var kernelInClient: Option[ActorRef] = None

  def main(args: Array[String]) {
    val loader = getClass().getClassLoader.asInstanceOf[URLClassLoader]
    println("Classpath is " + loader.getURLs.mkString(","))
    system.actorOf(Props[Kernel], name = "kernel")
    println("Client is running - press Enter to quit")
    Console.readLine()
    system.shutdown()
  }
}


object KernelServerNotifier {
  // Assigned by KernelConnector before it spawns. The KernelServerNotifier sends notice of the client kernel
  // when it is constructed.
  var connector: ActorRef = null
}
// Runs in the server process, to register the kernelInClient
class KernelServerNotifier extends Actor {
  override def preStart {
    KernelServerNotifier.connector ! ClientKernelStartup(context.parent)
  }

  def receive = {
    case _ => sys.error("Unexpected message for KernelServerNotifier")
  }
}

// Runs in the client process, and dispatches all messages for client
class Kernel extends Actor {
  var scalaKernel: ActorRef = _
  override def preStart {
    context.actorOf(Props[KernelServerNotifier], name = "kernelServerNotifier")
    scalaKernel = context.actorOf(Props[ScalaKernel], name = "scalaKernel")
  }

  def receive = {
    case msg@ExecuteRequest(code, sender) => scalaKernel ! msg
    case InterruptRequest => System.exit(KernelMain.EXIT_RESTART)
  }
}

class ScalaKernel extends Actor {
  lazy val stdoutBytes = new ByteArrayOutputStream()
  lazy val stdout = new PrintWriter(stdoutBytes)

  lazy val interp = {
    val settings = new Settings
    settings.embeddedDefaults[ScalaKernel]
    val i = new IMain(settings, stdout)
    i.initializeSynchronous()
    i
  }
  def receive = {
    case ExecuteRequest(code, sender) =>
      stdout.flush()
      stdoutBytes.reset()
      val res = interp.interpret(code)
      stdout.flush()
      if (res == Success)
        sender ! ExecuteResponse(stdoutBytes.toString)
      else
        sender ! ErrorResponse(stdoutBytes.toString)
  }
}

