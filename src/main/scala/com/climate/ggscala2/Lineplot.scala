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

package com.climate.ggscala2

import org.ddahl.rscala.RClient

trait Lineplot {

  private[ggscala2] def lineplot(r: RClient,
                                 x: Option[Array[Double]],
                                 y: Array[Double],
                                 z: Option[Array[String]],
                                 xlab: String,
                                 ylab: String,
                                 zlab: String,
                                 title: String,
                                 drawLine: Boolean,
                                 drawPoints: Boolean): Unit = {

    val zl: String = if (zlab == "") "guides(colour = 'none') " else ""
    val gp: String = if (drawPoints) "geom_point() " else ""
    val gl: String = if (drawLine) "geom_line() " else ""
    val tt: String = if (title == "") "" else "ggtitle('" + title + "') "

    val xx: Array[Double] = x match {
      case Some(a) => a
      case None => (1 to y.length).map(_.toDouble).toArray
    }

    val zz: Array[String] = z match {
      case Some(a) => a
      case None => Array.fill(y.length)("1")
    }

    r.set("x_lineplot", xx)
    r.set("y_lineplot", y)
    r.set("z_lineplot", zz)
    r.eval("df_lineplot = data.frame(x = x_lineplot, y = y_lineplot, z = z_lineplot)")

    val nm: String = if (drawLine) "lineplot" else "scatterplot"
    val cmd: Array[String] = Array(nm,
      "ggplot(df_lineplot, aes(x = x, y = y, colour = z, group = z))",
      gl,
      gp,
      "xlab('" + xlab + "')",
      "ylab('" + ylab + "')",
      tt,
      "scale_color_manual(values = paletteLine, name = '" + zlab + "') ",
      zl)

    r.set("cmd_lineplot", cmd)
    r.eval(cmd.head + " = " + cmd.tail.filter(_ != "").mkString(" + "))

  }
}