-- Create table as select (CTAS) technique

-- STEP #1: initial view
CREATE OR REPLACE VIEW SEPA_CODEBOOK_SOURCE AS
  --INSERT INTO SEPA_CODEBOOK
  SELECT
    ipf.iban_iso_country_code AS country_code,
    ipf.iban_national_id      AS national_id,
    ipf.service_context       AS is_sepa,
    sdp.service_id            AS is_target2,
    CASE WHEN bdp.bic IS NOT NULL
      THEN bdp.bic
    ELSE ipf.iban_bic END     AS BIC,
    ipf.institution_name      AS bank_name,
    ipf.country_name          AS bank_country,
    bdp.city                  AS bank_city,
    bdp.zip_code              AS zip_Code,
    bdp.street_address_1      AS bank_address
  FROM sepa_ibanplus_v3_full ipf
    LEFT JOIN sepa_bankdirectoryplus_v3_full bdp ON ipf.iban_national_id = bdp.national_id
    LEFT JOIN (SELECT
                 participant_id,
                 service_id
               FROM sepa_servicedirectory_v1_full
               WHERE service_id = 'TARGET2'
               GROUP BY participant_id, service_id) sdp ON ipf.iban_bic = sdp.participant_id
  WHERE (bdp.iso_country_code = ipf.iban_iso_country_code OR bdp.iso_country_code IS NULL)
  ORDER BY ipf.iban_iso_country_code, ipf.iban_national_id;

CREATE SEQUENCE sepa_sequence START WITH 1 NOCYCLE NOCACHE;

-- STEP #2: data upload
-- long-lasting data upload occurs at this step

-- STEP #3: temp table via CTAS
SELECT sepa_sequence.nextval
FROM dual;
-- result = 2 -> tmp_sepacodebook2

-- TBD: additional table options (5 of them)
CREATE TABLE tmp_sepacodebook2 (
    id,
    country_code,
    national_id,
    is_sepa,
    is_target2,
    bic,
    bank_name,
    bank_city,
    bank_country,
    bank_address,
    zip_code
)
AS SELECT
     rownum,
     country_code,
     national_id,
     is_sepa,
     is_target2,
     bic,
     bank_name,
     bank_city,
     bank_country,
     bank_address,
     zip_code
   FROM SEPA_CODEBOOK_SOURCE;

CREATE INDEX IDX_tmp_SEPACODEBOOK2_1 ON tmp_sepaCODEBOOK2 (COUNTRY_CODE, NATIONAL_ID);
CREATE INDEX IDX_tmp_SEPACODEBOOK2_2 ON tmp_sepaCODEBOOK2 (ID);

-- final view
CREATE OR REPLACE VIEW sepa_codebook AS
  SELECT *
  FROM tmp_sepacodebook2;

-- STEP #4: cleanup
SELECT 'drop table ' || table_name || ';'
FROM user_tables
WHERE table_name LIKE 'TMP_SEPACODEBOOK%' AND table_name <> 'TMP_SEPACODEBOOK2';

DROP TABLE TMP_SEPACODEBOOK1;

-- STEP #5: work completed
SELECT *
FROM sepa_codebook;
