//package com.k2sw.scalanb
//package client
//
//import java.net.URLClassLoader
//import akka.actor.{ActorRef, Actor, Props, ActorSystem}
//import java.io.{PrintWriter, ByteArrayOutputStream}
//import tools.nsc.Settings
//import tools.nsc.interpreter.IMain
//import akka.actor.Status.Success
//import org.clapper.avsl.Logger
//
//
///**
// * Author: Ken
// *
// * Server                 Client
// *    KernelManager
// *           spawns
// *    Kernel
// *           creates
// *    ScalaKernel
// */
//
//object KernelManagerMain {
//  lazy val system = ActorSystem("KernelManager")
//  val EXIT_RESTART = 100
//
//  var kernelInClient: Option[ActorRef] = None
//
//  def main(args: Array[String]) {
//    system.actorOf(Props[Kernel], name = "kernel")
//    println("Client is running - press Enter to quit")
//    Console.readLine()
//    system.shutdown()
//  }
//}
//
//
//// Runs in the server process, to register the kernelInClient
//class KernelManager extends Actor {
//
//  def receive = {
//    case _ => sys.error("Unexpected message for KernelServerNotifier")
//  }
//}
//
//// Runs in the client process, and dispatches all messages for client
//class Kernel extends Actor {
//  var scalaKernel: ActorRef = _
//  override def preStart {
//    context.actorOf(Props[KernelServerNotifier], name = "kernelServerNotifier")
//    scalaKernel = context.actorOf(Props[ScalaKernel], name = "scalaKernel")
//  }
//
//  def receive = {
//    case msg@ExecuteRequest(code, sender) => scalaKernel ! msg
//    case InterruptRequest => System.exit(KernelManagerMain.EXIT_RESTART)
//  }
//}
//
//class ScalaKernel extends Actor {
//  lazy val stdoutBytes = new ByteArrayOutputStream()
//  lazy val stdout = new PrintWriter(stdoutBytes)
//
//  lazy val interp = {
//    val settings = new Settings
//    settings.embeddedDefaults[ScalaKernel]
//    val i = new IMain(settings, stdout)
//    i.initializeSynchronous()
//    i
//  }
//  def receive = {
//    case ExecuteRequest(code, sender) =>
//      stdout.flush()
//      stdoutBytes.reset()
//      val res = interp.interpret(code)
//      stdout.flush()
//      if (res == Success)
//        sender ! ExecuteResponse(stdoutBytes.toString)
//      else
//        sender ! ErrorResponse(stdoutBytes.toString)
//  }
//}
//
