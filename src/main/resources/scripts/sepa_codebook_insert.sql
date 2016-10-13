-- SEPA_CODEBOOK data infill
INSERT INTO sepa_codebook
  SELECT
    rownum                    AS id,
    ipf.iban_iso_country_code AS country_code,
    ipf.iban_national_id      AS national_id,
    CASE WHEN ipf.service_context = 'SEPA'
      THEN 1
    ELSE 0 END                AS isSepa,
CASE WHEN exists(SELECT *
                   FROM T100_sepa_servicedirectory_v1 sdp
                   WHERE service_id = 'TARGET2' AND ipf.iban_bic = sdp.participant_id)
    THEN 1 ELSE 0 END         AS isTarget2,
    ipf.iban_bic              AS BIC,
    ipf.institution_name      AS bankName,
    bdp.city                  AS bankCity,
    ipf.country_name          AS bankCountry,
    bdp.street_address_1      AS bankAddress,
    bdp.zip_code              AS zipCode
  FROM sepa_ibanplus_v3_full ipf
    LEFT JOIN sepa_bankdirectoryplus_v3_full bdp ON ipf.iban_bic = bdp.bic
  ORDER BY ipf.iban_iso_country_code, ipf.iban_national_id;

COMMIT;
