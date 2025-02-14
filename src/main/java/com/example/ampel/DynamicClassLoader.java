package com.example.ampel;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;

public class DynamicClassLoader extends URLClassLoader {
    public DynamicClassLoader() {
        super(new URL[0], ClassLoader.getSystemClassLoader());
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (!name.equals("UserCode")) {
            return super.loadClass(name);
        }

        try {
            File file = new File("UserCode.class");
            byte[] classBytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(classBytes);
            }
            return defineClass(name, classBytes, 0, classBytes.length);
        } catch (IOException e) {
            throw new ClassNotFoundException("Klasse konnte nicht geladen werden", e);
        }
    }
}
