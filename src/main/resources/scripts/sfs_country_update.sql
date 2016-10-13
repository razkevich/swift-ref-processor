-- SFS_COUNTRY update
MERGE INTO sfs_country cc
USING (SELECT
         CASE WHEN isf.sepa = 'Y'
           THEN 1
         ELSE 0 END AS is_sepa,
         isf.iban_country_code,
         isf.bank_identifier_position,
         isf.iban_national_id_length,
         scc.country_name,
         isf.iban_total_length
       FROM sepa_ibanstructure_full isf
         JOIN sepa_country_code scc ON isf.iban_country_code = scc.country_code) s
ON (cc.code = s.iban_country_code)
WHEN MATCHED THEN UPDATE
SET cc.is_sepa = s.is_sepa,
  cc.start_from_symbol=s.bank_identifier_position,
  cc.national_id_length=s.iban_national_id_length,
  cc.country_name=s.country_name,
  cc.iban_total_length=s.iban_total_length;

COMMIT;
