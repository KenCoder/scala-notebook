package com.k2sw.scalanb.common

/**
 * Author: Ken
 */

case object InterruptRequest
case object RestartRequest
case class ExecuteRequest(code: String)
case class ExecuteResponse(stdout: String)
case class ErrorResponse(message: String)

