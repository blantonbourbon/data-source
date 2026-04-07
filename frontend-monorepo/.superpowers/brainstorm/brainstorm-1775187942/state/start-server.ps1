$env:BRAINSTORM_DIR = 'D:\projects\frontend-monorepo\.superpowers\brainstorm\brainstorm-1775187942'
$env:BRAINSTORM_HOST = '127.0.0.1'
$env:BRAINSTORM_URL_HOST = 'localhost'
Remove-Item Env:BRAINSTORM_OWNER_PID -ErrorAction SilentlyContinue
Set-Location 'C:\Users\lov3c\.codex\superpowers\skills\brainstorming\scripts'
node 'C:\Users\lov3c\.codex\superpowers\skills\brainstorming\scripts\server.cjs'
