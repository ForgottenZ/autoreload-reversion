package com.example.worldrestore;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class WorldRestoreService {
    private WorldRestoreService() {
    }

    public static void restoreWorld(MinecraftServer server) {
        if (!WorldRestoreConfig.VALUES.enabled.get()) {
            WorldRestoreMod.LOGGER.info("World restore is disabled by configuration.");
            WorldRestoreState.recordSkipped("Disabled by configuration.");
            return;
        }

        Instant start = Instant.now();
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        Path templateZip = resolveTemplatePath(WorldRestoreConfig.VALUES.templateZipPath.get());

        WorldRestoreMod.LOGGER.info("Preparing to restore world. worldDir={}, templateZip={}", worldDir, templateZip);

        if (!Files.exists(templateZip)) {
            String message = "Template zip does not exist: " + templateZip;
            WorldRestoreMod.LOGGER.error(message);
            if (WorldRestoreConfig.VALUES.failHardIfMissing.get()) {
                throw new IllegalStateException(message);
            }
            WorldRestoreState.recordFailure(message, Duration.between(start, Instant.now()).toMillis());
            return;
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("worldrestore");
            extractZip(templateZip, tempDir);
            Path templateRoot = detectWorldRoot(tempDir);

            boolean preserveServerConfig = WorldRestoreConfig.VALUES.preserveServerConfigDir.get();
            deleteWorldContents(worldDir, preserveServerConfig);
            int copiedFiles = copyTemplate(templateRoot, worldDir, preserveServerConfig);

            long durationMs = Duration.between(start, Instant.now()).toMillis();
            String message = "World restore completed. Copied " + copiedFiles + " files.";
            WorldRestoreMod.LOGGER.info("{} Took {} ms.", message, durationMs);
            WorldRestoreState.recordSuccess(message, durationMs);
        } catch (Exception exception) {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            String message = "World restore failed: " + exception.getMessage();
            WorldRestoreMod.LOGGER.error(message, exception);
            WorldRestoreState.recordFailure(message, durationMs);
            if (WorldRestoreConfig.VALUES.failHardIfMissing.get()) {
                throw new RuntimeException("Stopping server due to restore failure.", exception);
            }
        } finally {
            if (tempDir != null) {
                try {
                    deleteDirectory(tempDir);
                } catch (IOException ioException) {
                    WorldRestoreMod.LOGGER.warn("Failed to delete temp directory: {}", tempDir, ioException);
                }
            }
        }
    }

    private static Path resolveTemplatePath(String configuredPath) {
        Path path = Paths.get(configuredPath);
        if (path.isAbsolute()) {
            return path;
        }
        return FMLPaths.GAMEDIR.get().resolve(path).normalize();
    }

    private static void extractZip(Path zipPath, Path destination) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path resolved = destination.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(destination)) {
                    throw new IOException("Blocked zip slip entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    Files.createDirectories(resolved.getParent());
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        Files.copy(inputStream, resolved, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private static Path detectWorldRoot(Path extractionRoot) throws IOException {
        if (Files.exists(extractionRoot.resolve("level.dat"))) {
            return extractionRoot;
        }

        List<Path> directories = new ArrayList<>();
        try (var stream = Files.list(extractionRoot)) {
            stream.filter(Files::isDirectory).forEach(directories::add);
        }

        if (directories.size() == 1) {
            Path candidate = directories.get(0);
            if (Files.exists(candidate.resolve("level.dat"))) {
                return candidate;
            }
        }

        for (Path candidate : directories) {
            if (Files.exists(candidate.resolve("level.dat"))) {
                return candidate;
            }
        }

        throw new IOException("Could not find level.dat in template zip.");
    }

    private static void deleteWorldContents(Path worldDir, boolean preserveServerConfig) throws IOException {
        Path serverConfigDir = worldDir.resolve("serverconfig");
        Path sessionLock = worldDir.resolve("session.lock");

        Files.walkFileTree(worldDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir.equals(worldDir)) {
                    return FileVisitResult.CONTINUE;
                }
                if (preserveServerConfig && dir.startsWith(serverConfigDir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.equals(sessionLock)) {
                    return FileVisitResult.CONTINUE;
                }
                if (preserveServerConfig && file.startsWith(serverConfigDir)) {
                    return FileVisitResult.CONTINUE;
                }
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (dir.equals(worldDir)) {
                    return FileVisitResult.CONTINUE;
                }
                if (preserveServerConfig && dir.startsWith(serverConfigDir)) {
                    return FileVisitResult.CONTINUE;
                }
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static int copyTemplate(Path templateRoot, Path worldDir, boolean preserveServerConfig) throws IOException {
        Path sessionLock = worldDir.resolve("session.lock");
        Path serverConfigDir = worldDir.resolve("serverconfig");
        int[] count = new int[]{0};

        Files.walkFileTree(templateRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = templateRoot.relativize(dir);
                if (relative.toString().isEmpty()) {
                    return FileVisitResult.CONTINUE;
                }
                if (preserveServerConfig && relative.startsWith("serverconfig")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Path targetDir = worldDir.resolve(relative);
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = templateRoot.relativize(file);
                if (file.getFileName().toString().equals("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }
                if (preserveServerConfig && relative.startsWith("serverconfig")) {
                    return FileVisitResult.CONTINUE;
                }
                Path target = worldDir.resolve(relative);
                if (target.equals(sessionLock)) {
                    return FileVisitResult.CONTINUE;
                }
                Files.createDirectories(target.getParent());
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                count[0]++;
                return FileVisitResult.CONTINUE;
            }
        });

        return count[0];
    }

    private static void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
