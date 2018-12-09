package com.hrznstudio.spark.patch;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class JarPatcher implements IPatchContext, AutoCloseable {
    private final FileSystem input;
    private final FileSystem[] classpath;

    private final IBytePatcher patcher;

    public JarPatcher(Path input, Path[] classpath, IBytePatcher patcher) throws IOException {
        this.input = FileSystems.newFileSystem(input, null);
        this.classpath = new FileSystem[classpath.length];
        for (int i = 0; i < classpath.length; i++) {
            this.classpath[i] = FileSystems.newFileSystem(classpath[i], null);
        }

        this.patcher = patcher;
    }

    public static void main(String[] args) throws IOException {
        AtomicLong totalBytes = new AtomicLong();

        JarPatcher patcher = new JarPatcher(Paths.get("input.jar"), new Path[0], (target, bytes) -> {
            totalBytes.addAndGet(bytes.length);
            return bytes;
        });

        patcher.patch(Paths.get("output.jar"));
        System.out.println("Processed " + totalBytes.get() + " bytes");
    }

    public void patch(Path outputPath) throws IOException {
        PatchBlackboard.CONTEXT.set(this);

        Files.deleteIfExists(outputPath);

        URI outputUri = URI.create("jar:" + outputPath.toUri().toString());
        Map<String, String> outputEnv = Collections.singletonMap("create", "true");
        try (FileSystem output = FileSystems.newFileSystem(outputUri, outputEnv)) {
            for (Path root : this.input.getRootDirectories()) {
                Stream<Path> stream = Files.walk(root);
                for (Path inputFile : (Iterable<Path>) stream::iterator) {
                    Path outputFile = output.getPath(inputFile.toString());
                    if (Files.isDirectory(inputFile)) {
                        this.handleDirectory(outputFile);
                    } else {
                        this.handleFile(inputFile, outputFile);
                    }
                }
            }
        } finally {
            PatchBlackboard.CONTEXT.remove();
        }
    }

    private void handleFile(Path inputPath, Path outputPath) throws IOException {
        if (this.shouldPatch(inputPath)) {
            Files.write(outputPath, this.patchFile(inputPath));
        } else {
            Files.copy(inputPath, outputPath);
        }
    }

    private void handleDirectory(Path outputPath) throws IOException {
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
    }

    private byte[] patchFile(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        String target = this.getTarget(path);
        return this.patcher.apply(target, bytes);
    }

    private String getTarget(Path path) {
        String pathString = path.toString();
        return pathString.substring(1, pathString.length() - ".class".length()).replace('/', '.');
    }

    private boolean shouldPatch(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".class");
    }

    @Override
    public byte[] readRawBytes(String name) throws IOException {
        return this.readBytes(this.input, name);
    }

    @Override
    public byte[] readClasspathBytes(String name) throws IOException {
        for (FileSystem classpath : this.classpath) {
            byte[] bytes = this.readBytes(classpath, name);
            if (bytes != null) {
                return bytes;
            }
        }
        return null;
    }

    private byte[] readBytes(FileSystem jar, String name) throws IOException {
        Path path = jar.getPath(name.replace('.', '/') + ".class");
        if (!Files.exists(path)) {
            return null;
        }
        return Files.readAllBytes(path);
    }

    @Override
    public void close() throws IOException {
        this.input.close();
    }
}
