package com.k2sw.scalanb

import java.util.UUID

/**
 * Author: Ken
 */


case class Kernel(id: String, nbid: String)

class KernelManager {

  val nbToKernel = collection.mutable.Map[String, Kernel]()

  def startKernel(nbid: String) = {
    val kernel = nbToKernel.getOrElseUpdate(nbid, Kernel(UUID.randomUUID.toString, nbid))
    kernel.id
    }
  }
