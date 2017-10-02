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

private[ggscala2] trait Daemon extends Palettes {

  var path2r: String = "/Library/Frameworks/R.framework/Versions/Current/Resources/R"

  lazy val rDaemon: RClient = {
    val d: RClient = try {
      RClient()
    } catch {
      case _: java.io.IOException => RClient(rCmd = path2r)
    }
    d.eval("library(ggplot2)")
    d.eval("library(grid)")
    d.eval("vplayout = function(x, y) grid::viewport(layout.pos.row = x, layout.pos.col = y)")
    changePalettes(d)
    d
  }
}
