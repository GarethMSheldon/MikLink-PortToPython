# Changelog

## [Unreleased]

## [1.0.0] - 2026-04-04

### Added
- Initial release
- RouterOS v6.47.x native API support via librouteros v4
- Link status monitoring (speed, duplex, auto-negotiate)
- Network configuration detection (static/DHCP)
- Interface counter reset and before/after capture
- Cable TDR per-pair strand analysis via raw socket (bypasses librouteros
  blocking issue with cable-test's missing !done)
- LLDP/CDP neighbour discovery
- Ping via RouterOS API with local Windows fallback
- RouterOS bandwidth-server speed test (TCP + UDP)
- PDF report with traffic bar graphs
- JSON report output
- MD5 challenge/response login for RouterOS v6 raw socket sessions

### Fixed
- Cable-test no longer hangs indefinitely — raw socket with 8 s hard timeout
- librouteros v4 API call signature (`**kwargs` instead of positional strings)
- Interface counter filtering done in Python (API-side filters unreliable in v6)
- Neighbour filtering done in Python for same reason
- `/tool/ping` falls back to local `ping -n` when not available via API
