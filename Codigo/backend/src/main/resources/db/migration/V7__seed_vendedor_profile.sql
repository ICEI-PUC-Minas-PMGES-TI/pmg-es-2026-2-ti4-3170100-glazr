INSERT INTO access_profiles (id, name, description, active, system_profile, created_at, updated_at, version)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'VENDEDOR',
    'Perfil de vendedor para consulta do catálogo de kits e produtos',
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO access_profile_permissions (access_profile_id, permission_key)
VALUES ('00000000-0000-0000-0000-000000000002', 'KIT_READ')
ON CONFLICT (access_profile_id, permission_key) DO NOTHING;
