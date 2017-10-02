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

trait Surfaceplot {

  private[ggscala2] def surfaceplot(r: RClient,
                                    layerType: List[String],
                                    x: List[Array[Double]],
                                    y: List[Array[Double]],
                                    z: List[Array[Double]],
                                    xlim: Option[(Double, Double)],
                                    ylim: Option[(Double, Double)],
                                    zlim: Option[(Double, Double)],
                                    mainLayer: Int,
                                    xlab: String,
                                    ylab: String,
                                    zlab: String,
                                    title: String,
                                    pch: Option[Array[Int]],
                                    cex: Option[Array[Int]],
                                    text: Option[Array[String]],
                                    coordRatio: Double): Unit = {

    val nlayers: Int = x.length
    require(y.length == nlayers & z.length == nlayers, "Lengths of x, y, z do not match")
    require(mainLayer >= 0 & mainLayer < x.length, "mainLayer out of bounds")
    require((x, y, z).zipped.forall((xx, yy, zz) => {
      val n: Int = xx.length
      yy.length == n & (zz.length == 0 | zz.length == n | zz.length == n * 2)
    }), "Sublengths of x, y, z do not match")

    def sendArray2r(axisName: String, a: List[Array[Double]]): Unit = {
      r.eval(axisName + " = vector('list', length = " + nlayers + ")")
      a.zipWithIndex.foreach(v => {
        r.set("tmp", v._1)
        val h: String = axisName + "[[" + (v._2 + 1).toString + "]]"
        r.eval(h + " = tmp")
      })
    }

    val isUV: Array[Boolean] = (layerType, x, z).zipped.map((tt, xx, zz) => {
      List("vector", "vectors", "arrow", "arrows").contains(tt) && zz.length == 2 * xx.length
    }).toArray

    val isTxt: Array[Boolean] = text match {
      case Some(s) =>
        r.set("txt_surface", s)
        (layerType, x).zipped.map((tt, xx) => tt == "text" && s.length == xx.length).toArray
      case None => Array.fill(nlayers)(false)
    }
    if (layerType.contains("text")) require(isTxt.exists(t => t), "Incompatible lengths of x and text")

    r.set("nlayers_surface", x.length)
    r.set("mainLayer_surface", mainLayer + 1)
    r.set("isUV_surface", isUV)
    r.set("isTxt_surface", isTxt)

    sendArray2r("x_surface", x)
    sendArray2r("y_surface", y)
    sendArray2r("z_surface", z)

    def setAxesLimits(axisName: String, lim: Option[(Double, Double)]): Unit = {
      lim match {
        case Some((l, u)) =>
          r.set(axisName + "lim", Array(l, u))
        case None =>
          val cmd: String =
            axisName + "lim = c(min(" + axisName + "[[mainLayer_surface]]), max(" + axisName + "[[mainLayer_surface]]))"
          r.eval(cmd)
      }
    }

    r.eval("df_surface = vector('list', length = nlayers_surface)")
    r.eval("for (i in 1:nlayers_surface) { n = length(x_surface[[i]]); " +
      "if (isUV_surface[i])" +
      "df_surface[[i]] = data.frame(x = x_surface[[i]], y = y_surface[[i]], " +
      "u = z_surface[[i]][1:n], v = z_surface[[i]][(n + 1) : (2 * n)]) " +
      "else if (isTxt_surface[i]) " +
      "df_surface[[i]] = data.frame(x = x_surface[[i]], y = y_surface[[i]], z = txt_surface) " +
      "else df_surface[[i]] = data.frame(x = x_surface[[i]], y = y_surface[[i]], z = z_surface[[i]]) }")

    setAxesLimits("x_surface", xlim)
    setAxesLimits("y_surface", ylim)
    setAxesLimits("z_surface", zlim)

    //r.set("xl", xlab)
    //r.set("yl", ylab)
    //r.set("zl", zlab)
    val legendPos: String = if (zlab == "") "none" else "right" //legpos
    //r.set("coordRatio", coordRatio)

    val tt: String = if (title == "") "" else "ggtitle('" + title + "') "

    def pointSpecs(sp: Option[Array[Int]], default: Int): Array[Int] = sp match {
      case Some(s) => s
      case None => Array.fill(nlayers)(default)
    }

    r.set("pch_surface", pointSpecs(pch, 16))
    r.set("cex_surface", pointSpecs(cex, 1))

    val baseSpecs: Array[String] = Array(
      "coord_fixed(ratio = " + coordRatio + ", xlim = x_surfacelim, ylim = y_surfacelim) ",
      tt,
      "xlab('" + xlab + "')",
      "ylab('" + ylab + "')",
      "theme_bw()",
      "theme(axis.title.x = element_text(size = 16), axis.title.y = element_text(size = 16), " +
        "panel.grid.major = element_blank(), panel.grid.minor = element_blank(), " +
        "legend.position = '" + legendPos + "', legend.key = element_blank()) "
    )

    val paletteSpecs: Array[String] = r.evalD1("sign(z_surfacelim)") match {
      case (Array(1.0, 1.0) | Array(0.0, 1.0)) => Array(
        "scale_fill_gradient('" + zlab + "', limits = z_surfacelim, space = 'rgb', na.value = 'grey50', " +
          "guide = 'colourbar', low = paletteZeroExtreme[1], high = paletteZeroExtreme[2]) ",
        "scale_color_gradient('" + zlab + "', limits = z_surfacelim, space = 'rgb', na.value = 'grey50', " +
          "guide = 'colourbar', low = paletteZeroExtreme[1], high = paletteZeroExtreme[2]) ")
      case (Array(-1.0, -1.0) | Array(-1.0, 0.0)) => Array(
        "scale_fill_gradient('" + zlab + "', limits = z_surfacelim, space = 'rgb', na.value = 'grey50', " +
          "guide = 'colourbar', low = paletteZeroExtreme[2], high = paletteZeroExtreme[1]) ",
        "scale_color_gradient('" + zlab + "', limits = z_surfacelim, space = 'rgb', na.value = 'grey50', " +
          "guide = 'colourbar', low = paletteZeroExtreme[2], high = paletteZeroExtreme[1]) ")
      case (Array(-1.0, 1.0)) => Array(
        "scale_fill_gradient2('" + zlab + "', limits = z_surfacelim, space = 'rgb', na.value = 'grey50', " +
          "guide = 'colourbar', low = paletteNegZeroPos[1], mid = paletteNegZeroPos[2], high = paletteNegZeroPos[3]) ",
        "scale_color_gradient2('" + zlab + "', limits = z_surfacelim, space = 'rgb', na.value = 'grey50', " +
          "guide = 'colourbar', low = paletteNegZeroPos[1], mid = paletteNegZeroPos[2], high = paletteNegZeroPos[3]) ")
      case (Array(0.0, 0.0)) => throw new Error("Unsupported z_surfacelim")
    }

    val layerSpecs: Array[String] = (0 until nlayers).toArray.map(i => {
      val s: String = (i + 1).toString
      val mydt: String = "data = df_surface[[" + s + "]], "
      layerType(i) match {
        case ("vector" | "vectors" | "arrow" | "arrows") =>
          val magn: String = "sqrt(df_surface[[" + s + "]]$u ^ 2 + df_surface[[" + s + "]]$v ^ 2)"
          "geom_segment(" + mydt + "aes(x = x, y = y, xend = x+u, yend = y + v), " +
            "arrow = arrow(length = unit(" + magn + ", 'cm')), na.rm = TRUE)"
        case ("point" | "points") =>
          val head: String = "geom_point("
          val tail: String = "shape = pch_surface[" + s + "], size = cex_surface[" + s + "])"
          if (z(i).isEmpty)
            head + mydt + "aes(x=x, y=y), " + tail
          else
            head + mydt + "aes(x = x, y = y, color = z), " + tail
        // TODO case ("map" | "usa" | "state" | "county") =>
        case ("contour") =>
          "stat_contour(" + mydt + "aes(x = x, y = y, z = z))"
        case ("raster" | "surface") =>
          "geom_raster(" + mydt + "aes(x = x, y = y, fill = z))"
        case ("text") =>
          "geom_text(" + mydt + "aes(x = x, y = y, label = z))"
        case ("path") =>
          "geom_path(" + mydt + "aes(x = x, y = y, group = z))"
        case ("line") =>
          "geom_line(" + mydt + "aes(x = x, y = y, group = z))"
        case ("rectangle" | "rect") =>
          "geom_rect(" + mydt + "aes(xmin = min(x), xmax = max(x), ymin = min(y), ymax = max(y))," +
            "alpha = 0, color = 'black')"
        case _ => throw new Error("Unsupported plot type.")
      }
    })

    /*
    r.eval("surfaceplot = ggplot(df_surface[[mainLayer]], aes(x = x, y = y)) ")
    for (newBase <- baseSpecs ++ paletteSpecs) {
      val cmd: String = "surfaceplot = surfaceplot " + newBase
      r.eval(cmd)
    }
    for (newLayer <- layerSpecs) {
      val cmd: String = "surfaceplot = surfaceplot " + newLayer
      r.eval(cmd)
    }
    */
    val cmd: Array[String] = Array("surfaceplot",
      "ggplot(df_surface[[mainLayer_surface]], aes(x = x, y = y))") ++ baseSpecs ++ paletteSpecs ++ layerSpecs

    r.set("cmd_surfaceplot", cmd)
    r.eval(cmd.head + " = " + cmd.tail.filter(_ != "").mkString(" + "))

  }
}
