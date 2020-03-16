package com.gojek.daggers.postProcessors.longbow.storage;

import org.apache.hadoop.hbase.client.Scan;

public interface ScanRequest {
    Scan get();

    default Scan setScanRange(byte[] startRow, byte[] stopRow) {
        Scan scan = new Scan();
        scan.withStartRow(startRow, true);
        scan.withStopRow(stopRow, true);
        return scan;
    }

    String getTableId();
}