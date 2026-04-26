# MikLink (Python GUI)

*A Python GUI port of the [MikLink Android app](https://github.com/ShitWRKS/MikLink) for RouterOS v6.x routers, built with CustomTkinter.*

This tool is **purely inspired** by the original Android project, which turns a MikroTik RouterBoard into a cable testing probe. The Python version achieves similar functionality using the native RouterOS API (port 8728) and outputs a PDF/JSON report — no Android device required, now with a graphical interface.

---

## What it does

| Test | Description |
|------|-------------|
| **Link status** | Speed, duplex, auto-negotiate state |
| **Network config** | Static or DHCP address, gateway, DNS |
| **Traffic counters** | Resets interface counters, captures before/after snapshot with bar graphs in PDF |
| **Cable TDR** | Per-pair strand analysis (pair1–pair4), fault distance in metres |
| **LLDP / CDP neighbors** | Detects connected devices via neighbour discovery |
| **Ping** | RouterOS API ping with local OS fallback (Windows/Linux) |
| **Speed test** | TCP and UDP throughput via RouterOS bandwidth-server |
| **PDF report** | Full diagnostic report with traffic bar graphs |
| **JSON export** | Structured data for further analysis |
| **Stop button** | Gracefully abort running tests |

---

## Requirements

- Python 3.9 or later (tested on 3.14)
- MikroTik RouterOS v6.x (tested on 6.47.10)
- RouterOS API enabled on the router

```
pip install librouteros fpdf2 customtkinter
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

Verify the API port is reachable from your computer:

```powershell
Test-NetConnection 192.168.88.1 -Port 8728
```

---

## Usage (GUI)

Launch the application:

```
python miklink_v15.py
```

### GUI fields

| Field | Description |
|-------|-------------|
| **ROUTER CONNECTION** | |
| IP address | RouterBoard IP (e.g., 192.168.88.1) |
| Username | RouterOS login (default `admin`) |
| Password | RouterOS password |
| Port | API port (8728 plain, 8729 SSL) |
| Use SSL | Check to connect via API-SSL |
| Interface | Ethernet port to test (e.g., `ether1`) |
| **TEST OPTIONS** | |
| Link Status | Check physical link state |
| Cable TDR (raw) | Run per-pair cable test |
| LLDP / Neighbors | Discover connected devices |
| Ping targets | Comma-separated IPs/hostnames |
| Speed-test server | IP of bandwidth-server (e.g., 192.168.88.2) |

### Buttons

- **RUN DIAGNOSTICS** – start the test suite
- **STOP** – cancel the current test (graceful abort)
- **Save PDF** – export report to PDF after run
- **Save JSON** – export raw data to JSON

### Live log

All test outputs appear in the bottom text box, with timestamps.

---

## TDR cable testing

The TDR test uses a raw socket connection (bypasses librouteros) because RouterOS v6
cable-test never sends a `!done` sentence — it streams one result and goes silent.

**With link connected:** reports overall status (`link-ok`) but pair distances are not
available because the signal cannot reflect while a device is connected.

**For per-pair fault analysis:** unplug the cable at the far end, then run the test.
Uncheck "Link Status" to avoid misleading results.

Example result in log:

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
  MikLink falls back to a local OS ping automatically (Windows `ping -n`, Linux `ping -c`).
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

This Python tool is **purely inspired** by the **MikLink Android app** created by **ShitWRKS**. The original Android project (for RouterOS 7.x) turns a MikroTik RouterBoard into a cable testing probe with a beautiful UI and PDF reporting. This Python version adapts the same concept for RouterOS v6.x and adds a cross-platform GUI.

Special thanks to the original authors for proving that a €100 MikroTik can replace a $2000 Fluke tester.

## License

MIT
