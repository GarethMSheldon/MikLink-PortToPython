# MikLink (Python CLI)

*A Python command-line port of the [MikLink Android app](https://github.com/ShitWRKS/MikLink) for RouterOS v6.x routers.*

This tool is **purely inspired** by the original Android project, which turns a MikroTik RouterBoard into a cable testing probe. The Python version achieves similar functionality using the native RouterOS API (port 8728) and outputs a PDF report — no Android device required.

---

## What it does

| Test | Description |
|------|-------------|
| **Link status** | Speed, duplex, auto-negotiate state |
| **Network config** | Static or DHCP address, gateway, DNS |
| **Traffic counters** | Resets interface counters, captures before/after snapshot with bar graphs in PDF |
| **Cable TDR** | Per-pair strand analysis (pair1–pair4), fault distance in metres |
| **LLDP / CDP neighbors** | Detects connected devices via neighbour discovery |
| **Ping** | RouterOS API ping with local Windows fallback |
| **Speed test** | TCP and UDP throughput via RouterOS bandwidth-server |
| **PDF report** | Full diagnostic report with traffic bar graphs |

---

## Requirements

- Python 3.9 or later (tested on 3.14)
- MikroTik RouterOS v6.x (tested on 6.47.10)
- RouterOS API enabled on the router

```
pip install librouteros fpdf2
```

---

## Router setup

Run these commands once on your MikroTik before using MikLink:

```
# Enable the API service
/ip service enable api
/ip service set api address=0.0.0.0/0

# Optional: enable bandwidth server for speed tests
/tool bandwidth-server set enabled=yes authenticate=no
```

Verify the API port is reachable from Windows:

```powershell
Test-NetConnection 192.168.88.1 -Port 8728
```

---

## Usage

### Basic run

```
python miklink.py --host 192.168.88.1 --user admin --password "yourpass" --interface ether1 --pdf report.pdf
```

### With ping and speed test

```
python miklink.py \
  --host 192.168.88.1 \
  --user admin \
  --password "yourpass" \
  --interface ether1 \
  --ping 8.8.8.8,192.168.88.2 \
  --speed-server 192.168.88.2 \
  --pdf report.pdf \
  --json report.json
```

### TDR only (unplug far end for fault distances)

```
python miklink.py \
  --host 192.168.88.1 \
  --user admin \
  --password "yourpass" \
  --interface ether1 \
  --no-link \
  --no-lldp \
  --pdf tdr_report.pdf
```

---

## All options

```
usage: miklink.py [-h] --host HOST [--user USER] [--password PASSWORD]
                  [--port PORT] [--https] [--interface INTERFACE]
                  [--no-link] [--no-tdr] [--no-lldp]
                  [--ping PING] [--ping-count PING_COUNT]
                  [--speed-server SPEED_SERVER] [--speed-user SPEED_USER]
                  [--speed-pass SPEED_PASS] [--speed-dur SPEED_DUR]
                  [--pdf PDF] [--json JSON]
```

| Argument | Default | Description |
|----------|---------|-------------|
| `--host` | required | RouterBoard IP address |
| `--user` | `admin` | RouterOS username |
| `--password` | `""` | RouterOS password |
| `--port` | `8728` | API port |
| `--https` | off | Use API-SSL on port 8729 |
| `--interface` | `ether1` | Interface to test |
| `--no-link` | off | Skip link status check |
| `--no-tdr` | off | Skip cable TDR test |
| `--no-lldp` | off | Skip neighbour discovery |
| `--ping` | — | Comma-separated ping targets |
| `--ping-count` | `4` | Packets per ping target |
| `--speed-server` | — | IP of bandwidth-server target |
| `--speed-user` | `admin` | Bandwidth-server username |
| `--speed-pass` | `""` | Bandwidth-server password |
| `--speed-dur` | `5` | Speed test duration in seconds |
| `--pdf` | — | Output PDF path |
| `--json` | — | Output JSON path |

---

## TDR cable testing

The TDR test uses a raw socket connection (bypasses librouteros) because RouterOS v6
cable-test never sends a `!done` sentence — it streams one result and goes silent.

**With link connected:** reports overall status (`link-ok`) but pair distances are not
available because the signal cannot reflect while a device is connected.

**For per-pair fault analysis:** unplug the cable at the far end, then run with `--no-link`:

```
python miklink.py --host 192.168.88.1 --user admin --password "x" \
  --interface ether1 --no-link --pdf tdr.pdf
```

Result example:

```
pair1: status=link-ok    dist=-
pair2: status=open       dist=14
pair3: status=link-ok    dist=-
pair4: status=short      dist=8
```

A distance value is the number of metres from the router port to the fault.

---

## PDF report sections

1. Header — board, RouterOS version, interface, timestamp, overall PASS/FAIL
2. Link Status — speed, duplex, auto-negotiate
3. Network Configuration — mode, address, gateway, DNS
4. Traffic Counters — before/after reset with Rx/Tx bar graphs and error/drop flags
5. Cable TDR — per-pair status and fault distances
6. LLDP/CDP Neighbors — identity, address, MAC, VLAN, PoE class
7. Ping Results — sent/received/loss/RTT per target
8. Speed Test — TCP and UDP throughput
9. Errors and Warnings

---

## Known limitations

- RouterOS v6 only. RouterOS v7 uses a REST API — a separate tool is needed.
- `/tool/ping` via the API may not be available on all v6 firmware builds.
  MikLink falls back to a local Windows `ping` automatically.
- TDR is only supported on physical copper ethernet ports that have a TDR
  capable PHY (most MikroTik hAP and CRS boards do).
- The bandwidth-server speed test runs the test against the router itself
  (loopback) or another RouterOS device — it does not test internet throughput.

---

## Tested on

| Hardware | RouterOS |
|----------|----------|
| hAP ac² (RBD52G-5HacD2HnD) | 6.47.10 |

---

## Acknowledgements

This Python CLI tool is **purely inspired** by the **MikLink Android app** created by **ShitWRKS**. The original Android project (for RouterOS 7.x) turns a MikroTik RouterBoard into a cable testing probe with a beautiful UI and PDF reporting. This Python version adapts the same concept for RouterOS v6.x and command-line automation.

Special thanks to the original authors for proving that a €100 MikroTik can replace a $2000 Fluke tester.

## License

MIT
