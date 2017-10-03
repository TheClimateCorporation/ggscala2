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

import org.ddahl.rscala.RClient
import org.joda.time.DateTime

trait Timeseriesplot {

  private[ggscala2] def timeseriesplot(r: RClient,
                                       x: Array[DateTime],
                                       y: Array[Double],
                                       z: Option[Array[Double]],
                                       ymin: Option[Array[Double]],
                                       ymax: Option[Array[Double]],
                                       xlab: String,
                                       ylab: String,
                                       title: String): Unit = {

    require(y.length == x.length, "Incompatible lengths of x and y")

    val tt: String = if (title == "") "" else "ggtitle('" + title + "') "

    r.set("xs", x.map(_.toString))
    r.eval("x_timeseries = as.Date(xs)")
    r.set("y_timeseries", y)

    val gp: String = z match {
      case Some(yy) =>
        require(yy.length == x.length, "Incompatible lengths of x and z")
        r.set("y_timeseries2", yy)
        r.eval("df_timeseries2 = data.frame(x = x_timeseries, y = y_timeseries2)")
        "geom_point(data = df_timeseries2, aes(x = x, y = y)) "
      case None => ""
    }

    val rb: String = (ymin, ymax) match {
      case (Some(l), Some(u)) =>
        require(l.length == x.length, "Incompatible lengths of x and low")
        require(u.length == x.length, "Incompatible lengths of x and high")
        r.set("ymin_timeseries", l)
        r.set("ymax_timeseries", u)
        r.eval("df_timeseries = data.frame(x = x_timeseries, y = y_timeseries, " +
          "ymin = ymin_timeseries, ymax = ymax_timeseries)")
        "geom_ribbon(aes(x = x, ymin = ymin, ymax = ymax), fill = paletteFill[1], alpha = transparencyAlpha) "
      case _ =>
        r.eval("df_timeseries = data.frame(x = x_timeseries, y = y_timeseries)")
        ""
    }

    val _ = Array(gp,
      "xlab('" + xlab + "')",
      "ylab('" + ylab + "')",
      tt +
        "scale_color_manual(values = paletteLine)")

    val cmd: Array[String] = Array("timeseriesplot",
      "ggplot(df_timeseries, aes(x = x, y = y)) ",
      rb,
      "geom_line()",
      gp,
      "xlab('" + xlab + "')",
      "ylab('" + ylab + "')",
      tt,
      "scale_color_manual(values = paletteLine)")

    r.set("cmd_timeseriesplot", cmd)
    r.eval(cmd.head + " = " + cmd.tail.filter(_ != "").mkString(" + "))

  }
}
