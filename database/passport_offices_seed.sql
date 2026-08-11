USE passport_db;

-- Seed data for verified PSK/POPSK offices only.
-- This script preserves the existing passport_offices table and avoids duplicate inserts.
-- Latitude/longitude are NULL because the verified Passport Seva/MEA sources used here did not provide coordinates.

-- RPO Coimbatore
-- Official cross-check: Passport Seva RPO Coimbatore page confirms the RPO jurisdiction includes Coimbatore,
-- Erode, Namakkal, Salem, The Nilgiris and related districts.
-- Location/address source used for PSK/POPSK rows: Passport Seva locator data as published from
-- passportindia.gov.in and surfaced in the PSK/RPO contact listing updated 28-Mar-2026.
-- Reference: https://www.passportindia.gov.in/psp/RPO/CoimbatoreRPO
-- Reference: https://olaw.in/passport?q=Coimbatore
INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'PSK Coimbatore', 'PSK', 'Coimbatore', 'Tamil Nadu', NULL, NULL,
       '25, AGT Business Park, Avinashi Road, Civil Aerodrome Post, Coimbatore', '641014', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='PSK Coimbatore' AND city='Coimbatore' AND pincode='641014'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Salem', 'POPSK', 'Salem', 'Tamil Nadu', NULL, NULL,
       'Head Post Office, 143 Victoria Market Road, Near to Old Bus Stand, Salem', '636001', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Salem' AND city='Salem' AND pincode='636001'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Rasipuram', 'POPSK', 'Rasipuram', 'Tamil Nadu', NULL, NULL,
       'Post Office, Jahir Hussain Salai, Near to Old Bus Stand, Rasipuram, Namakkal Dt.', '637408', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Rasipuram' AND city='Rasipuram' AND pincode='637408'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Erode', 'POPSK', 'Erode', 'Tamil Nadu', NULL, NULL,
       'Head Post Office, 76, Gandhiji Road, Erode', '638001', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Erode' AND city='Erode' AND pincode='638001'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Coonoor', 'POPSK', 'Coonoor', 'Tamil Nadu', NULL, NULL,
       'Head Post Office, 8 Bedford Circle, Coonoor', '643101', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Coonoor' AND city='Coonoor' AND pincode='643101'
);

-- RPO Tiruchirappalli
-- Official cross-check: Passport Seva RPO Tiruchirappalli page confirms RPO jurisdiction and office location.
-- Location/address source used for PSK rows: Passport Seva locator data as published from
-- passportindia.gov.in and surfaced in the PSK/RPO contact listing updated 28-Mar-2026.
-- Reference: https://www.passportindia.gov.in/psp/RPO/TiruchirappalliRPO
-- Reference: https://olaw.in/passport?q=Tiruchirappalli
INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'PSK Tiruchirappalli', 'PSK', 'Tiruchirappalli', 'Tamil Nadu', NULL, NULL,
       'Plot No. A5, Salai Road and Shastri Road Junction, Thillai Nagar, Tiruchirappalli', '620018', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='PSK Tiruchirappalli' AND city='Tiruchirappalli' AND pincode='620018'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'PSK Thanjavur', 'PSK', 'Thanjavur', 'Tamil Nadu', NULL, NULL,
       'New No. 201/7D, Old No. 201/7, Medical College Road, Next to Kumaran Cinema Theatre, Thanjavur', '613004', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='PSK Thanjavur' AND city='Thanjavur' AND pincode='613004'
);

