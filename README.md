# Akka Quickstart (Java)

Akka is a toolkit and runtime for building highly concurrent, distributed, 
and fault-tolerant event-driven applications on the JVM. Akka can be used 
with both Java and Scala. This guide introduces Akka by describing the 
Java version of the Hello World example. If you prefer to use Akka with Scala, 
switch to the Akka Quickstart with Scala guide.

Actors are the unit of execution in Akka. The Actor model is an abstraction 
that makes it easier to write correct concurrent, parallel and distributed 
systems. The Hello World example illustrates Akka basics. Within 30 minutes, 
you should be able to download and run the example and use this guide to 
understand how the example is constructed. This will get your feet wet, 
and hopefully inspire you to dive deeper into the wonderful sea of Akka!

### Gradle

```shell
gradle run
```

### Maven

```shell
mvn compile exec:exec
```

### Explained

- Greet: Receives commands to Greet someone and responds with a Greeted to 
confirm the greeting has taken place.

- GreeterBot: receives the reply from the Greeter and sends a number of 
additional greeting messages and collect the replies until a given max 
number of messages have been reached.

- GreeterMain: The guardian actor that bootstraps everything.

## Defining Actors and messages

Each actor defines a type T for the messages it can receive. Case classes and 
case objects make excellent messages since they are immutable and have support 
for pattern matching, something we will take advantage of in the Actor when 
matching on the messages it has received.

The Hello World Actors use three different messages:

- Greet: command sent to the Greeter actor to greet
- Greeted: reply from the Greeter actor to confirm the greeting has happened
- SayHello: command to the GreeterMain to start the greeting process

When defining Actors and their messages, keep these recommendations in mind:

- Since messages are the Actor’s public API, it is a good practice to define messages
 with good names and rich semantic and domain specific meaning, even if they just 
 wrap your data type. This will make it easier to use, understand and debug actor-based 
 systems.

- Messages should be immutable, since they are shared between different threads.

- It is a good practice to put an actor’s associated messages as static classes in 
the AbstractBehaavior’s class. This makes it easier to understand what type of 
messages the actor expects and handles.

- It is a good practice obtain an actor’s initial behavior via a static factory method

Let's see how the implementations for Greeter, GreeterBot and GreeterMain demonstrate 
these best practices.

### The Greeter Actor

The following snippet from the Greeter.java implements the Greeter Actor:

```java
public class Greeter extends AbstractBehavior<Greeter.Greet> {

  public static final class Greet {
    public final String whom;
    public final ActorRef<Greeted> replyTo;

    public Greet(String whom, ActorRef<Greeted> replyTo) {
      this.whom = whom;
      this.replyTo = replyTo;
    }
  }

  public static final class Greeted {
    public final String whom;
    public final ActorRef<Greet> from;

    public Greeted(String whom, ActorRef<Greet> from) {
      this.whom = whom;
      this.from = from;
    }

  }

  public static Behavior<Greet> create() {
    return Behaviors.setup(Greeter::new);
  }

  private Greeter(ActorContext<Greet> context) {
    super(context);
  }

  @Override
  public Receive<Greet> createReceive() {
    return newReceiveBuilder().onMessage(Greet.class, this::onGreet).build();
  }

  private Behavior<Greet> onGreet(Greet command) {
    getContext().getLog().info("Hello {}!", command.whom);
    command.replyTo.tell(new Greeted(command.whom, getContext().getSelf()));
    return this;
  }
}
```

This small piece of code defines two message types, one for commanding the Actor to greet 
someone and one that the Actor will use to confirm that it has done so. The Greet type 
contains not only the information of whom to greet, it also holds an ActorRef that the 
sender of the message supplies so that the Greeter Actor can send back the confirmation message.

The behavior of the Actor is defined as the Greeter AbstractBehavior with the help of the 
newReceiveBuilder behavior factory. Processing the next message then results in a new behavior 
that can potentially be different from this one. The state can be updated by modifying the 
current instance as it is mutable. In this case we don’t need to update any state, so we 
return this without any field updates, which means the next behavior is “the same as the 
current one”.

The type of the messages handled by this behavior is declared to be of class Greet. 
Typically, an actor handles more than one specific message type and then there is one 
common interface that all messages that the actor can handle implements.

On the last line we see the Greeter Actor send a message to another Actor, which is done 
using the tell method. It is an asynchronous operation that doesn’t block the caller’s thread.

Since the replyTo address is declared to be of type ActorRef<Greeted>, the compiler will 
only permit us to send messages of this type, other usage will be a compiler error.

The accepted message types of an Actor together with all reply types defines the protocol 
spoken by this Actor; in this case it is a simple request–reply protocol but Actors can 
model arbitrarily complex protocols when needed. The protocol is bundled together with 
the behavior that implements it in a nicely wrapped scope—the Greeter class.

### The GreeterBot Actor

Note how this Actor manages the counter with an instance variable. No concurrency 
guards such as synchronized or AtomicInteger are needed since an actor instance 
processes one message at a time.

```java
package $package$;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class GreeterBot extends AbstractBehavior<Greeter.Greeted> {

    public static Behavior<Greeter.Greeted> create(int max) {
        return Behaviors.setup(context -> new GreeterBot(context, max));
    }

    private final int max;
    private int greetingCounter;

    private GreeterBot(ActorContext<Greeter.Greeted> context, int max) {
        super(context);
        this.max = max;
    }

    @Override
    public Receive<Greeter.Greeted> createReceive() {
        return newReceiveBuilder().onMessage(Greeter.Greeted.class, this::onGreeted).build();
    }

    private Behavior<Greeter.Greeted> onGreeted(Greeter.Greeted message) {
        greetingCounter++;
        getContext().getLog().info("Greeting {} for {}", greetingCounter, message.whom);
        if (greetingCounter == max) {
            return Behaviors.stopped();
        } else {
            message.from.tell(new Greeter.Greet(message.whom, getContext().getSelf()));
            return this;
        }
    }
}
```

### The Greeter main actor
A third actor spawns the Greeter and the GreeterBot and starts the interaction, 
creating actors and what spawn does is discussed next.

```java
package $package$;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

public class GreeterMain extends AbstractBehavior<GreeterMain.SayHello> {

    public static class SayHello {
        public final String name;

        public SayHello(String name) {
            this.name = name;
        }
    }

    private final ActorRef<Greeter.Greet> greeter;

    public static Behavior<SayHello> create() {
        return Behaviors.setup(GreeterMain::new);
    }

    private GreeterMain(ActorContext<SayHello> context) {
        super(context);
        greeter = context.spawn(Greeter.create(), "greeter");
    }

    @Override
    public Receive<SayHello> createReceive() {
        return newReceiveBuilder().onMessage(SayHello.class, this::onSayHello).build();
    }

    private Behavior<SayHello> onSayHello(SayHello command) {
        ActorRef<Greeter.Greeted> replyTo =
                getContext().spawn(GreeterBot.create(3), command.name);
        greeter.tell(new Greeter.Greet(command.name, replyTo));
        return this;
    }
}
```

### Reference

- [Defining Actors and Messages](https://developer.lightbend.com/guides/akka-quickstart-java/define-actors.html)