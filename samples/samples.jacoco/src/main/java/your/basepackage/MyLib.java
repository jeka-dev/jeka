package your.basepackage;

import java.util.Arrays;
import java.util.Iterator;

public class MyLib {

    public String toJsonArray(String ... items) {
        if (items.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (Iterator it = Arrays.asList(items).iterator(); it.hasNext();) {
            sb.append("\"").append(it.next()).append("\"");
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
