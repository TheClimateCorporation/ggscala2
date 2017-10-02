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

import java.io.File
import breeze.linalg.DenseMatrix
import org.joda.time.DateTime

object Gigi extends Daemon with IO with Spooler with Screen with
  Lineplot with Timeseriesplot with Densityplot with Surfaceplot with ReliabilityPlot with Graphplot with Histogram {

  var inline: Option[String => Any] = None

  // *******************************************************************************************************************
  // * Methods for setting up the screen buffer (split), adding plots to the buffer (spool), and rendering (print) *****
  // *******************************************************************************************************************

  /**
    * Divide a figure horizontally and/or vertically, for multi-panel plotting
    *
    * @param rows Number of rows of panels in the multi-panel figure
    * @param cols Number of columns of panels in the multi-panel figure
    */
  def split(rows: Int, cols: Int): Unit = setBufferSize(r = rDaemon, rows = rows, cols = cols)

  /**
    * Send a ggplot to a (single or multi-panel) figure.
    *
    * @param plotName Name of plot in R environment
    * @param position Vertical position (0 = top) & horizontal position (0 = left) of plot in multi-panel figure
    * @return If row and col are provided
    */
  private def spool(plotName: String, position: Option[(Int, Int)] = None): Any = position match {
    case (Some((r: Int, c: Int))) =>
      require(r >= 0 & c >= 0, "row and col cannot be negative")
      setInBuffer(r = rDaemon, plotName = plotName, row = r, col = c)
    case _ =>
      split(1, 1)
      setInBuffer(r = rDaemon, plotName = plotName, row = 0, col = 0)
      print()
  }

  /**
    * Produce a (single or multi-panel) figure with the spooled ggplot(s)
    *
    * @param myFile Output file
    */
  def print(myFile: File = new File(outputFile)): Any = {
    plotBuffer(rDaemon)
    assert(outputFileType(myFile.getName) == "png", "File must be a png")
    inline match {
      case None => {
        render(myFile, scale = screenScale)
        "Plot sent to JPanel"
      }
      case Some(f) => f(renderHTML(myFile, scale = screenScale))
    }
  }

  /**
    * Bypass the spooler and render a single plot on screen
    *
    * @param plotName Name of plot in R environment
    */
  def print(plotName: String): Any = {
    spool(plotName)
    print()
  }

  /**
    * Empty the buffer
    */
  def clean(): Unit = setBufferClean(rDaemon)

  // *******************************************************************************************************************
  // * Basic methods for built-in plot functions ***********************************************************************
  // *******************************************************************************************************************

  /**
    * Produce a line plot
    *
    * @param y          y-coordinates, f(x)
    * @param x          x-coordinates
    * @param z          group identifier
    * @param xlab       label under x-axis
    * @param ylab       label left of y-axis
    * @param zlab       label over z-bar
    * @param title      plot title
    * @param drawPoints draw (x, y) coordinates as points?
    * @param position   (row, col) position of plot in spooler
    * @return Unit if position is provided; otherwise, Unit if inline = None, or Any if inline has been changed by user
    */
  def lineplot(y: Array[Double],
               x: Option[Array[Double]] = None,
               z: Option[Array[String]] = None,
               xlab: String = "x",
               ylab: String = "y",
               zlab: String = "",
               title: String = "",
               drawPoints: Boolean = false,
               position: Option[(Int, Int)] = None): Any = {
    lineplot(rDaemon, x, y, z, xlab, ylab, zlab, title, drawLine = true, drawPoints)
    spool("lineplot", position)
  }

  /**
    * Produce a scatter plot
    *
    * @param x        x-coordinates
    * @param y        y-coordinates
    * @param xlab     label under x-axis
    * @param ylab     label left of y-axis
    * @param title    plot title
    * @param position (row, col) position of plot in spooler
    * @return Unit if position is provided; otherwise, Unit if inline = None, or Any if inline has been changed by user
    */
  def scatterplot(x: Array[Double],
                  y: Array[Double],
                  z: Option[Array[String]] = None,
                  xlab: String = "x",
                  ylab: String = "y",
                  zlab: String = "",
                  title: String = "",
                  position: Option[(Int, Int)] = None): Any = {
    lineplot(rDaemon, Option(x), y, z, xlab, ylab, zlab, title, drawLine = false, drawPoints = true)
    spool("scatterplot", position)
  }

  /**
    * Produce a time series plot
    *
    * @param x        x-coordinate
    * @param y        y-coordinate
    * @param z        additional y-coordinates of points that are overlaid on the plot
    * @param ymin     lower bound for y-axis
    * @param ymax     upper bound for y-axis
    * @param xlab     label under x-axis
    * @param ylab     label left of y-axis
    * @param title    plot title
    * @param position (row, col) position of plot in spooler
    * @return Unit if position is provided; otherwise, Unit if inline = None, or Any if inline has been changed by user
    */
  def timeseriesplot(x: Array[DateTime],
                     y: Array[Double],
                     z: Option[Array[Double]] = None,
                     ymin: Option[Array[Double]] = None,
                     ymax: Option[Array[Double]] = None,
                     xlab: String = "time",
                     ylab: String = "y",
                     title: String = "",
                     position: Option[(Int, Int)] = None): Any = {
    timeseriesplot(rDaemon, x, y, z, ymin, ymax, xlab, ylab, title)
    spool("timeseriesplot", position)
  }

  /**
    * Produce univariate or multivariate density plots
    *
    * @param x        draws of the random variables
    * @param z        random variable identifier
    * @param xlab     label under x-axis
    * @param zlab     label above z-bar
    * @param title    plot title
    * @param position (row, col) position of plot in spooler
    * @return Unit if position is provided; otherwise, Unit if inline = None, or Any if inline has been changed by user
    */
  def densityplot(x: Array[Double],
                  z: Option[Array[String]] = None,
                  xlab: String = "x",
                  zlab: String = "",
                  title: String = "",
                  position: Option[(Int, Int)] = None): Any = {
    densityplot(rDaemon, x, z, xlab, zlab, title)
    spool("densityplot", position)
  }

  /**
    * Produce a surface plot with one or many layers
    *
    * @param layerType  type of each layer
    * @param x          x-coordinates for each layer
    * @param y          y-coordinates for each layer
    * @param z          z-coordinates for each layer (2x the size of x and y, to represent vectors; empty if not used)
    * @param xlim       bounds of x-axis
    * @param ylim       bounds of y-axis
    * @param zlim       bounds of z-axis
    * @param mainLayer  layer whose z-values are represented in the z-bar, and whose x,y values are used to clip plot
    * @param xlab       label under x axis
    * @param ylab       label left of y axis
    * @param zlab       label over z-bar
    * @param title      plot title
    * @param pch        shape of points; used only in layers with layerType = "points"
    * @param cex        size of points; used only in layers with layerType = "points"
    * @param text       add text to plot; text array length must match length of x and y in layer with layerType="text"
    * @param coordRatio ratio of x and y spacings (default = 1)
    * @param position   (row, col) position of plot in spooler
    * @return Unit if position is provided; otherwise, Unit if inline = None, or Any if inline has been changed by user
    */
  def surfaceplot(layerType: List[String],
                  x: List[Array[Double]],
                  y: List[Array[Double]],
                  z: List[Array[Double]],
                  xlim: Option[(Double, Double)] = None,
                  ylim: Option[(Double, Double)] = None,
                  zlim: Option[(Double, Double)] = None,
                  mainLayer: Int = 0,
                  xlab: String = "",
                  ylab: String = "",
                  zlab: String = "z",
                  title: String = "",
                  pch: Option[Array[Int]] = None,
                  cex: Option[Array[Int]] = None,
                  text: Option[Array[String]] = None,
                  coordRatio: Double = 1.0,
                  position: Option[(Int, Int)] = None): Any = {
    surfaceplot(rDaemon, layerType, x, y, z, xlim, ylim, zlim, mainLayer,
      xlab, ylab, zlab, title, pch, cex, text, coordRatio)
    spool("surfaceplot", position)
  }

  /**
    * Simpler interface to produce surface plots
    *
    * @param x x-coordinates
    * @param y y-coordinates
    * @param z z-coordinates
    * @return Unit if position is provided; otherwise, Unit if inline = None, or Any if inline has been changed by user
    */
  def surfaceplot(x: Array[Double],
                  y: Array[Double],
                  z: Array[Double],
                  xlab: String,
                  ylab: String,
                  zlab: String): Any = {
    surfaceplot(layerType = List("surface"), x = List(x), y = List(y), z = List(z),
      xlim = None, ylim = None, zlim = None, xlab = xlab, ylab = ylab, zlab = zlab,
      pch = None, cex = None, text = None, position = None)
  }

  /**
    * Produce a reliability plot
    *
    * @param x        estimated success probabilities
    * @param y        observed success (true) or failure (false) associated with each estimated probability
    * @param z        reference (e.g. climatological) success probability
    * @param xlab     label under x-axis
    * @param ylab     label left of y-axis
    * @param title    plot title
    * @param position (row, col) position of plot in spooler
    * @return Unit if position is provided; otherwise, Unit if inline = None, or Any if inline has been changed by user
    */
  def reliabilityplot(x: Array[Double],
                      y: Array[Boolean],
                      z: Option[Double] = None,
                      xlab: String = "estimated probability",
                      ylab: String = "observed frequency",
                      title: String = "",
                      position: Option[(Int, Int)] = None): Any = {
    reliabilityplot(rDaemon, x, y, z, xlab, ylab, title)
    spool("reliabilityplot", position)
  }

  /**
    * Produce a graph of model parameters, to show dependencies
    *
    * @param x        A boolean adjacency matrix: x(i, j) = true if parameter i depends on j
    * @param y        Array with (point size, text size, arrow size, arrow separation to point)
    * @param z        Parameter names
    * @param title    Plot title
    * @param position (row, col) position of plot in spooler
    * @return Unit if position is provided; otherwise, Unit if inline = None, or Any if inline has been changed by user
    */
  def graphplot(x: DenseMatrix[Boolean],
                y: Array[Double] = Array(10.0, 3.0, 0.2, 0.1),
                z: Array[String],
                title: String = "",
                position: Option[(Int, Int)] = None): Any = {
    graphplot(rDaemon, x, y, z, title)
    spool("graphplot", position)
  }

  /**
    * Produce a histogram of data
    *
    * @param x                      Values used to construct histogram
    * @param z                      group identifier
    * @param nBins                  number of bins
    * @param usePercentileCutpoints use percentiles to define bin widths?
    * @param xlab                   x-label
    * @param zlab                   group label
    * @param title                  plot title
    * @param position               (row, col) position of plot in spooler
    * @return Unit if position is provided; otherwise, Unit if inline = None, or Any if inline has been changed by user
    */
  def histogram(x: Array[Double],
                z: Option[Array[String]] = None,
                nBins: Int = 30,
                usePercentileCutpoints: Boolean = false,
                xlab: String = "",
                zlab: String = "",
                title: String = "",
                position: Option[(Int, Int)] = None): Any = {
    histogram(rDaemon, x, z, nBins, usePercentileCutpoints, xlab, zlab, title)
    spool("histogram", position)
  }

  // *******************************************************************************************************************
  // * Advanced methods to build custom plots **************************************************************************
  // *******************************************************************************************************************

  /**
    * Pass an object to the R environment
    *
    * @param name  String that defines the object's name in the R environment
    * @param value A Boolean, Int, Double, an Array or an Array of Arrays thereof (see rscala for supported types)
    */
  def set(name: String, value: Any): Unit = rDaemon.set(name, value)

  /**
    * Retrieve an object from the R environment
    *
    * @param name String that defines the object's name in the R environment
    */
  def get(name: String): Any = rDaemon.get(name)

  /**
    * Evaluate a command in R that does not return anything back to Scala. Use this to build a ggplot in R.
    * E.g.:
    * > Gigi("example = ggplot(data.frame(x = 1, y = 2), aes(x = x, y = y)) + geom_point()")
    * > Gigi.print("example")
    *
    * @param cmd A string with an instruction that R can parse
    */
  def apply(cmd: String): Unit = rDaemon.eval(cmd)

  /**
    * After changing colors in one or more palettes, call this method to enforce the changes
    */
  def changePalettes(): Unit = changePalettes(rDaemon)
}
