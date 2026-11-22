# GameParty report screenshots via Chrome DevTools Protocol (ASCII only on purpose: PowerShell 5.1 reads BOM-less files as ANSI)
param(
  [string]$Base = "http://localhost:8081",
  [string]$Out,
  [string]$AdminPw,
  [string]$Theme = "",      # "light" or "dark": stored before every page load, like the toggle button does
  [switch]$Quick            # only the main screens
)
$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $Out | Out-Null

$chrome = "C:\Program Files\Google\Chrome\Application\chrome.exe"
$port = 9333
$profile = Join-Path $env:TEMP "gp-chrome-profile"
if (Test-Path $profile) { Remove-Item $profile -Recurse -Force -ErrorAction SilentlyContinue }
$proc = Start-Process $chrome -PassThru -ArgumentList @(
  "--headless=new", "--disable-gpu", "--hide-scrollbars", "--no-first-run", "--no-default-browser-check",
  "--remote-debugging-port=$port", "--user-data-dir=$profile", "--window-size=1440,900", "about:blank")

try {
  $ws = $null
  for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Milliseconds 300
    try {
      $targets = Invoke-RestMethod "http://127.0.0.1:$port/json/list"
      $page = $targets | Where-Object { $_.type -eq "page" } | Select-Object -First 1
      if ($page) { $ws = $page.webSocketDebuggerUrl; break }
    } catch { }
  }
  if (-not $ws) { throw "Chrome DevTools endpoint did not come up" }

  $client = New-Object System.Net.WebSockets.ClientWebSocket
  $client.ConnectAsync([Uri]$ws, [Threading.CancellationToken]::None).Wait()
  $script:id = 0

  function Recv-Message {
    $ms = New-Object System.IO.MemoryStream
    $buf = New-Object byte[] 262144
    do {
      $seg = [System.ArraySegment[byte]]::new($buf)
      $res = $client.ReceiveAsync($seg, [Threading.CancellationToken]::None).Result
      $ms.Write($buf, 0, $res.Count)
    } while (-not $res.EndOfMessage)
    return [Text.Encoding]::UTF8.GetString($ms.ToArray())
  }

  function Cdp($method, $params = @{}) {
    $script:id++
    $json = (@{ id = $script:id; method = $method; params = $params } | ConvertTo-Json -Depth 12 -Compress)
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    $client.SendAsync([System.ArraySegment[byte]]::new($bytes), [System.Net.WebSockets.WebSocketMessageType]::Text, $true, [Threading.CancellationToken]::None).Wait()
    while ($true) {
      $m = Recv-Message
      if ($m -match '^\{"id":(\d+),') {
        if ([int]$matches[1] -eq $script:id) { return $m }
      }
    }
  }

  function Eval($js) {
    return Cdp "Runtime.evaluate" @{ expression = $js; awaitPromise = $true; returnByValue = $true }
  }

  function EvalJson($js) {
    $m = Eval $js
    $obj = $m | ConvertFrom-Json
    return $obj.result.result.value
  }

  function Go($url, $settle = 600) {
    Cdp "Page.navigate" @{ url = $url } | Out-Null
    Start-Sleep -Milliseconds 400
    for ($i = 0; $i -lt 60; $i++) {
      $state = EvalJson "document.readyState"
      if ($state -eq "complete") { break }
      Start-Sleep -Milliseconds 250
    }
    # lazy images below the fold never load in a fixed viewport: make them eager, and never wait more than 6s
    Eval "document.querySelectorAll('img[loading=lazy]').forEach(i => { i.loading = 'eager'; }); Promise.race([Promise.all([document.fonts.ready].concat([...document.images].map(i => i.complete ? 1 : new Promise(r => { i.onload = i.onerror = r; })))), new Promise(r => setTimeout(r, 6000))]).then(() => 1)" | Out-Null
    Start-Sleep -Milliseconds $settle
  }

  function SetSize($h) {
    Cdp "Emulation.setDeviceMetricsOverride" @{ width = 1440; height = [int]$h; deviceScaleFactor = 1; mobile = $false } | Out-Null
  }

  function Save-Shot($name) {
    $r = Cdp "Page.captureScreenshot" @{ format = "png" }
    if ($r -match '"data":"([^"]+)"') {
      $path = Join-Path $Out ($name + ".png")
      [IO.File]::WriteAllBytes($path, [Convert]::FromBase64String($matches[1]))
      Write-Output ("saved " + $path)
    } else { throw "no screenshot data for $name" }
  }

  # full = whole page height (capped), otherwise just the 1440x900 viewport
  function Shot($name, $path, $full = $true, $settle = 700) {
    SetSize 900
    Go ($Base + $path) $settle
    if ($full) {
      $lm = Cdp "Page.getLayoutMetrics"
      $h = 900
      if ($lm -match '"cssContentSize":\{[^}]*"height":([\d.]+)') { $h = [math]::Min([double]$matches[1], 2600) }
      SetSize ([math]::Max([math]::Ceiling($h), 900))
      Start-Sleep -Milliseconds 500
    }
    Save-Shot $name
    SetSize 900
  }

  function Login($u, $p) {
    Cdp "Network.clearBrowserCookies" | Out-Null
    Go ($Base + "/login") 300
    $js = "document.querySelector('input[name=username]').value='$u'; document.querySelector('input[name=password]').value='$p'; document.querySelector('form[action$=""/login""]').requestSubmit(); 'ok'"
    Eval $js | Out-Null
    Start-Sleep -Milliseconds 2200
  }

  # run a same-origin POST (with CSRF token) from inside the page
  function PostForm($path, $tokenPage, $fields = "{}") {
    $js = "(async () => { const html = async p => (await fetch(p, {credentials:'same-origin'})).text(); const csrf = h => h.match(/name=""_csrf""[^>]*value=""([^""]+)""/)[1]; const t = csrf(await html('$tokenPage')); const r = await fetch('$path', {method:'POST', credentials:'same-origin', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body:new URLSearchParams(Object.assign($fields, {_csrf:t}))}); return r.status; })()"
    return EvalJson $js
  }

  # ---------------- public pages ----------------
  Cdp "Network.enable" | Out-Null
  if ($Theme) {
    Cdp "Page.enable" | Out-Null
    Cdp "Page.addScriptToEvaluateOnNewDocument" @{ source = "try { localStorage.setItem('gp-theme', '$Theme'); } catch (e) { }" } | Out-Null
  }
  Shot "01-main" "/" $false 3000
  if (-not $Quick) { Shot "02-login" "/login" $false }
  Shot "03-signup" "/signup" $true
  Shot "04-parties" "/parties" $true 1200
  Shot "17-parties-filter" "/parties?open=true&sort=ending" $true 1200
  Shot "21-error-404" "/no-such-page" $false 800

  # ---------------- member (dummy01) ----------------
  Login "dummy01" "dummy1234!"
  # an open party hosted by dummy01
  Go ($Base + "/mypage") 300
  $hostedId = EvalJson "(() => { const rows = [...document.querySelectorAll('.table tbody tr')]; const r = rows.find(x => x.querySelector('.badge-recruiting')); return r ? r.querySelector('a').getAttribute('href').split('/').pop() : ''; })()"
  Write-Output ("dummy01 hosted open party: " + $hostedId)

  if (-not $Quick) { Shot "05-party-create" "/parties/new" $true 1200 }
  Shot "06-party-review" "/parties/33" $true 1000
  Shot "07-mypage" "/mypage" $true 1000
  if (-not $Quick) { Shot "08-mypage-edit" "/mypage/edit" $true 1200 }
  if (-not $Quick) { Shot "20-mypage-withdraw" "/mypage/withdraw" $true 800 }

  # pending application so the host screen shows approve / reject (restored afterwards)
  if ($hostedId -and -not $Quick) {
    Login "dummy12" "dummy1234!"
    $st = PostForm "/parties/$hostedId/apply" "/parties/$hostedId"
    Write-Output ("dummy12 apply status: " + $st)
    Login "dummy01" "dummy1234!"
    Shot "09-party-host" "/parties/$hostedId" $true 1000
    Login "dummy12" "dummy1234!"
    $st = PostForm "/parties/$hostedId/cancel" "/parties/$hostedId"
    Write-Output ("dummy12 cancel status (restore): " + $st)
    # applicant view of an open party
    Shot "10-party-apply" "/parties/$hostedId" $true 1000
  }

  # ---------------- notifications (a temporary account applies, the host approves; everything is undone afterwards) ----------------
  if ($hostedId -and -not $Quick) {
    $guest = "shotguest"
    $guestPw = "Shot1234x"
    Cdp "Network.clearBrowserCookies" | Out-Null
    Go ($Base + "/signup") 300
    $st = PostForm "/signup" "/signup" "{username:'$guest',password:'$guestPw',passwordConfirm:'$guestPw',nickname:'shotguest'}"
    Write-Output ("shotguest signup status: " + $st)
    Login $guest $guestPw
    $st = PostForm "/parties/$hostedId/apply" "/parties/$hostedId"
    Write-Output ("shotguest apply status: " + $st)
    # host side: the new application shows up under the news panel and the header badge
    Login "dummy01" "dummy1234!"
    Shot "18-mypage-news-host" "/mypage" $true 1000
    Go ($Base + "/parties/$hostedId") 300
    $appId = EvalJson "(() => { const f = [...document.querySelectorAll('form[action$=""/approve""]')].find(x => x.closest('li').textContent.includes('shotguest')); return f ? f.getAttribute('action').split('/').slice(-2)[0] : ''; })()"
    Write-Output ("shotguest application id: " + $appId)
    if ($appId) {
      $st = PostForm "/parties/$hostedId/applications/$appId/approve" "/parties/$hostedId"
      Write-Output ("approve status: " + $st)
      # applicant side: the result shows up
      Login $guest $guestPw
      Shot "19-mypage-news-applicant" "/mypage" $true 1000
      $st = PostForm "/parties/$hostedId/cancel" "/parties/$hostedId"
      Write-Output ("shotguest cancel status (restore): " + $st)
    }
    $st = PostForm "/mypage/withdraw" "/mypage/withdraw" "{password:'$guestPw'}"
    Write-Output ("shotguest withdraw status (cleanup): " + $st)
  }

  # ---------------- admin ----------------
  Login "admin" $AdminPw
  Shot "11-admin-dashboard" "/admin" $true 800
  if (-not $Quick) {
    Shot "12-admin-members" "/admin/members" $true 800
    Shot "13-admin-member-detail" "/admin/members/4" $true 800
  }
  Shot "14-admin-parties" "/admin/parties" $true 800
  if (-not $Quick) { Shot "15-admin-reviews" "/admin/reviews" $true 800 }

  # confirm dialog (open it, capture, never confirm)
  SetSize 900
  Go ($Base + "/admin/parties?status=RECRUITING") 800
  Eval "document.querySelector('form[data-confirm] button').click(); 'ok'" | Out-Null
  Start-Sleep -Milliseconds 700
  Save-Shot "16-confirm-dialog"
}
finally {
  if ($client) { try { $client.Dispose() } catch { } }
  if ($proc -and -not $proc.HasExited) { Stop-Process -Id $proc.Id -Force }
  Start-Sleep -Milliseconds 500
  if (Test-Path $profile) { Remove-Item $profile -Recurse -Force -ErrorAction SilentlyContinue }
}
