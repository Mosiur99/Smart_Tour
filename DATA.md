# Adding District Tour Data

District files are generated from `bangladesh_64_district_tourism_dataset_v4_with_transport_costs.json` at the project root.

## Regenerate all district files

```bash
python3 scripts/sync-districts-from-dataset.py
```

## v4 highlights

- **Transport costs** — estimated `costPerPerson` per bus/train option (BRTA baseline + planning ranges)
- **Official hotels** — named Parjatan/gov.bd listings where available (prices often TBD — verify live)
- **Spots, historical places, universities, foods** — same structure as v2

Rajshahi also merges verified Banalata/Dhumketu train fares and local hotels from the original tour plan PDF.

Restart the application after syncing.
