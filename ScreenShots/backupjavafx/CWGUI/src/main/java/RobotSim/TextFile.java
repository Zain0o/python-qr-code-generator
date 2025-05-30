package RobotSim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * The TextFile class provides utility methods for reading from and writing to text files.
 * It is designed to handle file input/output operations for saving and loading the arena state.
 */
public class TextFile {

    /**
     * Writes the given content to the specified file.
     * If the file exists, it will be overwritten; otherwise, a new file will be created.
     *
     * @param filename The name or path of the file to write to.
     * @param content  The content to write into the file.
     * @return True if the file was successfully written, false otherwise.
     * @throws IllegalArgumentException if filename or content is null.
     */
    public static boolean writeFile(String filename, String content) {
        if (filename == null || content == null) {
            throw new IllegalArgumentException("Filename and content cannot be null");
        }

        try {
            // Ensure the directory exists
            java.io.File file = new java.io.File(filename);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }

            // Write the file with UTF-8 encoding
            Files.writeString(Paths.get(filename), content, java.nio.charset.StandardCharsets.UTF_8);
            DebugUtils.logFileOperation("WRITE", filename, true);
            return true;
        } catch (IOException e) {
            // Log and return false if writing fails
            DebugUtils.logFileOperation("WRITE", filename, false);
            DebugUtils.log("Error writing file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reads and returns the content from the specified file.
     *
     * @param filename The name or path of the file to read.
     * @return The content of the file as a String.
     * @throws IllegalArgumentException if filename is null.
     * @throws RuntimeException if an IO error occurs while reading the file.
     */
    public static String readFile(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        try {
            // Read the file with UTF-8 encoding
            String content = Files.readString(Paths.get(filename), java.nio.charset.StandardCharsets.UTF_8);
            DebugUtils.logFileOperation("READ", filename, true);
            return content;
        } catch (IOException e) {
            // Log and throw a runtime exception if reading fails
            DebugUtils.logFileOperation("READ", filename, false);
            DebugUtils.log("Error reading file: " + e.getMessage());
            throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes the specified file.
     *
     * @param filename The name or path of the file to delete.
     * @return True if the file was successfully deleted, false otherwise.
     * @throws IllegalArgumentException if filename is null.
     */
    public static boolean deleteFile(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        try {
            // Attempt to delete the file if it exists
            boolean result = Files.deleteIfExists(Paths.get(filename));
            if (result) {
                System.out.println("File deleted successfully: " + filename);
                DebugUtils.logFileOperation("DELETE", filename, true);
            } else {
                System.out.println("File not found or could not be deleted: " + filename);
                DebugUtils.logFileOperation("DELETE", filename, false);
            }
            return result;
        } catch (IOException e) {
            // Log and return false if deletion fails
            System.err.println("Error deleting file: " + e.getMessage());
            DebugUtils.logFileOperation("DELETE", filename, false);
            return false;
        }
    }
}
