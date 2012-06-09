package com.k2sw.scalanb
package client

import akka.actor.{Actor, Props, ActorSystem}


/**
 * Author: Ken
 */

object NotebookClient {
  lazy val system = ActorSystem("ScalaNotebookClient")
  def main(args: Array[String]) {
    system.actorOf(Props[NotebookClient], name = "primer")
    println("Client is running - press Enter to quit")
    Console.readLine()
    system.shutdown()
  }
}

class NotebookClient extends Actor {
  def receive = {
    case _ => println("received")
  }
}