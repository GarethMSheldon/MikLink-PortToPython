@echo off
REM ============================================================
REM MikLink - example commands for Windows
REM ============================================================

REM Basic run - link, TDR, neighbours, no ping, no speed test
python miklink.py ^
  --host 192.168.88.1 ^
  --user admin ^
  --password "yourpassword" ^
  --interface ether1 ^
  --pdf report.pdf

REM Full run - all tests
python miklink.py ^
  --host 192.168.88.1 ^
  --user admin ^
  --password "yourpassword" ^
  --interface ether1 ^
  --ping 8.8.8.8,192.168.88.2 ^
  --speed-server 192.168.88.2 ^
  --pdf full_report.pdf ^
  --json full_report.json

REM TDR only - unplug far end before running for fault distances
python miklink.py ^
  --host 192.168.88.1 ^
  --user admin ^
  --password "yourpassword" ^
  --interface ether1 ^
  --no-link ^
  --no-lldp ^
  --pdf tdr_report.pdf

REM Quick link check only
python miklink.py ^
  --host 192.168.88.1 ^
  --user admin ^
  --password "yourpassword" ^
  --interface ether1 ^
  --no-tdr ^
  --no-lldp ^
  --pdf link_check.pdf
