<?php
/**
 * End-to-end API test against a running server.
 * Run:  php backend/tests/api_test.php [base_url]
 * Default base_url: http://localhost/clutch/backend/api
 * Live server example: php backend/tests/api_test.php https://api.yourdomain.com
 * Creates a throwaway account and deletes it afterwards through the API (safe on production).
 */

declare(strict_types=1);

$base = rtrim($argv[1] ?? 'http://localhost/clutch/backend/api', '/');
$failures = 0;
$passes = 0;

function call(string $method, string $url, ?array $body = null, ?string $token = null, bool $tokenHeaderOnly = false): array
{
    $ch = curl_init($url);
    $headers = ['Content-Type: application/json'];
    if ($token && !$tokenHeaderOnly) $headers[] = "Authorization: Bearer $token";
    if ($token && $tokenHeaderOnly) $headers[] = "X-Clutch-Token: $token";
    curl_setopt_array($ch, [
        CURLOPT_CUSTOMREQUEST => $method,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_HTTPHEADER => $headers,
        CURLOPT_POSTFIELDS => $body === null ? null : json_encode($body),
    ]);
    $raw = curl_exec($ch);
    $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return [$status, json_decode((string)$raw, true) ?? ['raw' => $raw]];
}

function check(string $name, bool $ok, $detail = null): void
{
    global $failures, $passes;
    if ($ok) {
        $passes++;
        echo "  ok   $name\n";
    } else {
        $failures++;
        echo "  FAIL $name " . json_encode($detail) . "\n";
    }
}

$phoneLocal = '024' . str_pad((string)random_int(0, 9999999), 7, '0', STR_PAD_LEFT);
$phoneCanonical = '233' . substr($phoneLocal, 1);
$t0 = 1_700_000_000_000;

echo "Clutch API test → $base ($phoneLocal)\n";

// --- Server ready? ---
[$s, $r] = call('GET', "$base/health.php");
check('health check passes', $s === 200 && ($r['ok'] ?? false) === true, [$s, $r]);
if ($s !== 200) {
    echo "\nServer is not ready: " . json_encode($r) . "\n";
    exit(1);
}
if (!($r['checks']['https'] ?? false)) {
    echo "  note not served over HTTPS (fine for local testing only)\n";
}

// --- Accounts ---
[$s, $r] = call('POST', "$base/register.php", ['phone' => $phoneLocal, 'name' => 'Test Farmer', 'password' => 'pass123', 'device' => 'Test phone A']);
check('register creates account', $s === 201 && strlen($r['token'] ?? '') === 64 && ($r['user']['phone'] ?? '') === $phoneCanonical, [$s, $r]);
$tokenA = $r['token'] ?? '';

[$s] = call('POST', "$base/register.php", ['phone' => '+233 ' . substr($phoneLocal, 1), 'name' => 'Again', 'password' => 'pass123']);
check('same number in another format is refused', $s === 409, $s);

[$s, $r] = call('POST', "$base/register.php", ['phone' => '12345', 'name' => 'X', 'password' => 'pass123']);
check('bad phone number is refused', $s === 422 && ($r['error']['code'] ?? '') === 'invalid_phone', [$s, $r]);

[$s] = call('POST', "$base/register.php", ['phone' => '0550000001', 'name' => 'X', 'password' => '123']);
check('short password is refused', $s === 422, $s);

[$s] = call('POST', "$base/login.php", ['phone' => $phoneLocal, 'password' => 'wrong!!']);
check('wrong password is refused', $s === 401, $s);

[$s, $r] = call('POST', "$base/login.php", ['phone' => $phoneLocal, 'password' => 'pass123', 'device' => 'Test phone B']);
check('login on a second phone', $s === 200 && strlen($r['token'] ?? '') === 64, [$s, $r]);
$tokenB = $r['token'] ?? '';

[$s] = call('GET', "$base/me.php");
check('no token is refused', $s === 401, $s);

[$s, $r] = call('GET', "$base/me.php", null, $tokenA);
check('me shows empty counts', $s === 200 && ($r['counts']['dailyRecords'] ?? -1) === 0, [$s, $r]);

[$s] = call('GET', "$base/me.php", null, $tokenA, tokenHeaderOnly: true);
check('token also works via X-Clutch-Token (hosts that drop Authorization)', $s === 200, $s);

// --- Sync: phone A pushes ---
$flock = ['id' => 'flock_1', 'name' => 'Pen A', 'breed' => 'Lohmann Brown', 'hensHoused' => 500, 'placedDate' => '2026-07-01',
    'ageWeeksAtPlacement' => 17, 'archived' => false, 'updatedAt' => $t0, 'deleted' => false];
$record = ['id' => 'flock_1_2026-10-20', 'flockId' => 'flock_1', 'date' => '2026-10-20', 'liveHens' => 495, 'eggs' => 452,
    'cracked' => 4, 'dirty' => 5, 'softShell' => 1, 'deaths' => 1, 'culls' => 0, 'feedKg' => 57.5, 'waterL' => 108.0,
    'avgEggWeightG' => 61.2, 'maxTempC' => null, 'bodyWeightG' => 1950.0, 'uniformityPct' => 82.5, 'notes' => 'Vaccinated',
    'enteredBy' => 'local_farmer', 'enteredByName' => 'Farmer', 'updatedAt' => $t0, 'deleted' => false];
