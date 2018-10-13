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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DtfAggregator {
    private static final String DTF_DRIVER_DEFAULT_TMP_PREFIX = ".tmp";

    private String outputFileExt;

    private FileUtil util;

    private HashSet<String> existingFiles = new HashSet<String>();
    private String tempFileName;
    private long fileOffset;

    DtfAggregator(FileUtil util, String outputFileExt) throws IdMUnitException {
        this.util = util;
        this.outputFileExt = outputFileExt;

        List<FileUtil.FileInfo> newFiles = util.listFiles();
        for (FileUtil.FileInfo f : newFiles) {
            String filename = f.name;
            if (filename.endsWith(outputFileExt)) {
                existingFiles.add(filename);
            } else if (filename.endsWith(DTF_DRIVER_DEFAULT_TMP_PREFIX)) {
                tempFileName = filename;
                fileOffset = f.length;
            }
        }
    }

    InputStream getInputStream() throws IdMUnitException {
        return new ByteArrayInputStream(readNewData());
    }

    private byte[] readNewData() throws IdMUnitException {
        List<FileUtil.FileInfo> newFiles = util.listFiles();
        for (ListIterator<FileUtil.FileInfo> i = newFiles.listIterator(); i.hasNext(); ) {
            FileUtil.FileInfo f = i.next();

            String filename = f.name;
            if (existingFiles.contains(filename)) {
                i.remove();
            } else if (!filename.endsWith(outputFileExt) && !filename.endsWith(DTF_DRIVER_DEFAULT_TMP_PREFIX)) {
                i.remove();
            }
        }

        // Sort files oldest to newest
        Collections.sort(newFiles, new Comparator<FileUtil.FileInfo>() {
            public int compare(FileUtil.FileInfo file1, FileUtil.FileInfo file2) {
                if (file1.lastModified < file2.lastModified) {
                    return -1;
                }
                if (file1.lastModified > file2.lastModified) {
                    return 1;
                }
                // If the files have the same date-timestamp then the previous temp file is older
                if (tempFileName != null) {
                    String name = tempFileName.substring(0, tempFileName.length() - DTF_DRIVER_DEFAULT_TMP_PREFIX.length());
                    if (file1.name.startsWith(name)) {
                        return -1;
                    }
                    if (file2.name.startsWith(name)) {
                        return 1;
                    }
                }
                // If the files have the same date-timestamp then the file with a .tmp extension is newer
                boolean isFile1Temp = file1.name.endsWith(DTF_DRIVER_DEFAULT_TMP_PREFIX);
                boolean isFile2Temp = file2.name.endsWith(DTF_DRIVER_DEFAULT_TMP_PREFIX);
                if (isFile1Temp == isFile2Temp) {
                    return file1.name.compareTo(file2.name);
                }
                if (isFile1Temp) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        for (ListIterator<FileUtil.FileInfo> i = newFiles.listIterator(); i.hasNext(); ) {
            FileUtil.FileInfo f = i.next();
            // If previously read a temp file, check it for more data
            if (tempFileName != null) {
                if (i.previousIndex() == -1) {
                    throw new IdMUnitException("File '" + tempFileName + "' was not the newest file");
                }
                // The oldest (first in the list) "new" file should be the temp file last read from
                if (!f.name.startsWith(tempFileName.substring(0, tempFileName.length() - DTF_DRIVER_DEFAULT_TMP_PREFIX.length()))) {
                    String rolloverName = tempFileName.substring(0, tempFileName.length() - DTF_DRIVER_DEFAULT_TMP_PREFIX.length()) + outputFileExt;
                    throw new IdMUnitException("Expected file '" + tempFileName + "' or '" + rolloverName + "' to be the oldest file but '" + f.name + "' was the oldest");
                    // This could also happen if the file wasn't renamed and new files were added
                }

                if (f.name.endsWith(DTF_DRIVER_DEFAULT_TMP_PREFIX)) {
                    if (f.length == fileOffset) {
                        // If the temp file hasn't rolled over and we have already read the data in the file then there is nothing to do
                        return data.toByteArray();
                    }
                } else {
                    tempFileName = null;
                    existingFiles.add(f.name);
                    if (f.length == fileOffset) {
                        // No new data was written to the temp file and it rolled over, so move to the next oldest file
                        continue;
                    }
                }
            } else {
                // fileOffset needs to be reset when a temp file rolls over but there
                // are two code paths, either there is new data to read or there is
                // not. So this makes this the easiest place to reset it because we
                // are reading a new file so we should always start at 0.
                fileOffset = 0;
            }

            // Read all the available data
            byte[] b = util.readFile(f.name, fileOffset);
            try {
                data.write(b);
            } catch (IOException e) {
                throw new IdMUnitException("Error adding new data to buffer", e);
            }
            if (f.name.endsWith(DTF_DRIVER_DEFAULT_TMP_PREFIX)) {
                tempFileName = f.name;
                fileOffset += b.length;
            } else {
                existingFiles.add(f.name);
            }
        }

        return data.toByteArray();
    }
}
