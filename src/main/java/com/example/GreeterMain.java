package com.example;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class GreeterMain extends AbstractBehavior<GreeterMain.SayHello> {

    private final ActorRef<Greeter.Greet> greeter;

    private GreeterMain(ActorContext<SayHello> context) {
        super(context);
        //#create-actors
        greeter = context.spawn(Greeter.create(), "greeter");
        //#create-actors
    }

    /**
     * Send Message Type
     */
    public static class SayHello {
        public final String name;
        public SayHello(String name) {
            this.name = name;
        }
    }

    /**
     * Define Behavior
     *
     * @return
     */
    public static Behavior<SayHello> create() {
        return Behaviors.setup(GreeterMain::new);
    }

    /**
     * Message handler
     * @return
     */
    @Override
    public Receive<SayHello> createReceive() {
        return newReceiveBuilder().onMessage(SayHello.class, this::onSayHello).build();
    }

    /**
     * Message handler private
     * @param command
     * @return
     */
    private Behavior<SayHello> onSayHello(SayHello command) {
        //#create-actors
        ActorRef<Greeter.Greeted> replyTo =
                getContext().spawn(GreeterBot.create(5), command.name);
        greeter.tell(new Greeter.Greet(command.name, replyTo));
        //#create-actors
        return this;
    }
}
