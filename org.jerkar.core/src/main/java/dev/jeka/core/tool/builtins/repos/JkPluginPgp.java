package dev.jeka.core.tool.builtins.repos;

import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkPlugin;
import dev.jeka.core.tool.JkRun;
import org.jerkar.api.crypto.pgp.JkPgp;
import org.jerkar.api.system.JkLog;

import java.nio.file.Paths;

@JkDoc("Provides configurable JkPgp instance for signing artifacts. This instance is directly usable by def code.")
public class JkPluginPgp extends JkPlugin {

    @JkDoc("Path for the public key ring.")
    public String publicRingPath;

    @JkDoc("Path for the secret key ring.")
    public String secretRingPath;

    @JkDoc("Secret password for decoding secret key ring.")
    public String secretKeyPassword;

    @JkDoc("Key name to sign and verify.")
    public String keyName = "";


    protected JkPluginPgp(JkRun run) {
        super(run);
        JkPgp defaultPgp = JkPgp.ofDefaultGnuPg();
        publicRingPath = defaultPgp.getPublicRing().normalize().toString();
        secretRingPath = defaultPgp.getSecretRing().normalize().toString();
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
        sb.append("PGP public ring path : " + pgp.getPublicRing());
        sb.append("\nPGP secret ring path : " + pgp.getSecretRing());
        sb.append("\nPGP key name : " + keyName);
        JkLog.info(sb.toString());
    }
}
