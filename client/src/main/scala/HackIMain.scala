package scala.tools.nsc
package interpreter

/**
 * Subclass to access some hidden shit that I actually need
 */
class HackIMain(settings: Settings, out: JPrintWriter) extends IMain(settings, out) {
  def previousRequests = prevRequestList
}
