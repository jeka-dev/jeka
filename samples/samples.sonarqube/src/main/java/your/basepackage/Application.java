package your.basepackage;

public class Application {

    public void sayHello() {
        System.out.println("Hello");
    }

    public void deadCode() {
        System.out.println("This is dead code");
    }

    public static void main(String[] args) {
        new Application().sayHello();
    }

}
