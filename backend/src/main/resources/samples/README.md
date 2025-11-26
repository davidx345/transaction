# Nigerian Bank CSV Format Samples

This folder contains sample CSV files demonstrating the expected formats for each supported Nigerian bank.

## Supported Banks

| Bank | Format Key | Date Format | Reference Prefix |
|------|------------|-------------|------------------|
| GTBank | `GTBank` | dd/MM/yyyy | GTB- |
| Access Bank | `AccessBank` | dd-MMM-yyyy | ACC- |
| Zenith Bank | `ZenithBank` | dd/MM/yyyy | ZEN- |
| First Bank | `FirstBank` | yyyy-MM-dd | FBN- |
| UBA | `UBA` | dd/MM/yyyy | UBA- |
| Paystack | `Paystack` | yyyy-MM-dd'T'HH:mm:ss | PSK_ |
| Flutterwave | `Flutterwave` | yyyy-MM-dd HH:mm:ss | FLW- |

## Auto-Detection

The system automatically detects the bank format based on:
1. Column headers (e.g., "TRANS REF", "VALUE DATE")
2. Reference patterns (e.g., "GTB-xxx", "PSK_xxx")
3. Date formats
4. Known bank identifiers in the data

## API Usage

### Upload with specific bank format:
```bash
curl -X POST http://localhost:8080/api/ingest/csv \
  -F "file=@bank_statement.csv" \
  -F "bank=GTBank"
```

### Upload with auto-detection:
```bash
curl -X POST http://localhost:8080/api/ingest/csv/auto \
  -F "file=@bank_statement.csv"
```

### Get supported banks:
```bash
curl http://localhost:8080/api/ingest/banks
```

## Sample Files

See the individual CSV files in this folder for examples of each bank's format.
