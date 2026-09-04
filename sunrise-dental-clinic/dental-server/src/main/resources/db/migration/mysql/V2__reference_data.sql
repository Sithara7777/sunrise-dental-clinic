-- =========================================================================
--  V2 : reference data (H2 profile)
-- =========================================================================
--  Dentists and the treatment catalogue are seeded here because they are
--  reference data the clinic operates on from day one, not test fixtures.
--
--  Staff LOGIN accounts are deliberately NOT seeded in SQL. Committing BCrypt
--  hashes to a Git repository - which Task D requires to be public - would
--  publish the clinic's credentials. They are created instead at first start-up
--  by StaffAccountInitializer, which hashes the passwords at run time.
-- =========================================================================


-- -------------------------------------------------------------------------
-- Dentists. Shift hours differ per dentist; the booking validation chain
-- uses them to reject an appointment outside that dentist's working day.
-- -------------------------------------------------------------------------
INSERT INTO dentist
    (dentist_code, full_name, specialization, contact_number, email,
     consultation_fee, slmc_registration_no, work_start_time, work_end_time,
     active, created_at, version)
VALUES
    ('DEN-001', 'Nimal Perera',           'General Dentistry',    '0112573101', 'nimal.perera@sunrisedental.lk',
     1500.00, 'SLMC/D/2009/1142', '08:00:00', '16:00:00', TRUE, CURRENT_TIMESTAMP, 0),
    ('DEN-002', 'Anusha Fernando',        'Orthodontics',         '0112573102', 'anusha.fernando@sunrisedental.lk',
     2500.00, 'SLMC/D/2012/2287', '09:00:00', '17:00:00', TRUE, CURRENT_TIMESTAMP, 0),
    ('DEN-003', 'Kasun Silva',            'Oral & Maxillofacial Surgery', '0112573103', 'kasun.silva@sunrisedental.lk',
     3000.00, 'SLMC/D/2008/0913', '10:00:00', '18:00:00', TRUE, CURRENT_TIMESTAMP, 0),
    ('DEN-004', 'Dilini Jayawardena',     'Paediatric Dentistry', '0112573104', 'dilini.j@sunrisedental.lk',
     2000.00, 'SLMC/D/2015/3341', '08:00:00', '14:00:00', TRUE, CURRENT_TIMESTAMP, 0),
    ('DEN-005', 'Rohan Wickramasinghe',   'Endodontics',          '0112573105', 'rohan.w@sunrisedental.lk',
     3500.00, 'SLMC/D/2011/1876', '12:00:00', '20:00:00', TRUE, CURRENT_TIMESTAMP, 0);


-- -------------------------------------------------------------------------
-- Treatment catalogue = the scenario's "treatment type" list.
-- pricing_strategy selects the billing rule the Strategy factory applies:
--   STANDARD  - price as listed
--   SURGICAL  - sterilisation and consumables surcharge
--   COSMETIC  - elective, no concession discounts
--   EMERGENCY - out-of-hours loading
-- Prices are indicative Colombo private-practice rates in LKR.
-- -------------------------------------------------------------------------
INSERT INTO treatment
    (code, name, description, category, base_price, duration_minutes,
     pricing_strategy, active, created_at, version)
VALUES
    ('CONSULT',  'General Consultation',
     'Examination, diagnosis and treatment planning.',                 'Diagnostic',     1500.00,  30, 'STANDARD',  TRUE, CURRENT_TIMESTAMP, 0),
    ('XRAY',     'Dental X-Ray (OPG)',
     'Full mouth panoramic radiograph.',                               'Diagnostic',     3500.00,  15, 'STANDARD',  TRUE, CURRENT_TIMESTAMP, 0),
    ('SCALING',  'Scaling and Polishing',
     'Ultrasonic removal of calculus followed by polishing.',          'Preventive',     6500.00,  45, 'STANDARD',  TRUE, CURRENT_TIMESTAMP, 0),
    ('FLUORIDE', 'Fluoride Application',
     'Topical fluoride varnish, commonly for paediatric patients.',    'Preventive',     4500.00,  30, 'STANDARD',  TRUE, CURRENT_TIMESTAMP, 0),
    ('FILLING',  'Composite Filling',
     'Tooth-coloured restoration of a decayed or fractured tooth.',    'Restorative',    8500.00,  45, 'STANDARD',  TRUE, CURRENT_TIMESTAMP, 0),
    ('RCT',      'Root Canal Treatment',
     'Endodontic treatment of an infected pulp.',                      'Endodontic',    35000.00,  90, 'SURGICAL',  TRUE, CURRENT_TIMESTAMP, 0),
    ('EXTRACT',  'Simple Extraction',
     'Removal of an erupted tooth under local anaesthetic.',           'Surgical',       7500.00,  30, 'SURGICAL',  TRUE, CURRENT_TIMESTAMP, 0),
    ('SURGEXT',  'Surgical Extraction (Wisdom Tooth)',
     'Surgical removal of an impacted third molar.',                   'Surgical',      25000.00,  90, 'SURGICAL',  TRUE, CURRENT_TIMESTAMP, 0),
    ('IMPLANT',  'Dental Implant',
     'Titanium implant fixture placement including surgical stent.',   'Surgical',     250000.00, 120, 'SURGICAL',  TRUE, CURRENT_TIMESTAMP, 0),
    ('CROWN',    'Porcelain Crown',
     'Full coverage porcelain-fused-to-metal crown.',                  'Prosthodontic', 45000.00,  60, 'STANDARD',  TRUE, CURRENT_TIMESTAMP, 0),
    ('DENTURE',  'Partial Denture',
     'Removable acrylic partial denture, per arch.',                   'Prosthodontic', 55000.00,  60, 'STANDARD',  TRUE, CURRENT_TIMESTAMP, 0),
    ('BRACES',   'Orthodontic Braces (Fitting)',
     'Fixed appliance bonding and first activation.',                  'Orthodontic',  150000.00, 120, 'STANDARD',  TRUE, CURRENT_TIMESTAMP, 0),
    ('WHITEN',   'Teeth Whitening',
     'In-chair LED-activated whitening, single session.',              'Cosmetic',      28000.00,  60, 'COSMETIC',  TRUE, CURRENT_TIMESTAMP, 0),
    ('VENEER',   'Dental Veneer',
     'Porcelain laminate veneer, per tooth.',                          'Cosmetic',      40000.00,  90, 'COSMETIC',  TRUE, CURRENT_TIMESTAMP, 0),
    ('EMERG',    'Emergency Pain Relief',
     'Same-day assessment and pain management.',                       'Emergency',      5000.00,  30, 'EMERGENCY', TRUE, CURRENT_TIMESTAMP, 0);


-- -------------------------------------------------------------------------
-- Identifier counters. The appointment and invoice keys carry the year and
-- are created on demand by SequenceGeneratorService each January.
-- -------------------------------------------------------------------------
INSERT INTO number_sequence (sequence_key, next_value) VALUES ('PATIENT', 1);
INSERT INTO number_sequence (sequence_key, next_value) VALUES ('DENTIST', 6);
