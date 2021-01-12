/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.development.utils;

import java.io.File;
import org.apache.ignite.internal.pagemem.wal.WALIterator;
import org.apache.ignite.internal.pagemem.wal.WALPointer;
import org.apache.ignite.internal.pagemem.wal.record.DataRecord;
import org.apache.ignite.internal.pagemem.wal.record.TxRecord;
import org.apache.ignite.internal.pagemem.wal.record.UnwrapDataEntry;
import org.apache.ignite.internal.pagemem.wal.record.WALRecord;
import org.apache.ignite.internal.pagemem.wal.record.WALRecord.RecordType;
import org.apache.ignite.internal.processors.cache.GridCacheOperation;
import org.apache.ignite.internal.processors.cache.persistence.tree.io.PageIO;
import org.apache.ignite.internal.processors.cache.persistence.wal.FileWriteAheadLogManager;
import org.apache.ignite.internal.processors.cache.persistence.wal.reader.IgniteWalIteratorFactory;
import org.apache.ignite.internal.processors.query.h2.database.io.H2ExtrasInnerIO;
import org.apache.ignite.internal.processors.query.h2.database.io.H2ExtrasLeafIO;
import org.apache.ignite.internal.processors.query.h2.database.io.H2InnerIO;
import org.apache.ignite.internal.processors.query.h2.database.io.H2LeafIO;
import org.apache.ignite.internal.processors.query.h2.database.io.H2MvccInnerIO;
import org.apache.ignite.internal.processors.query.h2.database.io.H2MvccLeafIO;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.logger.NullLogger;
import static org.apache.ignite.IgniteSystemProperties.IGNITE_TO_STRING_INCLUDE_SENSITIVE;
import static org.apache.ignite.IgniteSystemProperties.getEnum;
import static org.apache.ignite.development.utils.ProcessSensitiveData.HIDE;
import static org.apache.ignite.development.utils.ProcessSensitiveData.SHOW;

/**
 * Print WAL log data in human-readable form for detectanomy.
 */
public class IgniteWalConverterFordetectanomy extends IgniteWalConverter {

    public static void main(String[] args) throws Exception {
        if (args.length < 2)
            throw new IllegalArgumentException("\nYou need to provide:\n" +
                    "\t1. Size of pages, which was selected for file store (1024, 2048, 4096, etc).\n" +
                    "\t2. Path to dir with wal files.\n" +
                    "\t3. (Optional) Path to dir with archive wal files.");

        PageIO.registerH2(H2InnerIO.VERSIONS, H2LeafIO.VERSIONS, H2MvccInnerIO.VERSIONS, H2MvccLeafIO.VERSIONS);
        H2ExtrasInnerIO.register();
        H2ExtrasLeafIO.register();

        boolean printRecords = true;
        boolean printStat = false;
        ProcessSensitiveData sensitiveData = getEnum(SENSITIVE_DATA, SHOW); //TODO read them from argumetns

        if (printRecords && HIDE == sensitiveData)
            System.setProperty(IGNITE_TO_STRING_INCLUDE_SENSITIVE, Boolean.FALSE.toString());

        final IgniteWalIteratorFactory factory = new IgniteWalIteratorFactory(new NullLogger());

        final File walWorkDirWithConsistentId = new File(args[1]);

        final File[] workFiles = walWorkDirWithConsistentId.listFiles(FileWriteAheadLogManager.WAL_SEGMENT_FILE_FILTER);

        if (workFiles == null)
            throw new IllegalArgumentException("No .wal files in dir: " + args[1]);

        IgniteWalIteratorFactory.IteratorParametersBuilder iteratorParametersBuilder =
                new IgniteWalIteratorFactory.IteratorParametersBuilder().filesOrDirs(workFiles)
                    .pageSize(Integer.parseInt(args[0]));

        try (WALIterator stIt = factory.iterator(iteratorParametersBuilder)) {
            while (stIt.hasNextX()) {
                IgniteBiTuple<WALPointer, WALRecord> next = stIt.nextX();

                final WALRecord record = next.get2();

                if (printRecords)
                    System.out.println(toString(record));
            }
        }

        if (args.length >= 3) {
            final File walArchiveDirWithConsistentId = new File(args[2]);

            try (WALIterator stIt = factory.iterator(walArchiveDirWithConsistentId)) {
                while (stIt.hasNextX()) {
                    IgniteBiTuple<WALPointer, WALRecord> next = stIt.nextX();

                    final WALRecord record = next.get2();

                    if (printRecords)
                        System.out.println(toString(record));
                }
            }
        }

        System.err.flush();

        System.exit(0);
    }

    private static String toString(WALRecord walRecord) {
        String walString = "";        
        if (walRecord.type() == RecordType.DATA_RECORD) {
            DataRecord record = (DataRecord) walRecord;
            String type = record.type().name();
            String timestamp = String.valueOf(record.timestamp());
            UnwrapDataEntry dataEntry = (UnwrapDataEntry) record.writeEntries().get(record.writeEntries().size() - 1);
            String writeVer = dataEntry.writeVersion().toString();
            String op = dataEntry.op().name();
            String cacheId = String.valueOf(dataEntry.cacheId());
            String k = dataEntry.unwrappedKey().toString();
            String v = "";
            if (dataEntry.op() != GridCacheOperation.DELETE)
                v = dataEntry.unwrappedValue().toString();
            walString = type + ", " + timestamp + ", " + writeVer + ", " + op + ", " + cacheId + ", " + k + ", " + v;
        } else if (walRecord.type() == RecordType.TX_RECORD) {
            TxRecord record = (TxRecord) walRecord;
            String type = record.type().name();
            String timestamp = String.valueOf(record.timestamp());
            String writeVer = record.writeVersion().toString();
            String state = record.state().name();
            walString = type + ", " + timestamp + ", " + writeVer + ", " + state;
        } 
        return walString;
    }
}