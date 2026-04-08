package com.acme.docsync.storage;

import com.acme.docsync.model.IngestionValidationException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Writes files atomically using temp-file then replace.
 */
public final class AtomicFileWriter {
  /**
   * Writes a file atomically.
   *
   * @param target output file
   * @param writer writer callback
   */
  public void write(Path target, Consumer<OutputStream> writer) {
    Objects.requireNonNull(target, "target is required");
    Objects.requireNonNull(writer, "writer is required");
    Path parent = target.getParent();
    try {
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Path temp = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
      try (OutputStream output = Files.newOutputStream(temp)) {
        writer.accept(output);
      }
      Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new IngestionValidationException("atomic write failed: " + target);
    }
  }
}
