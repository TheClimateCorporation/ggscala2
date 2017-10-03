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

trait Densityplot {

  private[ggscala2] def densityplot(r: RClient,
                                    x: Array[Double],
                                    z: Option[Array[String]],
                                    xlab: String,
                                    zlab: String,
                                    title: String,
                                    xlim: Option[(Double, Double)],
                                    ylim: Option[(Double, Double)]): Unit = {

    val tt: String = if (title == "") "" else "ggtitle('" + title + "') "

    r.set("x_density", x)

    val mn: String = z match {
      case Some(zz) =>
        require(x.length == zz.length, "Incompatible dimensions, z and x")
        r.set("z_density", zz)
        r.eval("df_densityplot = data.frame(x = x_density, z = z_density)")
        "ggplot(df_densityplot, aes(x = x, fill = z, colour = z)) "
      case None =>
        r.eval("df_densityplot = data.frame(x = x_density)")
        "ggplot(df_densityplot, aes(x = x)) "
    }

    val gu: String = if (zlab == "") "guides(colour = 'none', fill = 'none') " else ""

    val (xb, yb): (String, String) = (Window.axisLimits("x", xlim), Window.axisLimits("y", ylim))

    val cmd: Array[String] = Array("densityplot" , mn ,
      "geom_density(alpha = transparencyAlpha) " ,
      "xlab('" + xlab + "') ",
      "ylab('density') ",
      tt,
      gu,
      xb,
      yb,
      "scale_color_manual(values = paletteLine, name = '" + zlab + "') ",
      "scale_fill_manual(values = paletteFill, name = '" + zlab + "') ")

    r.set("cmd_densityplot", cmd)
    r.eval(cmd.head + " = " + cmd.tail.filter(_ != "").mkString(" + "))

  }
}
