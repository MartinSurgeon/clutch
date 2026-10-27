<?php
/**
 * Shared setup for every endpoint: JSON in/out, errors as JSON, database connection, auth helpers.
 */

declare(strict_types=1);

error_reporting(E_ALL);
ini_set('display_errors', '0');

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');

final class ApiError extends Exception
{
    public function __construct(public readonly int $status, public readonly string $errorCode, string $message)
    {
        parent::__construct($message);
    }
}

set_exception_handler(function (Throwable $e): void {
    if ($e instanceof ApiError) {
        http_response_code($e->status);
        echo json_encode(['error' => ["code" => $e->errorCode, "message" => $e->getMessage()]]);
        return;
    }
    error_log('[clutch] ' . $e);
    http_response_code(500);
    echo json_encode(['error' => ['code' => 'server_error', 'message' => 'Something went wrong on the server. Please try again.']]);
});

// Some shared hosts ship PHP without mbstring; these fallbacks keep the API working anyway.
if (!function_exists('mb_strlen')) {
    function mb_strlen(string $s): int
    {
        return (int) preg_match_all('/./us', $s);
    }
}
if (!function_exists('mb_substr')) {
    function mb_substr(string $s, int $start, ?int $length = null): string
    {
        $chars = preg_split('//u', $s, -1, PREG_SPLIT_NO_EMPTY) ?: [];
        return implode('', array_slice($chars, $start, $length));
    }
}

function config(): array
{
    static $config = null;
    if ($config === null) {
        $config = require __DIR__ . '/../config.php';
    }
    return $config;
}

function db(): PDO
{
    static $pdo = null;
    if ($pdo === null) {
        $c = config();
        $dsn = "mysql:host={$c['db_host']};port={$c['db_port']};dbname={$c['db_name']};charset=utf8mb4";
        $pdo = new PDO($dsn, $c['db_user'], $c['db_pass'], [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false,
        ]);
    }
    return $pdo;
}

function require_method(string $method): void
{
    if (($_SERVER['REQUEST_METHOD'] ?? '') !== $method) {
        throw new ApiError(405, 'method_not_allowed', "Use $method for this endpoint.");
    }
}

function json_input(): array
{
    $raw = file_get_contents('php://input') ?: '';
    if (strlen($raw) > 8 * 1024 * 1024) {
        throw new ApiError(413, 'too_large', 'The upload is too large. Sync in smaller parts.');
    }
    $data = json_decode($raw, true);
    if (!is_array($data)) {
        throw new ApiError(400, 'bad_json', 'The request body must be JSON.');
    }
    return $data;
}

function respond(array $body, int $status = 200): never
{
    http_response_code($status);
    echo json_encode($body, JSON_UNESCAPED_UNICODE | JSON_PRESERVE_ZERO_FRACTION);
    exit;
}

function now_ms(): int
{
    return (int) floor(microtime(true) * 1000);
}

/**
 * Ghana phone numbers in one canonical form: 233XXXXXXXXX (12 digits).
 * Accepts 0241234567, +233 24 123 4567, 233241234567.
 */
function normalize_phone(string $input): string
{
    $digits = preg_replace('/\D+/', '', $input) ?? '';
    if (strlen($digits) === 10 && $digits[0] === '0') {
        $digits = '233' . substr($digits, 1);
    }
    if (!preg_match('/^233[235]\d{8}$/', $digits)) {
        throw new ApiError(422, 'invalid_phone', 'Enter a Ghana phone number, e.g. 024 123 4567.');
    }
    return $digits;
}

function client_ip(): string
{
    return substr((string)($_SERVER['REMOTE_ADDR'] ?? ''), 0, 45);
}

function issue_token(int $userId, string $deviceName): string
{
    $token = bin2hex(random_bytes(32));
    $stmt = db()->prepare('INSERT INTO auth_tokens (user_id, token_hash, device_name) VALUES (?, ?, ?)');
    $stmt->execute([$userId, hash('sha256', $token), mb_substr($deviceName, 0, 80)]);
    return $token;
}

function bearer_token(): ?string
{
    // Many cPanel / PHP-FPM / LiteSpeed hosts drop the Authorization header, so the app also
    // sends the token as X-Clutch-Token. Either one is accepted.
    $header = $_SERVER['HTTP_AUTHORIZATION']
        ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION']
        ?? (function_exists('getallheaders') ? (array_change_key_case(getallheaders())['authorization'] ?? null) : null);
    if (is_string($header) && preg_match('/^Bearer\s+([a-f0-9]{64})$/i', trim($header), $m)) {
        return strtolower($m[1]);
    }
    $fallback = $_SERVER['HTTP_X_CLUTCH_TOKEN'] ?? null;
    if (is_string($fallback) && preg_match('/^[a-f0-9]{64}$/i', trim($fallback))) {
        return strtolower(trim($fallback));
    }
    return null;
}

/** Returns the signed-in user, or stops with 401. */
function require_user(): array
{
    $token = bearer_token();
    if ($token === null) {
        throw new ApiError(401, 'unauthorized', 'Please sign in again.');
    }
    $stmt = db()->prepare(
        'SELECT u.id, u.phone, u.name, t.id AS token_id FROM auth_tokens t JOIN users u ON u.id = t.user_id
         WHERE t.token_hash = ? AND t.revoked = 0'
    );
    $stmt->execute([hash('sha256', $token)]);
    $user = $stmt->fetch();
    if (!$user) {
        throw new ApiError(401, 'unauthorized', 'Please sign in again.');
    }
    db()->prepare('UPDATE auth_tokens SET last_used_at = NOW() WHERE id = ?')->execute([$user['token_id']]);
    return $user;
}

function public_user(array $user): array
{
    return ['id' => (int)$user['id'], 'phone' => $user['phone'], 'name' => $user['name']];
}
