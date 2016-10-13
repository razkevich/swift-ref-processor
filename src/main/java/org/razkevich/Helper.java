package org.razkevich;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class Helper {

    private Helper() {
    }

    static boolean checkWithRegExp(String string, String pattern) {
        return Pattern.compile(pattern).matcher(string).matches();
    }

    static void printHelp(String e, Options options, org.slf4j.Logger logger) {
        if (e != null) logger.error(e);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Please specify correct parameters to run this application:", options);
    }

    static void unzip(String zipFile, String destinationFolder) throws IOException {
        File directory = new File(destinationFolder);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        byte[] buffer = new byte[2048];
        try {
            FileInputStream fInput = new FileInputStream(zipFile);
            ZipInputStream zipInput = new ZipInputStream(fInput);
            ZipEntry entry = zipInput.getNextEntry();
            while (entry != null) {
                String entryName = entry.getName();
                File file = new File(destinationFolder + File.separator + entryName);
                LoggerFactory.getLogger("").info("Unzip file " + entryName + " to " + file.getAbsolutePath());
                if (entry.isDirectory()) {
                    File newDir = new File(file.getAbsolutePath());
                    if (!newDir.exists()) {
                        boolean success = newDir.mkdirs();
                        if (!success) {
                            LoggerFactory.getLogger("").info("Problem creating Folder");
                        }
                    }
                } else {
                    FileOutputStream fOutput = new FileOutputStream(file);
                    int count;
                    while ((count = zipInput.read(buffer)) > 0) {
                        fOutput.write(buffer, 0, count);
                    }
                    fOutput.close();
                }
                zipInput.closeEntry();
                entry = zipInput.getNextEntry();
            }
            zipInput.closeEntry();
            zipInput.close();
            fInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
