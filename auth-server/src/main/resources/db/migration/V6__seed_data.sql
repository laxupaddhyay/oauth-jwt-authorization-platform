INSERT INTO app_role (name, description) VALUES
  ('ROLE_USER',  'Standard user'),
  ('ROLE_ADMIN', 'Administrator');

INSERT INTO oauth_scope (name, description, is_openid) VALUES
  ('openid',         'OpenID Connect sign-in',            TRUE),
  ('profile',        'Basic profile information',         TRUE),
  ('email',          'Email address',                     TRUE),
  ('phone',          'Phone number',                      TRUE),
  ('offline_access', 'Keep access using refresh tokens',  FALSE);

INSERT INTO oauth_scope_claim (scope_name, claim_name) VALUES
  ('openid','sub'),
  ('profile','name'), ('profile','given_name'), ('profile','family_name'),
  ('profile','preferred_username'), ('profile','picture'), ('profile','locale'),
  ('profile','zoneinfo'), ('profile','updated_at'),
  ('email','email'), ('email','email_verified'),
  ('phone','phone_number'), ('phone','phone_number_verified');