$note = ['id' => 'flock_1_w32', 'flockId' => 'flock_1', 'ageWeek' => 32, 'notes' => 'Hot week', 'actions' => 'Extra drinkers',
    'updatedAt' => $t0, 'deleted' => false];

[$s, $r] = call('POST', "$base/sync.php", ['since' => 0, 'changes' => ['flocks' => [$flock], 'dailyRecords' => [$record], 'weeklyNotes' => [$note]]], $tokenA);
check('push is accepted', $s === 200 && ($r['accepted']['dailyRecords'] ?? []) === ['flock_1_2026-10-20'], [$s, $r]);
$cursorA = $r['serverTime'] ?? 0;

// --- Phone B restores everything ---
[$s, $r] = call('POST', "$base/sync.php", ['since' => 0, 'changes' => new stdClass()], $tokenB);
$pulled = $r['changes']['dailyRecords'][0] ?? [];
check('second phone restores all rows', $s === 200 && count($r['changes']['flocks'] ?? []) === 1 && count($r['changes']['weeklyNotes'] ?? []) === 1, [$s, $r]);
check('restored values keep types', ($pulled['eggs'] ?? null) === 452 && ($pulled['feedKg'] ?? null) === 57.5
    && array_key_exists('maxTempC', $pulled) && $pulled['maxTempC'] === null && ($pulled['waterL'] ?? null) === 108.0 && ($pulled['uniformityPct'] ?? null) === 82.5 && ($pulled['deleted'] ?? null) === false, $pulled);
$cursorB = $r['serverTime'] ?? 0;

// --- Last write wins ---
$older = $flock;
$older['name'] = 'Old name';
$older['updatedAt'] = $t0 - 1000;
[$s, $r] = call('POST', "$base/sync.php", ['since' => $cursorB, 'changes' => ['flocks' => [$older]]], $tokenB);
$returnedName = $r['changes']['flocks'][0]['name'] ?? null;
check('older change is rejected and server copy returned', $s === 200 && ($r['accepted']['flocks'] ?? ['x']) === [] && $returnedName === 'Pen A', [$s, $r]);

$newer = $flock;
$newer['hensHoused'] = 520;
$newer['updatedAt'] = $t0 + 5000;
$deletedNote = $note;
$deletedNote['deleted'] = true;
$deletedNote['updatedAt'] = $t0 + 5000;
[$s, $r] = call('POST', "$base/sync.php", ['since' => $cursorB, 'changes' => ['flocks' => [$newer], 'weeklyNotes' => [$deletedNote]]], $tokenB);
check('newer change and delete are accepted', $s === 200 && ($r['accepted']['flocks'] ?? []) === ['flock_1'] && ($r['accepted']['weeklyNotes'] ?? []) === ['flock_1_w32'], [$s, $r]);

// --- Phone A pulls only what changed since its cursor ---
[$s, $r] = call('POST', "$base/sync.php", ['since' => $cursorA, 'changes' => new stdClass()], $tokenA);
check('cursor pull returns only changed rows', $s === 200 && count($r['changes']['dailyRecords'] ?? ['x']) === 0
    && ($r['changes']['flocks'][0]['hensHoused'] ?? 0) === 520 && ($r['changes']['weeklyNotes'][0]['deleted'] ?? false) === true, [$s, $r]);

[$s, $r] = call('GET', "$base/me.php", null, $tokenA);
check('deleted note no longer counted', ($r['counts']['weeklyNotes'] ?? -1) === 0 && ($r['counts']['flocks'] ?? -1) === 1, $r);

// --- Validation ---
$bad = $record;
unset($bad['eggs']);
[$s] = call('POST', "$base/sync.php", ['since' => 0, 'changes' => ['dailyRecords' => [$bad]]], $tokenA);
check('row with missing number is refused', $s === 422, $s);

// --- Sign out ---
[$s] = call('POST', "$base/logout.php", null, $tokenB);
[$s2] = call('GET', "$base/me.php", null, $tokenB);
check('signed-out token stops working', $s === 200 && $s2 === 401, [$s, $s2]);

// --- Rate limit on wrong passwords ---
$last = 0;
for ($i = 0; $i < 6; $i++) {
    [$last] = call('POST', "$base/login.php", ['phone' => $phoneLocal, 'password' => 'nope' . $i]);
}
check('too many wrong passwords are slowed down', $last === 429, $last);

// --- Private files are not served (only checkable when the whole backend folder is public) ---
if (str_ends_with($base, '/api')) {
    [$s] = call('GET', preg_replace('#/api$#', '/config.php', $base));
    check('config.php is not downloadable', $s === 403 || $s === 404, $s);
} else {
    echo "  skip config.php check (API folder is the web root, so config.php is outside it)\n";
}

// --- Delete the throwaway account through the API ---
[$s] = call('POST', "$base/delete_account.php", ['password' => 'wrong!!'], $tokenA);
check('delete account needs the right password', $s === 403, $s);
[$s] = call('POST', "$base/delete_account.php", ['password' => 'pass123'], $tokenA);
[$s2] = call('GET', "$base/me.php", null, $tokenA);
check('delete account removes it', $s === 200 && $s2 === 401, [$s, $s2]);
[$s] = call('POST', "$base/login.php", ['phone' => $phoneLocal, 'password' => 'pass123']);
check('deleted account cannot sign in', $s === 401 || $s === 429, $s);

echo "\n$passes passed, $failures failed\n";
exit($failures === 0 ? 0 : 1);
