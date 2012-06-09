package com.k2sw.scalanb.client

import actors.Actor
import akka.actor.ActorSystem

/**
 * Author: Ken
 */

object NotebookClient {
  lazy val system = ActorSystem("ScalaNotebookClient")
  def main(args: Array[String]) {
    system.start()
  }
}


class NotebookClient extends Actor {
  def receive = {
  }
}