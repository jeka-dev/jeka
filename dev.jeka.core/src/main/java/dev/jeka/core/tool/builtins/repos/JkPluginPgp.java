package dev.jeka.core.tool.builtins.repos;

import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.tool.*;
import dev.jeka.core.api.crypto.pgp.JkPgp;
import dev.jeka.core.api.system.JkLog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@JkDoc({"Provides configurable JkPgp instance for signing artifacts.",
        "To get instantiated it need public and or secret ring files. The order to find those files is the following : ",
        "- Path mentioned in this plugin options 'publicRingPath' 'secretRingPath'.",
        "- If none, project local path [root]/jeka/pgp/pub[sec]ring.gpg",
        "- If none,  standard location ([USER HOME]/AppData/Roaming/gnupg/pub[sec]ring.gpg on Windows and [USER_HOME]/.gnupg/pub[sec]ring.gpg on *nix"})
public class JkPluginPgp extends JkPlugin {

    @JkDoc("Path for the public key ring.")
    @JkEnv("GPG_PUBLIC_RING")
    public Path publicRingPath;

    @JkDoc("Path for the secret key ring.")
    @JkEnv("GPG_SECRET_RING")
    public Path secretRingPath;

    @JkDoc("Secret password for decoding secret key ring.")
    @JkEnv("GPG_PASSPHRASE")
    public String secretKeyPassword;

    @JkDoc("Key name to sign and verify.")
    public String keyName = "";

    private final JkPgp pgp;

    protected JkPluginPgp(JkCommands run) {
        super(run);
        Path localPub = run.getBaseDir().resolve(JkConstants.JEKA_DIR).resolve("gpg/pubring.gpg");
        Path pub = JkUtilsPath.firstExisting(publicRingPath, localPub, JkPgp.getDefaultPubring());
        Path localSec = run.getBaseDir().resolve(JkConstants.JEKA_DIR).resolve("gpg/secring.gpg");
        Path sec = JkUtilsPath.firstExisting(secretRingPath, localSec, JkPgp.getDefaultSecring());
        this.pgp = JkPgp.of(pub, sec, secretKeyPassword);
    }

    /**
     * Returns JkPgp from option settings
     */
    public JkPgp get() {
        return pgp;
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
