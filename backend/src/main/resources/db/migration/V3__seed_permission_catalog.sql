INSERT INTO permissions (key, module, description) VALUES
    ('USER_READ', 'USER', 'Consultar usuários'),
    ('USER_CREATE', 'USER', 'Criar usuários'),
    ('USER_UPDATE', 'USER', 'Atualizar usuários'),
    ('USER_STATUS_UPDATE', 'USER', 'Ativar ou desativar usuários'),
    ('ACCESS_PROFILE_READ', 'ACCESS_PROFILE', 'Consultar perfis de acesso'),
    ('ACCESS_PROFILE_CREATE', 'ACCESS_PROFILE', 'Criar perfis de acesso'),
    ('ACCESS_PROFILE_UPDATE', 'ACCESS_PROFILE', 'Atualizar perfis de acesso'),
    ('ACCESS_PROFILE_DELETE', 'ACCESS_PROFILE', 'Excluir perfis de acesso não utilizados'),
    ('ACCESS_PROFILE_ASSIGN', 'ACCESS_PROFILE', 'Associar perfis a usuários'),
    ('PERMISSION_READ', 'PERMISSION', 'Consultar catálogo de permissões')
ON CONFLICT (key) DO UPDATE SET
    module = EXCLUDED.module,
    description = EXCLUDED.description;

INSERT INTO access_profiles (id, name, description, active, system_profile, created_at, updated_at, version)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'ADMINISTRADOR',
    'Perfil protegido com todas as permissões do sistema',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO access_profile_permissions (access_profile_id, permission_key)
SELECT '00000000-0000-0000-0000-000000000001', key FROM permissions
ON CONFLICT (access_profile_id, permission_key) DO NOTHING;
