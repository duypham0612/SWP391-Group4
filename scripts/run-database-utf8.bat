@echo off
setlocal

set "SERVER=%~1"
set "PASSWORD=%~2"

if "%SERVER%"=="" set "SERVER=localhost,1433"
if "%PASSWORD%"=="" set "PASSWORD=123"

pushd "%~dp0.."

where sqlcmd >nul 2>nul
if errorlevel 1 (
    echo Khong tim thay sqlcmd. Hay cai SQL Server Command Line Utilities hoac chay bang SSMS.
    popd
    exit /b 1
)

echo Dang tao/lap lai database CafeChain tren %SERVER% voi encoding UTF-8...
sqlcmd -S "%SERVER%" -U sa -P "%PASSWORD%" -f 65001 -I -i "sql\database.sql" -b
set "ERR=%ERRORLEVEL%"

if not "%ERR%"=="0" (
    echo.
    echo Loi khi chay database. Kiem tra SQL Server, user sa/password, va TCP/IP port.
    popd
    exit /b %ERR%
)

echo.
echo Hoan tat. Du lieu tieng Viet da duoc import bang UTF-8.
popd
endlocal
