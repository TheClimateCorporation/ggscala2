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

private[ggscala2] trait IO {

  var outputFile: String = "/tmp/ggscala2.png"
  var figWidthInches: Double = 16.0 / 2.54
  var figHeightInches: Double = 9.0 / 2.54
  var figDPI: Int = 300
  var screenScale: Double = 0.33

  private[ggscala2] def outputFileType(myFile: String = outputFile): String = myFile takeRight 3

  private[ggscala2] def openOutputFile(r: RClient, myFile: String = outputFile): Unit = {
    val cmd: String = outputFileType(myFile) match {
      case ("png") => "png('" + myFile + "', width = " + figWidthInches + ", height = " + figHeightInches +
        ", units = 'in', res = " + figDPI + ")"
      case ("pdf") => "pdf('" + myFile + "', width = " + figWidthInches + ", height = " + figHeightInches + ")"
      case _ => throw new Error("Unsupported output file type")
    }
    r.eval(cmd)
  }

  private[ggscala2] def closeOutputFile(r: RClient): Unit = r.eval("dev.off()")

}
