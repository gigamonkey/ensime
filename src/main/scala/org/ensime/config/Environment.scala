/**
 *  Copyright (c) 2010, Aemon Cannon
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of ENSIME nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL Aemon Cannon BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ensime.config

import java.net.{URL, JarURLConnection}

object Environment {

  private def p(k: String) = System.getProperty(k)

  def info: String = """
    |Environment:
    |  OS : %s
    |  Java : %s
    |  Scala : %s
    |  Ensime : %s
  """.trim.stripMargin.format(p("os.name"), javaVersion, scalaVersion, ensimeVersion)

  private def javaVersion: String = {
    p("java.vm.name")      + " " + p("java.vm.version") + ", " +
    p("java.runtime.name") + " " + p("java.runtime.version")
  }

  private def scalaVersion: String = scala.util.Properties.versionString

  private def ensimeVersion: String =
    try {
      val pathToEnsimeJar = getClass.getProtectionDomain.getCodeSource.getLocation
      val ensimeJar       = new URL("jar:" + pathToEnsimeJar.toString + "!/").openConnection().asInstanceOf[JarURLConnection].getJarFile()
      ensimeJar.getManifest.getMainAttributes().getValue("Implementation-Version")
    } catch {
      case _: Exception => "unknown"
    }
}
