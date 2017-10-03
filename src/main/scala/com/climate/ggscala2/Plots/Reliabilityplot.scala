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

import breeze.numerics.{Inf, abs, pow, round}
import com.climate.ggscala2.Window
import org.ddahl.rscala.RClient

trait Reliabilityplot {
  private[ggscala2] def reliabilityplot(r: RClient,
                                        x: Array[Double],
                                        y: Array[Boolean],
                                        z: Option[Double],
                                        xlab: String,
                                        ylab: String,
                                        title: String,
                                        xlim: Option[(Double, Double)],
                                        ylim: Option[(Double, Double)]): Unit = {

    val tt: String = if (title == "") "" else "ggtitle('" + title + "') "
    val (xb, yb): (String, String) = (Window.axisLimits("x", xlim), Window.axisLimits("y", ylim))

    val zlab: String = z match {
      case Some(_) => "BSS"
      case None => "BScore"
    }

    def quantile(data: Array[Double], p: Array[Double]): Array[Double] = {

      require(p.forall(pr => pr >= 0.0 && pr <= 1.0), "All p must be in [0,1]")

      val n: Int = data.length
      val dataSorted: Array[Double] = data.sorted

      p.map(pr => {
        val f: Double = (n + 1) * pr
        val i: Int = f.toInt
        if (i == 0)
          dataSorted.head
        else if (i >= n)
          dataSorted.last
        else
          dataSorted(i - 1) + (f - i) * (dataSorted(i) - dataSorted(i - 1))
      })
    }

    val p: Array[Double] = Array(0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0)

    val qtlX: Array[Double] = quantile(x, p) ++ Array(Inf)

    val xPred: Array[Double] = (qtlX.init, qtlX.tail).zipped.map((l, u) => {
      val z: Array[Double] = x.filter(xx => xx >= l && xx < u)
      if (z.nonEmpty) z.sum / z.length else Inf
    })

    val n: Array[Int] = (qtlX.init, qtlX.tail).zipped.map((l, u) => x.count(xx => xx >= l && xx < u))
    val nn: Array[Int] = (xPred, n).zipped.filter((xP, _) => !xP.isInfinite)._2
    val nTotal: Int = nn.sum

    val xx: Array[Double] = xPred.filter(!_.isInfinite)

    val yFreq: Array[Double] = (qtlX.init, qtlX.tail).zipped.map((l, u) => {
      val w: Array[Boolean] = (x, y).zipped.filter((xx, _) => xx >= l && xx < u)._2
      w.count(q => q).toDouble / w.length
    })

    val yy: Array[Double] = (xPred, yFreq).zipped.filter((xP, _) => !xP.isInfinite)._2

    // either compute brier score or brier skill score, depending on the presence of z
    val brier: Array[Double] = {
      val score: Array[Double] = (qtlX.init, qtlX.tail).zipped.map((l, u) => {
        (x zip y).filter(s => s._1 >= l && s._1 < u).map(s => {
          if (s._2) (s._1 - 1.0) * (s._1 - 1.0) else s._1 * s._1
        }).sum / nTotal
      })
      z match {
        case Some(zz) =>
          val ref: Array[Double] = (qtlX.init, qtlX.tail).zipped.map((l, u) => {
            (x zip y).filter(s => s._1 >= l && s._1 < u).map(s => {
              if (s._2) pow(zz - 1.0, 2) else pow(zz, 2)
            }).sum / nTotal
          })
          val refS: Double = ref.sum
          (ref, score).zipped.map((r, s) => (r - s) / refS)
        case None => score
      }
    }

    val bb: Array[Double] = (xPred, brier).zipped.filter((xP, _) => !xP.isInfinite)._2

    val omnibusB: Double = {
      val modelB: Double = (x, y).zipped.map((xx, yy) => if (yy) pow(xx - 1.0, 2) else pow(xx, 2)).sum / y.length
      z match {
        case Some(zz) =>
          val refB: Double = y.map(yy => if (yy) pow(zz - 1.0, 2) else pow(zz, 2)).sum / y.length
          1.0 - modelB / refB
        case None => modelB
      }
    }
    assert(abs(omnibusB - bb.sum) < 0.0001, "Failure in computing Brier metric " + omnibusB + " vs " + bb.sum)

    val labelB: String = z match {
      case Some(_) => "BSS = " + round(100.0 * omnibusB) / 100.0
      case None => "BScore = " + round(100.0 * omnibusB) / 100.0
    }

    val po: String = z match {
      case Some(zz) =>
        r.set("z_reliability", zz)
        r.eval("df_reliability2 = data.frame(x = c(0, 0, z_reliability, z_reliability, z_reliability, 1, 1), " +
          "y = c(0.5 * z_reliability, 0, 0, z_reliability, 1, 1, 0.5 * (z_reliability ^ 2 - 1) / (z_reliability - 1)))")
        "geom_polygon(data = df_reliability2, aes(x=x, y=y), alpha = 0.1) "
      case None => ""
    }

    val pl: String = z match {
      case Some(_) => "scale_color_gradient2(" +
        "low = paletteNegZeroPos[1], mid = paletteNegZeroPos[2], high = paletteNegZeroPos[3], " +
        " midpoint = 0, name = '" + zlab + "') "
      case None => "scale_color_gradientn(colors = paletteZeroExtreme, name = '" + zlab + "')"
    }

    r.set("x_reliability", xx)
    r.set("y_reliability", yy)
    r.set("b_reliability", bb)

    r.eval("df_reliability = data.frame(x = x_reliability, y = y_reliability, b = b_reliability)")

    val cmd: Array[String] = Array("reliabilityplot",
      "ggplot(df_reliability, aes(x = x, y = y))",
      "coord_fixed(ratio = 1, xlim = c(0,1), ylim = c(0,1))",
      po,
      "geom_abline(slope = 1, intercept = 0, alpha = transparencyAlpha)",
      "geom_line()",
      "geom_point(aes(x=x, y=y), size = 4.5)",
      "geom_point(aes(x=x, y=y, color = b), size = 4)",
      "xlab('" + xlab + "')",
      "ylab('" + ylab + "')",
      tt,
      xb,
      yb,
      "geom_label(data = data.frame(x = 0.2, y = 0.95, z = '" + labelB + "'), aes(x = x, y = y, label = z)) ",
      pl)

    r.set("cmd_reliabilityplot", cmd)
    r.eval(cmd.head + " = " + cmd.tail.filter(_ != "").mkString(" + "))

  }
}
