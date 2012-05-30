import unfiltered.netty.websockets.WebSocket

/**
 * Author: Ken
 */

class WebSockWrapper(val sock: WebSocket) {
  def send(msg: String) = {
    println("Sending " + msg)
    sock.send(msg)
  }
}
