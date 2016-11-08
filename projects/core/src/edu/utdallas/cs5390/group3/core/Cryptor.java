package edu.utdallas.cs5390.group3.core;

import java.security.MessageDigest;
import java.security.SecureRandom;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public final class Cryptor {
    private static MessageDigest _md5;
    private static MessageDigest _sha256;
    private static final SecureRandom _rand = new SecureRandom();

    static {
        try {
            _md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            // This can't actually happen since this MessageDigest
            // algorithm definitely exists, but we have to have the
            // try/catch block anyway.
        }
        try {
            _sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // This can't actually happen since this MessageDigest
            // algorithm definitely exists, but we have to have the
            // try/catch block anyway.
        }
    }

    // Java doesn't have top-level static classes (only static nested
    // classes for some reason...)  so we'll fake it by not letting
    // anyone instatiate this class.
    private Cryptor() {}

    // = MD5
    // Used to issue response to server challenge.
    public static byte[] hash1(String s) {
        byte[] hash;
        try {
            hash = _md5.digest(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // This can't actually happen.
            return null;
        }
        _md5.reset();
        return hash;
    }

    // = SHA-256
    // Used to generate private key for socket encryption.
    public static byte[] hash2(String s){
        byte[] hash;
        try {
            hash = _sha256.digest(s.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // This can't actually happen.
            return null;
        }
        _sha256.reset();
        return hash;
    }

    public static void nextBytes(byte[] bytes) {
        _rand.nextBytes(bytes);
    }
}
