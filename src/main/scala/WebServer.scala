import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.io.StdIn
import akka.stream.scaladsl.Flow
import akka.http.scaladsl.model.ws.{Message, TextMessage}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object WebServer extends App {

  // implicit values required by the server machinery
  //  implicit val actorSystem = akka.actor.ActorSystem("messaging-actorsystem")
  implicit val spawnSystem = ActorSystem(SpawnProtocol(), "spawn")
  //  implicit val materializer = ActorMaterializer()
  //  implicit val executionContext = actorSystem.dispatcher

  //  // define a basic route ("/") that returns a welcoming message
  //  def helloRoute: Route = pathEndOrSingleSlash {
  //    complete("Welcome to messaging service")
  //  }
  //
  //  // create WebSocket route
  //  def affirmRoute = path("affirm") {
  //    handleWebSocketMessages(
  //      Flow[Message].collect {
  //        case TextMessage.Strict(text) => TextMessage("You said " + text)
  //      }
  //    )
  //  }

  def messageRoute =
  // pattern matching on the URL
    pathPrefix("message" / Segment) { trainerId =>
      // await on the webflow materialization pending session actor creation by the spawnSystem
      Await.ready(ChatSessionMap.findOrCreate(trainerId).webflow(), Duration.Inf).value.get match {
        case Success(value) => handleWebSocketMessages(value)
        case Failure(exception) =>
          println(exception.getMessage())
          failWith(exception)
      }
    }

  // bind the route using HTTP to the server address and port
  val binding = Http().bindAndHandle(messageRoute, "localhost", 8080)
  println("Server running...")

  // kill the server with input
  StdIn.readLine()
  binding.flatMap(_.unbind()).onComplete(_ => spawnSystem.terminate())
  println("Server is shut down")
}