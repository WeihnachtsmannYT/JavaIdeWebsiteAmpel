package com.example.ampel;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

public class MemoryJavaFileObject extends SimpleJavaFileObject {
    private final ByteArrayOutputStream baos;

    protected MemoryJavaFileObject(String name, Kind kind, ByteArrayOutputStream baos) {
        super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
        this.baos = baos;
    }

    @Override
    public OutputStream openOutputStream() {
        return baos;
    }
}
