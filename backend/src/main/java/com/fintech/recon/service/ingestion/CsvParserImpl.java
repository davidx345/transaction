package com.fintech.recon.service.ingestion;

import com.fintech.recon.domain.Transaction;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CsvParserImpl implements CsvParser {

    private final Map<String, BankCsvStrategy> strategies;

    public CsvParserImpl(List<BankCsvStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(BankCsvStrategy::getBankName, Function.identity()));
    }

    @Override
    public List<Transaction> parse(InputStream inputStream, String bankName) {
        return Optional.ofNullable(strategies.get(bankName))
                .map(strategy -> strategy.parse(inputStream))
                .orElseThrow(() -> new IllegalArgumentException("Unsupported bank: " + bankName));
    }
}
