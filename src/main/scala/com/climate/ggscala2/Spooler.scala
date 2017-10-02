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

private[ggscala2] trait Spooler extends IO {

  private[ggscala2] def isGGPlot(r: RClient, plotName: String): Boolean =
    r.evalL0("is(" + plotName + ", 'ggplot')")

  private[ggscala2] def setBufferSize(r: RClient, rows: Int, cols: Int): Unit = {
    val cmd: String = "buffer = array(list(), c(" + rows + ", " + cols + "))"
    r.eval(cmd)
  }

  private[ggscala2] def getBufferDim(r: RClient): Array[Int] = {
    if (!r.evalL0("exists('buffer')")) setBufferSize(r, 1, 1)
    r.evalI1("dim(buffer)")
  }

  private[ggscala2] def setInBuffer(r: RClient, plotName: String, row: Int, col: Int): Unit = {

    require(isGGPlot(r, plotName), "You must pass an object of class ggplot to this method.")
    val dbuf: Array[Int] = getBufferDim(r)
    require(row < dbuf(0), "row exceeds limit; use split first")
    require(col < dbuf(1), "col exceeds limit; use split first")

    val cmd: String = "buffer[" + (row + 1).toString + ", " + (col + 1).toString + "] = list(" + plotName + ")"
    r.eval(cmd)
  }

  private[ggscala2] def setBufferClean(r: RClient): Unit = {
    val dbuf: Array[Int] = getBufferDim(r)
    setBufferSize(r = r, rows = dbuf(0), cols = dbuf(1))
  }

  private[ggscala2] def plotBuffer(r: RClient): Unit = {

    val dbuf: Array[Int] = getBufferDim(r)

    openOutputFile(r)
    r.eval("grid::grid.newpage()")
    r.eval("grid::pushViewport(grid::viewport(layout = grid::grid.layout(" + dbuf(0) + ", " + dbuf(1) + ")))")
    r.eval("for (i in 1:" + dbuf(0) + ") for (j in 1:" + dbuf(1) + ") " +
      "{p = buffer[i, j][[1]]; if (!is.null(p)) print(p, vp = vplayout(i, j))}")
    closeOutputFile(r)

  }


}
