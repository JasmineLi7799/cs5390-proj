package edu.utdallas.cs5390.group3.core;

import java.security.MessageDigest;
import java.security.SecureRandom;

import java.nio.charset.StandardCharsets;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public final class Cryptor {
    private static MessageDigest _md5;
    private static MessageDigest _sha256;
    private static final SecureRandom _rand = new SecureRandom();

    // As the console ouput implies, these NoSuchAlgorithmExceptions basically
    // can't happen since the MessageDigest class itself is part of the JCA
    // standard. The JCA standard requires implementation of MD5 and SHA-256,
    // along with several other common hashes.
    static {
        try {
            _md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Console.fatal("Failed to obtain MD5 MessageDigest.");
            Console.fatal("Your JVM is not JCA-compliant (i.e. broken).");
            System.exit(-1);
        }
        try {
            _sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Console.fatal("Failed to obtain SHA-256 MessageDigest.");
            Console.fatal("Your JVM is not JCA-compliant (i.e. broken).");
            System.exit(-1);
        }
    }

    // Java doesn't have top-level static classes (only static nested
    // classes for some reason...)  so we'll fake it by not letting
    // anyone instatiate this class.
    private Cryptor() {}

    // = MD5
    // Used to issue response to server challenge.
    public static byte[] hash1(String s) {
        return hash1(s.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] hash1(byte[] input) {
        byte[] hash = _md5.digest(input);
        _md5.reset();
        return hash;
    }

    // = SHA-256
    // Used to generate private key for socket encryption.
    public static byte[] hash2(String s){
        return hash2(s.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] hash2(byte[] input) {
        byte[] hash = _sha256.digest(input);
        _sha256.reset();
        return hash;
    }

    public static void nextBytes(byte[] bytes) {
        _rand.nextBytes(bytes);
    }
}
