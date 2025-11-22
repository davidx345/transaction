package com.fintech.recon.service.ingestion;

import com.fintech.recon.domain.Transaction;
import java.io.InputStream;
import java.util.List;

public interface CsvParser {
    List<Transaction> parse(InputStream inputStream, String bankName);
}
