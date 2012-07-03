package com.k2sw.scalanb

import akka.testkit.TestKit
import client.{SpawnActor, ActorSpawned, ActorSpawner}
import org.scalatest.WordSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.BeforeAndAfterAll
import akka.testkit.ImplicitSender
import akka.util.duration._
import akka.actor._

class EchoActor extends Actor {
  def receive = {
    case x ⇒ sender ! x
  }
}

class ActorSpawnTests(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpec with MustMatchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  override def afterAll {
    system.shutdown()
  }

  "An Echo actor" must {

    "send back messages unchanged" in {
      val r =  classOf[ActorSpawner].getResourceAsStream("/kernel.conf")

      val spawner = system.actorOf(Props[ActorSpawner])
      spawner ! SpawnActor(Props[EchoActor], "echo")
      val resp = expectMsgType[ActorSpawned](10 seconds)
      resp.child ! "hello world"
      expectMsg("hello world")
      spawner ! PoisonPill
    }
  }
  }

