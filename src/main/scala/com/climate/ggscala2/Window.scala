package com.climate.ggscala2

object Window {

  /**
    * Create a string that defines an axis' limits, e.g. "xlim(0, 1)"
    *
    * @param axisName Name of axis ("x" or "y")
    * @param limits   User-defined limits
    * @return
    */
  def axisLimits(axisName: String, limits: Option[(Double, Double)]): String = {
    limits match {
      case Some((l, u)) => axisName + "lim(" + l + ", " + u + ")"
      case None => ""
    }
  }
}
