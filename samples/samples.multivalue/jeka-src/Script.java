import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;

import java.util.Arrays;

@JkDoc("Demo for JkMultiValue fields")
class Script extends KBean {

    @JkDoc("The phone numbers. key serves as a qualifier like 'home' or 'gsm'")
    public final JkMultiValue<String> phones = JkMultiValue.of(String.class);

    @JkDoc("The Addresses of the client.")
    public final JkMultiValue<Address> addresses = JkMultiValue.of(Address.class);

    public String toto;

    @JkDoc("Print greeting on console")
    public void hello() {
        System.out.println("hello " + toto);
        phones.getEntries().forEach(e -> System.out.printf("%s=%s%n", e.getKey(), e.getValue()));
        System.out.println("---------");
        addresses.getEntries().forEach(e -> System.out.printf("%s=%s%n", e.getKey(), e.getValue()));
    }

    // Used by E2E tests, do not change
    public void test() {
        JkUtilsAssert.state(phones.getEntries().size() == 2, "phones must have 2 entries");
        JkUtilsAssert.state(phones.getValues().equals(Arrays.asList("+3200000000", "+0965432123")), "");

        JkUtilsAssert.state(addresses.getValues().get(0).zip == 1050, "");
    }

    // Assumes that this method is called passing "addresses.2.zip=9999" in command line
    public void testCmdLine() {
        JkUtilsAssert.state(addresses.get("2").zip == 9999, "Expected 9999 but get %s", addresses.get("2").zip);
    }

    @JkPostInit
    protected void postInit(IntellijKBean intellijKBean) {
        intellijKBean.replaceLibByModule("dev.jeka.jeka-core.jar", "core");
    }

    public static class Address {

        @JkDoc("The street name and number")
        public String street;

        public int zip;

        public final JkMultiValue<String> flags = JkMultiValue.of(String.class);

        @Override
        public String toString() {
            return "Address{" +
                    "street='" + street + '\'' +
                    ", zip=" + zip +
                    ", flags=" + flags +
                    '}';
        }
    }



}