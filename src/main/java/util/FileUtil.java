package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtil {
    
    public static void zipDirectory(String sourceDir, String zipFilePath) throws IOException {
        File sourceDirectory = new File(sourceDir);
        File zipFile = new File(zipFilePath);
        String zipCanonicalPath = zipFile.getCanonicalPath();

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            zipDirectoryRecursive(sourceDirectory, "", zos, zipCanonicalPath);
        }
    }
    
    private static void zipDirectoryRecursive(File dir, String parentPath, ZipOutputStream zos, String excludedZipPath) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.getCanonicalPath().equals(excludedZipPath)) {
                continue;
            }

            String path = parentPath + "/" + file.getName();
            if (file.isDirectory()) {
                zipDirectoryRecursive(file, path, zos, excludedZipPath);
            } else {
                addFileToZip(file, path, zos);
            }
        }
    }
    
    private static void addFileToZip(File file, String zipPath, ZipOutputStream zos) throws IOException {
        ZipEntry entry = new ZipEntry(zipPath.substring(1)); // Remove leading /
        zos.putNextEntry(entry);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
        }

        zos.closeEntry();
    }
    
    public static void unzipDirectory(String zipFilePath, String destDir) throws IOException {
        File dir = new File(destDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String rootPath = dir.getCanonicalPath() + File.separator;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(destDir + File.separator + entry.getName());
                String filePath = file.getCanonicalPath();

                if (!filePath.startsWith(rootPath)) {
                    throw new IOException("Invalid zip entry path: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parent = file.getParentFile();
                    if (parent != null) {
                        parent.mkdirs();
                    }

                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }

                zis.closeEntry();
            }
        }
    }
    
    public static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
}
