package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.Objects;

public class Greeter extends AbstractBehavior<Greeter.Greet> {

  private Greeter(ActorContext<Greet> context) {
    super(context);
  }

  /**
   * Send Message Type
   */
  public static final class Greet {
    public final String whom;
    public final ActorRef<Greeted> replyTo;

    public Greet(String whom, ActorRef<Greeted> replyTo) {
      this.whom = whom;
      this.replyTo = replyTo;
    }
  }

  /**
   * Received Message Type
   */
  public static final class Greeted {
    public final String whom;
    public final ActorRef<Greet> from;

    public Greeted(String whom, ActorRef<Greet> from) {
      this.whom = whom;
      this.from = from;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Greeted greeted = (Greeted) o;
      return Objects.equals(whom, greeted.whom) &&
              Objects.equals(from, greeted.from);
    }

    @Override
    public int hashCode() {
      return Objects.hash(whom, from);
    }

    @Override
    public String toString() {
      return "Greeted{" +
              "whom='" + whom + '\'' +
              ", from=" + from +
              '}';
    }
  }

  /**
   * Define Behavior
   *
   * @return
   */
  public static Behavior<Greet> create() {
    return Behaviors.setup(Greeter::new);
  }

  /**
   * Message handler
   *
   * @return
   */
  @Override
  public Receive<Greet> createReceive() {
    return newReceiveBuilder().onMessage(Greet.class, this::onGreet).build();
  }

  /**
   * Message handler private
   *
   * @param command
   * @return
   */
  private Behavior<Greet> onGreet(Greet command) {
    getContext().getLog().info("Hello {}!", command.whom);
    //#greeter-send-message
    command.replyTo.tell(new Greeted(command.whom, getContext().getSelf()));
    //#greeter-send-message
    return this;
  }
}


