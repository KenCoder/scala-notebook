package com.k2sw.scalanb
package client

import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}
import org.apache.commons.codec.binary.Base64

/**
 * To change this template use File | Settings | File Templates.
 */

object ObjectEncoder {

  def encode[T <: AnyRef](o: T): String = {
    val bos = new ByteArrayOutputStream
    val oos = new ObjectOutputStream(bos)
    oos.writeObject(o)
    oos.close()
    bos.close()
    Base64.encodeBase64String(bos.toByteArray)
  }

  def decode[T](s: String) = {
    val a = Base64.decodeBase64(s)
    val bis = new ByteArrayInputStream(a)
    val ois = new ObjectInputStream(bis)
    val res = ois.readObject().asInstanceOf[T]
    ois.close()
    bis.close()
    res
  }
}
