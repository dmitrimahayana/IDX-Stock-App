/*
 * Copyright 2016 Lomig Mégard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import scala.io.StdIn
import scala.util.{Failure, Success}

/** Example of a Scala HTTP server using the CORS directive.
 */
object CorsServer {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "cors-server")
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 9090).bind(route)

    futureBinding.onComplete {
      case Success(_) =>
        System.out.println("Server online at http://localhost:9090/\nPress RETURN to stop...")
      case Failure(exception) =>
        System.out.println("Failed to bind HTTP endpoint, terminating system", exception)
        system.terminate()
    }

    StdIn.readLine() // let it run until user presses return
    futureBinding
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  private def route: Route = {
    import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

    // Your CORS settings are loaded from `application.conf`

    // Your rejection handler
    val rejectionHandler = corsRejectionHandler.withFallback(RejectionHandler.default)

    // Your exception handler
    val exceptionHandler = ExceptionHandler { case e: NoSuchElementException =>
      complete(StatusCodes.NotFound -> e.getMessage)
    }

    // Combining the two handlers only for convenience
    val handleErrors = handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)

    // Note how rejections and exceptions are handled *before* the CORS directive (in the inner route).
    // This is required to have the correct CORS headers in the response even when an error occurs.
    // format: off
    handleErrors {
      cors() {
        handleErrors {
          path("ping") {
            complete("[{\"changepercent\": \"+2%\",\"changeval\": \"+20\",\"close\": 725.0,\"date\": \"2023-07-24\",\"id\": \"ACES-2023-07-24\",\"logo\": \"https://s3.goapi.id/logo/ACES.jpg\",\"name\": \"Ace Hardware Indonesia Tbk\",\"open\": 710.0,\"status\": \"Up\",\"ticker\": \"ACES\",\"volume\": 78348510}," +
              "{\"changepercent\": \"+2%\",\"changeval\": \"+20\",\"close\": 725.0,\"date\": \"2023-07-21\",\"id\": \"ACES-2023-07-21\",\"logo\": \"https://s3.goapi.id/logo/ACES.jpg\",\"name\": \"Ace Hardware Indonesia Tbk\",\"open\": 710.0,\"status\": \"Up\",\"ticker\": \"ACES\",\"volume\": 78348510}]")
          } ~
            path("pong") {
              failWith(new NoSuchElementException("pong not found, try with ping"))
            }
        }
      }
    }
    // format: on
  }
}