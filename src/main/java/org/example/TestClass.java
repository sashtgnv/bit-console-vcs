package org.example;


import java.io.File;
import java.io.IOException;

public class TestClass {
    public static void main(String[] args) throws IOException {
        File file = new File("testdir");
        System.out.println(file.getPath());
        System.out.println(file.getParent());
        System.out.println(file.getCanonicalPath());
        System.out.println(file.getAbsolutePath());
    }
}
