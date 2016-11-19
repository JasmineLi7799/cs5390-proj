package edu.utdallas.cs5390.group3.core;

import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import java.nio.charset.StandardCharsets;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.io.IOException;

import java.net.DatagramPacket;

import java.util.Arrays;
import java.io.ByteArrayOutputStream;

public final class Cryptor {
    private static MessageDigest _md5;
    private static MessageDigest _sha1;
    private static final SecureRandom _rand = new SecureRandom();
    // Initialization vector for AES in CBC mode
    private static IvParameterSpec _iv;

    // As the console ouput implies, these NoSuchAlgorithmExceptions basically
    // can't happen since the MessageDigest class itself is part of the JCA
    // standard. The JCA standard requires implementation of MD5 and SHA-1.
    static {
        try {
            _md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Console.fatal("Failed to obtain MD5 MessageDigest.");
            Console.fatal("Your JVM is not JCA-compliant (i.e. broken).");
            System.exit(-1);
        }
        try {
            _sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            Console.fatal("Failed to obtain SHA-256 MessageDigest.");
            Console.fatal("Your JVM is not JCA-compliant (i.e. broken).");
            System.exit(-1);
        }
        try {
            // Some random bytes.
            // IVs really shouldn't be reused like this, but we have no way to
            // negotiate one with the client, so...yeah.
            byte[] pad = { (byte)0xdf, (byte)0xa8, (byte)0xac, (byte)0xdb,
                           (byte)0x12, (byte)0x30, (byte)0x1c, (byte)0xaf,
                           (byte)0x52, (byte)0xe8, (byte)0x79, (byte)0x8a,
                           (byte)0x29, (byte)0xb0, (byte)0xed, (byte)0xe9 };
            _iv = new IvParameterSpec(pad);
        } catch (Exception e) {
            Console.fatal("Cryptor could not initialize IV.");
            System.exit(-1);
        }
    }

    // Java doesn't have top-level static classes (only static nested
    // classes for some reason...)  so we'll fake it by not letting
    // anyone instatiate this class.
    private Cryptor() {}

    // = SHA-1
    // Used to issue response to server challenge.
    public static byte[] hash1(String pk, byte[] rand) {
        ByteArrayOutputStream concat
            = new ByteArrayOutputStream();
        try {
            concat.write(pk.getBytes(StandardCharsets.UTF_8));
            concat.write(rand);
            byte[] digest = hash1(concat.toByteArray());
            concat.close();
            return digest;
        } catch (IOException e) {
            // This can't really fail since the underlying "stream" is a
            // byte array.
            return null;
        }
    }

    public static byte[] hash1(String s) {
        return hash1(s.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] hash1(byte[] input) {
        byte[] hash = _sha1.digest(input);
        _sha1.reset();
        return hash;
    }

    // = MD5
    // Used to generate private key for socket encryption.
    public static byte[] hash2(String pk, byte[] rand) {
        ByteArrayOutputStream concat
            = new ByteArrayOutputStream();
        try {
            concat.write(pk.getBytes(StandardCharsets.UTF_8));
            concat.write(rand);
            byte[] digest = hash2(concat.toByteArray());
            concat.close();
            return digest;
        } catch (IOException e) {
            // This can't really fail since the underlying "stream" is a
            // byte array.
            return null;
        }
    }

    public static byte[] hash2(String s){
        return hash2(s.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] hash2(byte[] input) {
        byte[] hash = _md5.digest(input);
        _md5.reset();
        return hash;
    }

    public static void nextBytes(byte[] bytes) {
        _rand.nextBytes(bytes);
    }

    // All of these potentially throw a bazillion exception types, but none of
    // them can actually happen unless the JVM is badly broken or we've chosen
    // a bad key generation algorithm. So, we'll just toss them upstream as a
    // generic exception.

    // AES (128) is a block cipher. But we can put it into Cipher Block Chaining
    // mode to turn it into a stream cipher.  "PKCS5Padding" refers to how
    // messages are padded to the block size (128 bits) when they are not evenly
    // divisble by it.

    public static byte[] encrypt(SecretKeySpec key, String plaintext)
        throws Exception {
        return encrypt(key, plaintext.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] encrypt(SecretKeySpec key, byte[] plaintext)
        throws Exception {

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, _iv);
        return cipher.doFinal(plaintext);
    }

    public static void decrypt(
        SecretKeySpec key, DatagramPacket dgram) throws Exception {
        // Truncate the dgram buffer to the data length.
        byte[] plaintext = decrypt(key,
                                   Arrays.copyOf(dgram.getData(),
                                                 dgram.getLength()));
        dgram.setData(plaintext);
    }

    public static byte[] decrypt(SecretKeySpec key, byte[] cryptext)
        throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, _iv);
        return cipher.doFinal(cryptext);
    }
}
