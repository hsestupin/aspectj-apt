package my;

import apt.Event;

/**
 * @author s.stupin
 */
public class Application {

  public static void main(String[] args) {
    Application app = new Application();
    app.OnSomeMethod1Event.add((emmiter) -> {
      System.out.println("before-callback for method \"someMethod1\"");
    });
    app.someMethod1();
  }

  @Event(Event.Order.Before)
  public void someMethod1() {
    System.out.println("someMethod1 is invoked");
  }
}