<?php
/**
 * POST /api/logout.php  (Authorization: Bearer <token>)
 * Revokes this phone's token. Records stay on the server.
 */

declare(strict_types=1);
require __DIR__ . '/../lib/bootstrap.php';

require_method('POST');
$user = require_user();
db()->prepare('UPDATE auth_tokens SET revoked = 1 WHERE id = ?')->execute([$user['token_id']]);
respond(['ok' => true]);
