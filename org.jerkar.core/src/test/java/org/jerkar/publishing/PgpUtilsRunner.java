package org.jerkar.publishing;

import java.io.File;

import org.jerkar.utils.JkUtilsIO;

public class PgpUtilsRunner {

	public static void main(String[] args) {
		PgpUtils.sign(PgpUtilsRunner.class.getResourceAsStream("sampleFileToSign.txt"),
				PgpUtilsRunner.class.getResourceAsStream("keyring.pgp"),
				JkUtilsIO.outputStream(new File("signature.asm")), "momo".toCharArray(), true);
	}



}
