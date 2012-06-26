package com.k2sw.scalanb
package client

import java.net.URLClassLoader
import akka.actor.{ActorRef, Actor, Props, ActorSystem}


/**
 * Author: Ken
 */

object NotebookClient {
  lazy val system = ActorSystem("ScalaNotebookClient")

  var kernelInClient: Option[ActorRef] = None

  def main(args: Array[String]) {
    val loader = getClass().getClassLoader.asInstanceOf[URLClassLoader]
    println("Classpath is " + loader.getURLs.mkString(","))
    system.actorOf(Props[KernelInClient], name = "kernelInClient")
    println("Client is running - press Enter to quit")
    Console.readLine()
    system.shutdown()
  }
}

// Runs in the server process, to register the kernelInClient
class KernelInServer extends Actor {
  override def preStart {
    context.actorOf(Props[KernelInServer], name = "kernelInServer")
    NotebookClient.kernelInClient = context.parent
  }

  def receive = {
    case _ => println("received")
  }
}
// Runs in the client process, and dispatches all messages for client
class KernelInClient extends Actor {
  override def preStart {
    context.actorOf(Props[KernelInServer], name = "kernelInServer")
  }
  def receive = {
    case _ => println("received")
  }
}