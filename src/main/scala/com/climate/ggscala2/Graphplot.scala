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

import breeze.linalg.DenseMatrix
import breeze.numerics.{cos, pow, sin, sqrt}

import scala.math.Pi
import org.ddahl.rscala.RClient

trait Graphplot {
  private[ggscala2] def graphplot(r: RClient,
                                  x: DenseMatrix[Boolean],
                                  y: Array[Double],
                                  z: Array[String],
                                  title: String): Unit = {
    val n: Int = z.length
    require(x.rows == n && x.cols == n, "x should be a matrix of size " + n + " * " + n)
    val v: Array[Int] = (0 until n).toArray
    val xraw: Array[Double] = v.map(i => 0.5 + 0.5 * cos(2.0 * Pi * i / n))
    val yraw: Array[Double] = v.map(i => 0.5 + 0.5 * sin(2.0 * Pi * i / n))

    val pairs: Array[(Int, Int)] = for (en <- v; st <- v if x(st, en)) yield (st, en)

    val tol: Double = y(3)
    val xstart: Array[Double] = pairs.map(p => xraw(p._1))
    val ystart: Array[Double] = pairs.map(p => yraw(p._1))
    val deltaX: Array[Double] = pairs.map(p => xraw(p._2) - xraw(p._1))
    val deltaY: Array[Double] = pairs.map(p => yraw(p._2) - yraw(p._1))
    val xend: Array[Double] = (xstart, deltaX, deltaY).zipped.map((x, dx, dy) => {
      x + dx * (1.0 - tol / sqrt(pow(dx, 2) + pow(dy, 2)))
    })
    val yend: Array[Double] = (ystart, deltaX, deltaY).zipped.map((y, dx, dy) => {
      y + dy * (1.0 - tol / sqrt(pow(dx, 2) + pow(dy, 2)))
    })

    r.set("xstart_graph", xstart)
    r.set("xend_graph", xend)
    r.set("ystart_graph", ystart)
    r.set("yend_graph", yend)
    r.set("xraw_graph", xraw)
    r.set("yraw_graph", yraw)
    r.set("sz_graph", y)
    r.set("z_graph", z)

    r.eval("df_graph = data.frame(xstart = xstart_graph, xend = xend_graph, ystart = ystart_graph, yend = yend_graph)")
    r.eval("df_graph2 = data.frame(x = xraw_graph, y = yraw_graph, z = z_graph)")

    val tt: String = if (title == "") "" else "ggtitle('" + title + "') "

    val cmd: Array[String] = Array("graphplot", "ggplot()",
      "coord_fixed(ratio = 1, xlim = c(-0.1, 1.1), ylim = c(-0.1, 1.1))",
      "theme_void()",
      tt,
      "geom_segment(data = df_graph, aes(x = xstart, y = ystart, xend = xend, yend = yend), " +
      "arrow = arrow(length = unit(sz_graph[3],'cm'), type='closed'), color = 'dark grey')",
      "geom_point(data = df_graph2, aes(x=x, y=y), colour = 'light grey', size = sz_graph[1])",
      "geom_text(data = df_graph2, aes(x=x, y=y, label = z), size = sz_graph[2])")

    r.set("cmd_graphplot", cmd)
    r.eval(cmd.head + " = " + cmd.tail.filter(_ != "").mkString(" + "))

  }
}
