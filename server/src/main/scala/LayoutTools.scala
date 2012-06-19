package com.k2sw.scalanb
/**
 * Global variables available to all templates
 */

object LayoutTools {
  def static_url(path: String) = "/static/" + path

  // Items that will eventually move into the app
  val base_project_url = "/"
  val base_kernel_url = "/"
  val read_only = false
  val mathjax_url = "" // http://cdn.mathjax.org/mathjax/latest/MathJax.js"

}
