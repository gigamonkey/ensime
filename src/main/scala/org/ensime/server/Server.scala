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

package org.ensime.server

import java.io._
import java.net.{ ServerSocket, Socket }
import org.ensime.protocol._
import org.ensime.util.WireFormat
import org.ensime.config.Environment
import scala.actors._
import scala.actors.Actor._

object Server {

  def main(args: Array[String]): Unit = {
    System.setProperty("actors.corePoolSize", "10")
    System.setProperty("actors.maxPoolSize", "100")

    println("Starting escalier")

    args match {
      case Array(portfile) =>
        {
          println("Starting up Ensime...")
          println(Environment.info)

          // TODO add an option to change the protocol
          val protocol: Protocol = SwankProtocol
          val project: Project = new Project(protocol)
          project.start()

          try {
            // 0 will cause socket to bind to first available port
            val requestedPort = 0
            val listener = new ServerSocket(requestedPort)
            val actualPort = listener.getLocalPort
            println("Server listening on " + actualPort + "..")
            writePort(portfile, actualPort)
            System.out.flush()
            while (true) {
              try {
                val socket = listener.accept()
                println("Got connection, creating handler...")
                startSocketHandlers(socket, protocol, project)
              } catch {
                case e: IOException =>
                  {
                    System.err.println("Error in server listen loop: " + e)
                  }
              }
            }
            listener.close()
          } catch {
            case e: IOException =>
              {
                System.err.println("Server listen failed: " + e)
                System.exit(-1)
              }
          }
        }
      case _ => {
        println("Usage: PROGRAM <portfile>")
        System.exit(0)

      }
    }
  }

  private def writePort(filename: String, port: Int) {
    try {
      val out = new PrintWriter(filename)
      out.println(port)
      out.flush()
      out.close()
      if (!out.checkError()) {
        System.out.println("Wrote port " + port + " to " + filename + ".")
      } else {
        throw new IOException
      }
    } catch {
      case e:IOException => {
        System.err.println("Could not write port to " + filename + ".")
        System.exit(-1)
      }
    } finally {
      Runtime.getRuntime.addShutdownHook(
        new Thread { override def run { new java.io.File(filename).delete } })
    }
  }

  def startSocketHandlers(socket: Socket, protocol: Protocol, project: Project) {

    val reader = actor {
      try {
        val in = new BufferedInputStream(socket.getInputStream());
        while (true) {
          // This has to burn a thread, blocking on the read.
          project ! IncomingMessageEvent(protocol.readMessage(in))
        }
      } catch {
        case e: IOException => {
          System.err.println("Error in socket reader: " + e)
          if (System.getProperty("ensime.explode.on.disconnect") != null) {
            println("Tick-tock, tick-tock, tick-tock... boom!")
            System.exit(-1)
          } else exit('error)
        }
      }
    }

    // This writer is the protocol's output actor. So when it wants to
    // send a message it sends an OutgoingMessageEvent to writer which
    // then turns around and calls writeMessage on the protocol. WTF?
    // (The only way that makes any sense is that it does make sure
    // that only one writeMessage happens at a time. But not clear it
    // should live here.)
    val writer = actor {
      val out = new BufferedOutputStream(socket.getOutputStream())
      loop {
        receive { // react?
          case OutgoingMessageEvent(value: WireFormat) => {
            try {
              protocol.writeMessage(value, out)
            } catch {
              case e: IOException => {
                System.err.println("Write to client failed: " + e)
                exit('error)
              }
            }
          }
          case Exit(_, reason) => exit(reason)
        }
      }
    }

    protocol.setOutputActor(writer)
    writer.link(reader)
  }
}
