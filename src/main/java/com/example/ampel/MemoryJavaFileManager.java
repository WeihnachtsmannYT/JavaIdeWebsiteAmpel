package com.example.ampel;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class MemoryJavaFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
    private final Map<String, ByteArrayOutputStream> classBytes = new HashMap<>();

    protected MemoryJavaFileManager(StandardJavaFileManager fileManager) {
        super(fileManager);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        classBytes.put(className, baos);
        return new MemoryJavaFileObject(className, kind, baos);
    }

    public Map<String, byte[]> getClassBytes() {
        Map<String, byte[]> result = new HashMap<>();
        classBytes.forEach((name, baos) -> result.put(name, baos.toByteArray()));
        return result;
    }
}