-- RPO Chennai
-- Official cross-check: Passport Seva RPO Chennai page confirms RPO jurisdiction including Chennai,
-- Tiruvallur, Kanchipuram, Chengalpattu, Ranipet, Vellore, Krishnagiri, Dharmapuri,
-- Tiruvannamalai, Villupuram, Kallakurichi, Cuddalore and related districts.
-- Location/address source used for PSK/POPSK rows: Passport Seva locator data as published from
-- passportindia.gov.in and surfaced in the PSK/RPO contact listing updated 28-Mar-2026.
-- Reference: https://www.passportindia.gov.in/psp/RPO/ChennaiRPO
-- Reference: https://olaw.in/passport?q=Chennai
INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Arani', 'POPSK', 'Arani', 'Tamil Nadu', NULL, NULL,
       'No. 2A, Suriyakulam North Street, Arani', '632301', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Arani' AND city='Arani' AND pincode='632301'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Chennai GPO', 'POPSK', 'Chennai', 'Tamil Nadu', NULL, NULL,
       '1/10, Rajaji Salai, Chennai', '600001', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Chennai GPO' AND city='Chennai' AND pincode='600001'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Chidambaram', 'POPSK', 'Chidambaram', 'Tamil Nadu', NULL, NULL,
       'No. 98, North Car Street, Chidambaram', '608001', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Chidambaram' AND city='Chidambaram' AND pincode='608001'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Cuddalore', 'POPSK', 'Cuddalore', 'Tamil Nadu', NULL, NULL,
       'Cuddalore HPO, Nellikuppam Road, Manjakuppam, Cuddalore', '607001', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Cuddalore' AND city='Cuddalore' AND pincode='607001'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Dharmapuri', 'POPSK', 'Dharmapuri', 'Tamil Nadu', NULL, NULL,
       'No. 16 A, Nachiappa Street, Dharmapuri Bazaar, Dharmapuri', '636701', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Dharmapuri' AND city='Dharmapuri' AND pincode='636701'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Kallakurichi', 'POPSK', 'Kallakurichi', 'Tamil Nadu', NULL, NULL,
       'Gandhi Road, Kallakurichi', '606202', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Kallakurichi' AND city='Kallakurichi' AND pincode='606202'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Kancheepuram', 'POPSK', 'Kancheepuram', 'Tamil Nadu', NULL, NULL,
       'HPO, No. 46, Railway Road, Kanchipuram', '631501', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Kancheepuram' AND city='Kancheepuram' AND pincode='631501'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Krishnagiri', 'POPSK', 'Krishnagiri', 'Tamil Nadu', NULL, NULL,
       'No. 18, Dharmaraja Koyil Street, Krishnagiri', '635001', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Krishnagiri' AND city='Krishnagiri' AND pincode='635001'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Periyar Nagar', 'POPSK', 'Chennai', 'Tamil Nadu', NULL, NULL,
       'Post Office Passport Seva Kendra Periyar Nagar SO, Balasubramanyam Street, Periyar Nagar West, Perambur, Chennai', '600082', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Periyar Nagar' AND city='Chennai' AND pincode='600082'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Ranipet', 'POPSK', 'Ranipet', 'Tamil Nadu', NULL, NULL,
       'HPO Ranipet, Arcot Road, Ranipet', '632401', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Ranipet' AND city='Ranipet' AND pincode='632401'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Tiruvallur', 'POPSK', 'Tiruvallur', 'Tamil Nadu', NULL, NULL,
       'No. 37, J.N. Road, Tiruvallur', '602001', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Tiruvallur' AND city='Tiruvallur' AND pincode='602001'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Tiruvannamalai', 'POPSK', 'Tiruvannamalai', 'Tamil Nadu', NULL, NULL,
       'Tiruvannamalai HPO, Thiyagi Annamalaiyar Street, Tindivanam Road, Tiruvannamalai', '606601', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Tiruvannamalai' AND city='Tiruvannamalai' AND pincode='606601'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Vellore', 'POPSK', 'Vellore', 'Tamil Nadu', NULL, NULL,
       'Vellore Head Post Office, Officers Lane, Anna Salai, Vellore', '632001', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Vellore' AND city='Vellore' AND pincode='632001'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Villupuram', 'POPSK', 'Villupuram', 'Tamil Nadu', NULL, NULL,
       'Villupuram HPO, 161, Kamarajar Street, Villupuram', '606602', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Villupuram' AND city='Villupuram' AND pincode='606602'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'PSK Aminjikarai', 'PSK', 'Chennai', 'Tamil Nadu', NULL, NULL,
       'Navins Presidium, No. 103, Nelson Manickam Road, Aminjikarai, Chennai', '600030', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='PSK Aminjikarai' AND city='Chennai' AND pincode='600030'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'PSK Saligramam', 'PSK', 'Chennai', 'Tamil Nadu', NULL, NULL,
       'No. 1, Bhanumathi Ramakrishna Road, Saligramam, Chennai', '600093', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='PSK Saligramam' AND city='Chennai' AND pincode='600093'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'PSK Tambaram', 'PSK', 'Chennai', 'Tamil Nadu', NULL, NULL,
       'Claret Complex, Duraisamy Reddy Street, Tambaram, Chennai', '600045', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='PSK Tambaram' AND city='Chennai' AND pincode='600045'
);

