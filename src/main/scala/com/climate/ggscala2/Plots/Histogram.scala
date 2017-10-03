/*
 * Copyright 2017 The Climate Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.climate.ggscala2.Plots

import com.climate.ggscala2.Window
import org.ddahl.rscala.RClient

trait Histogram {
  private[ggscala2] def histogram(r: RClient,
                                  x: Array[Double],
                                  z: Option[Array[String]],
                                  nBins: Int,
                                  usePercentileCutpoints: Boolean,
                                  xlab: String,
                                  zlab: String,
                                  title: String,
                                  xlim: Option[(Double, Double)],
                                  ylim: Option[(Double, Double)]): Unit = {

    r.set("x_hist", x)

    if (z.isDefined) {
      require(z.get.length == x.length, "Incompatible dimensions in histogram")
      r.set("z_hist", z.get)
      r.eval("df_hist = data.frame(x = x_hist, z = z_hist)")
    } else
      r.eval("df_hist = data.frame(x = x_hist)")

    val aes: String = if (z.isDefined) "aes(x = x, fill = z, group = z)" else "aes(x = x)"
    val binw: String = if (usePercentileCutpoints) {
      val xs: Array[Double] = x.sorted
      val n: Int = xs.length
      val breaks: Array[Double] = (0 to nBins).toArray.map(i => xs(i * (n - 1) / nBins))
      r.set("brk", breaks)
      ", breaks = brk"
    } else ""

    val tt: String = if (title == "") "" else "ggtitle('" + title + "') "
    val zl: String = if (zlab == "") "guides(colour = 'none') " else ""
    val (xb, yb): (String, String) = (Window.axisLimits("x", xlim), Window.axisLimits("y", ylim))

    val cmd: Array[String] = Array("histogram",
      "ggplot(df_hist, " + aes + ") ",
      "stat_bin(colour = 'white', bins = " + nBins + binw + ")",
      "xlab('" + xlab + "') ",
      tt,
      xb,
      yb,
      "scale_fill_manual(values = paletteFill, name = '" + zlab + "') ",
      zl)

    r.set("cmd_histogram", cmd)
    r.eval(cmd.head + " = " + cmd.tail.filter(_ != "").mkString(" + "))
  }

}
