package org.example;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Chunk {
    private String hash;
    private byte[] bytes;

    public Chunk(byte[] bytes) {
        this.bytes = bytes;
        this.hash = makeHash(bytes);
    }

    public String getHash() {
        return hash;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public static String makeHash(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] fullHash = md.digest(bytes);
            byte[] shortHash = Arrays.copyOfRange(fullHash, 0, 8);

            StringBuilder sb = new StringBuilder();
            for (byte b : shortHash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
