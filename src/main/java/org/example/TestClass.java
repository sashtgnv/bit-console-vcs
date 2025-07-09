package org.example;


import java.io.File;
import java.io.IOException;

public class TestClass {
    public static void main(String[] args) throws IOException {
        String s = "Hello, World!!!";
        Chunk chunk = new Chunk(s.getBytes());
        System.out.println(chunk.getHash());
    }
}
