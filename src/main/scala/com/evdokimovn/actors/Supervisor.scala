package com.evdokimovn.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Terminated}
import akka.event.Logging

class Supervisor(actor: ActorRef, system: ActorSystem) extends Actor {
  val log = Logging(context.system, this)

  context.watch(actor)

  def receive = {
    case Terminated(corpse) =>
      if (corpse == actor) {
        system.terminate()
      }
  }
}
