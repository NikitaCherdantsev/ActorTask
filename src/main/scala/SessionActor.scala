import akka.actor.typed.{ActorRef, Behavior, SpawnProtocol}
import akka.actor.typed.scaladsl.Behaviors

object SessionActor {

  // protocol - see gist below
  import CoreChatEvents._
  import WebSocketsEvents._

  def receive(websocket: Option[ActorRef[WebSocketsEvent]]): Behavior[CoreChatEvent] = Behaviors.receiveMessage {
    // received a message from the user, route to this phone
    case UserMessage(msg, phone)     =>
      println(s"Sending message $msg to phone $phone")
      // interface with ClientMessageService here
      Behaviors.same

    // received an SMS message from the client, route back to the websocket
    case SMSMessage(sender, message) =>
      println("Received SMS Message!")
      websocket.foreach { socket =>
        socket ! MessageToUser(sender, message)
      }
      Behaviors.same

    // received connection request, update websocket
    case Connected(websocket)        =>
      println("Received connection request!")
      receive(Some(websocket))

    case Disconnected =>
      println("Dying now!")
      Behaviors.stopped

    case Failed(ex) =>
      throw new RuntimeException(ex)
  }
}
