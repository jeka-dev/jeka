package org.jerkar.tool.builtins.repos;

import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.system.JkLog;
import org.jerkar.tool.JkBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkPlugin;

import java.nio.file.Paths;

@JkDoc("Provides configured JkPgp instance for signing artifacts.")
public class JkPluginPgp extends JkPlugin {

    @JkDoc("Path for the public key ring.")
    public String publicRingPath;

    @JkDoc("Path for the secret key ring.")
    public String secretRingPath;

    @JkDoc("Secret password for decoding secret key ring.")
    public String secretKeyPassword;


    protected JkPluginPgp(JkBuild build) {
        super(build);
        JkPgp defaultPgp = JkPgp.ofDefaultGnuPg();
        publicRingPath = defaultPgp.publicRing().normalize().toString();
        secretRingPath = defaultPgp.secretRing().normalize().toString();
    }

    /**
     * Creates a JkPgp from option settings
     */
    public JkPgp get() {
        return JkPgp.of(Paths.get(publicRingPath), Paths.get(secretRingPath), secretKeyPassword);
    }

    @JkDoc("Displays PGP settings.")
    public void display() {
        StringBuilder sb = new StringBuilder();
        JkPgp pgp = get();
        sb.append("PGP public ring path : " + pgp.publicRing());
        sb.append("\nPGP secret ring path : " + pgp.secretRing());
        JkLog.info(sb.toString());
    }
}