-- RPO Madurai
-- Official cross-check: Passport Seva RPO Madurai page confirms the RPO has 02 PSKs and 08 POPSKs,
-- and confirms the RPO jurisdiction over Madurai, Dindigul, Sivaganga, Virudhunagar,
-- Ramanathapuram, Thoothukudi, Tirunelveli, Tenkasi, Theni and Kanyakumari districts.
-- Location/address source used for PSK/POPSK rows: Passport Seva locator data as published from
-- passportindia.gov.in and surfaced in the PSK/RPO contact listing updated 28-Mar-2026.
-- Reference: https://www.passportindia.gov.in/psp/RPO/MaduraiRPO
-- Reference: https://olaw.in/passport?q=Madurai
INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'PSK Madurai', 'PSK', 'Madurai', 'Tamil Nadu', NULL, NULL,
       'Claret Plaza, Melakkal Main Road, Kochadai, Madurai', '625016', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='PSK Madurai' AND city='Madurai' AND pincode='625016'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'PSK Tirunelveli City', 'PSK', 'Tirunelveli', 'Tamil Nadu', NULL, NULL,
       'Mukesh Towers, 13, South Byepass Road, Xavier Colony, Tirunelveli', '627005', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='PSK Tirunelveli City' AND city='Tirunelveli' AND pincode='627005'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Bodinayakkanur', 'POPSK', 'Bodinayakkanur', 'Tamil Nadu', NULL, NULL,
       'Head Post Office, Bodinayakkanur, Theni District', '625513', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Bodinayakkanur' AND city='Bodinayakkanur' AND pincode='625513'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Devakottai', 'POPSK', 'Devakottai', 'Tamil Nadu', NULL, NULL,
       'Head Post Office, Thirupathur Road, Devakottai', '630302', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Devakottai' AND city='Devakottai' AND pincode='630302'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Kodai Road', 'POPSK', 'Kodai Road', 'Tamil Nadu', NULL, NULL,
       'Kodai Road Sub Post Office, Kodai Road, Dindigul District', '624206', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Kodai Road' AND city='Kodai Road' AND pincode='624206'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Nagercoil', 'POPSK', 'Nagercoil', 'Tamil Nadu', NULL, NULL,
       'Nagercoil Head Post Office, No. 79, North Car Street, Nagercoil', '629001', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Nagercoil' AND city='Nagercoil' AND pincode='629001'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Rajapalayam', 'POPSK', 'Rajapalayam', 'Tamil Nadu', NULL, NULL,
       'Head Post Office, Rajapalayam, Virudhunagar District', '626117', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Rajapalayam' AND city='Rajapalayam' AND pincode='626117'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Ramanathapuram', 'POPSK', 'Ramanathapuram', 'Tamil Nadu', NULL, NULL,
       'Ramanathapuram Head Post Office', '623501', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Ramanathapuram' AND city='Ramanathapuram' AND pincode='623501'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Thoothukkudi', 'POPSK', 'Thoothukkudi', 'Tamil Nadu', NULL, NULL,
       'Head Post Office, Postmaster Quarters, Tiruchendur Road, Thoothukkudi', '628001', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Thoothukkudi' AND city='Thoothukkudi' AND pincode='628001'
);

INSERT INTO passport_offices (office_name, office_type, city, state, latitude, longitude, address, pincode, active)
SELECT 'POPSK Virudhunagar', 'POPSK', 'Virudhunagar', 'Tamil Nadu', NULL, NULL,
       'Virudhunagar Head Post Office, No. 15 A A Road, Pandiyan Nagar, Virudhunagar', '626001', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM passport_offices
    WHERE office_name='POPSK Virudhunagar' AND city='Virudhunagar' AND pincode='626001'
);
