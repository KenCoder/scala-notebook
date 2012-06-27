package com.k2sw.scalanb.client

import tools.nsc.Settings
import tools.nsc.interpreter.IMain
import java.io.{PrintWriter, ByteArrayOutputStream}
import tools.nsc.interpreter.Results.Success
import akka.actor.{ActorRef, Actor}

/**
 * Author: Ken
 *   Session - sits
 */


case class ExecuteRequest(code: String, sender: ActorRef)

case class ExecuteResponse(stdout: String)

case class ErrorResponse(message: String)

case object InterruptRequest

case class ClientKernelStartup(kernel: ActorRef)
