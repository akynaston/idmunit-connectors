/*
 * IdMUnit - Automated Testing Framework for Identity Management Solutions
 * Copyright (c) 2005-2018 TriVir, LLC
 *
 * This program is licensed under the terms of the GNU General Public License
 * Version 2 (the "License") as published by the Free Software Foundation, and
 * the TriVir Licensing Policies (the "License Policies").  A copy of the License
 * and the Policies were distributed with this program.
 *
 * The License is available at:
 * http://www.gnu.org/copyleft/gpl.html
 *
 * The Policies are available at:
 * http://www.idmunit.org/licensing/index.html
 *
 * Unless required by applicable law or agreed to in writing, this program is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied.  See the License and the Policies
 * for specific language governing the use of this program.
 *
 * www.TriVir.com
 * TriVir LLC
 * 13890 Braddock Road
 * Suite 310
 * Centreville, Virginia 20121
 *
 */

package com.trivir.idmunit.connector;

import org.idmunit.IdMUnitException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class LocalFileUtil implements FileUtil {
    private File readPath;
    private String writePath;

    public LocalFileUtil(String readPath, String writePath) {
        this.readPath = new File(readPath);
        this.writePath = writePath;
    }

    public void close() {
    }

    public List<FileInfo> listFiles() throws IdMUnitException {
        String[] dirEntries = readPath.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        });

        ArrayList<FileInfo> newFiles = new ArrayList<FileInfo>();
        for (String filename : dirEntries) {
            File f = new File(readPath, filename);
            newFiles.add(new FileInfo(filename, f.length(), f.lastModified()));
        }

        return newFiles;
    }

    public byte[] readFile(String filename, long skip) throws IdMUnitException {
        byte[] b;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(new File(readPath, filename));
        } catch (FileNotFoundException e) {
            throw new IdMUnitException("Error opening file", e);
        }
        try {
            if (skip != 0) {
                long bytesSkipped = fileInputStream.skip(skip);
                if (bytesSkipped != skip) {
                    throw new IdMUnitException("Not enough bytes skipped in '" + filename + "'.");
                }
            }
            int bytesAvailable = fileInputStream.available();
            if (bytesAvailable <= 0) {
                throw new IdMUnitException("The file '" + filename + "' has more data but no bytes are available to read.");
            }
            b = new byte[bytesAvailable];
//            b = new byte[1];
            int bytesRead = fileInputStream.read(b);
            if (bytesRead == -1) {
                // This should never happen since bytesAvailable was not 0
                throw new IdMUnitException("End of file '" + filename + "' was reached before all data (" + bytesAvailable + ") was read.");
            }
            if (bytesRead != b.length) {
                throw new IdMUnitException("Less data was read from '" + filename + "' than was available.");
            }
        } catch (IOException e) {
            throw new IdMUnitException("Error reading from file", e);
        } finally {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                throw new IdMUnitException("Error closing file", e);
            }
        }

        return b;
    }

    public void writeFile(String filename, String contents) throws IdMUnitException {
        String fullFilename = writePath + File.separator + filename;
        BufferedWriter outputFile = null;
        try {
            outputFile = new BufferedWriter(new FileWriter(fullFilename, false));
            outputFile.write(contents);
            outputFile.flush();
        } catch (IOException e) {
            throw new IdMUnitException("Failed to write to the file: " + fullFilename, e);
        } finally {
            if (outputFile != null) {
                try {
                    outputFile.close();
                } catch (IOException e) {
                    throw new IdMUnitException("Failed to close the file: " + fullFilename, e);
                }
            }
        }
    }
}
