#!/usr/bin/env python3
"""
miklink_v6.py - RouterOS v6.47.x cable & network diagnostics tool
Requires: pip install librouteros fpdf2
Enable API:  /ip service enable api
             /ip service set api address=0.0.0.0/0
Bandwidth server (for speed test):
             /tool bandwidth-server set enabled=yes authenticate=no
"""
from __future__ import annotations
import argparse
import json
import socket
import struct
import subprocess
import sys
import time
from dataclasses import dataclass, field, asdict
from datetime import datetime
from typing import Any, Optional

try:
    from librouteros import connect
    from librouteros.exceptions import ConnectionClosed, FatalError, TrapError
except ImportError as e:
    print(f"ERROR: Failed to import 'librouteros'.\n  {e}")
    print("Fix: python -m pip install librouteros")
    sys.exit(1)


# ---------------------------------------------------------------------------
# RouterOS raw API helpers (used for cable-test which never sends !done)
# ---------------------------------------------------------------------------

def _ros_encode_length(n: int) -> bytes:
    if n < 0x80:
        return bytes([n])
    elif n < 0x4000:
        n |= 0x8000
        return struct.pack("!H", n)
    elif n < 0x200000:
        n |= 0xC00000
        return struct.pack("!I", n)[1:]
    elif n < 0x10000000:
        n |= 0xE0000000
        return struct.pack("!I", n)
    else:
        return b'\xf0' + struct.pack("!I", n)

def _ros_encode_word(word: str) -> bytes:
    b = word.encode("utf-8")
    return _ros_encode_length(len(b)) + b

def _ros_encode_sentence(words: list) -> bytes:
    out = b"".join(_ros_encode_word(w) for w in words)
    out += b'\x00'
    return out

def _ros_read_length(sock) -> int:
    b = sock.recv(1)
    if not b:
        return 0
    first = b[0]
    if first < 0x80:
        return first
    elif first < 0xC0:
        second = sock.recv(1)[0]
        return ((first & 0x3F) << 8) | second
    elif first < 0xE0:
        rest = sock.recv(2)
        return ((first & 0x1F) << 16) | (rest[0] << 8) | rest[1]
    elif first < 0xF0:
        rest = sock.recv(3)
        return ((first & 0x0F) << 24) | (rest[0] << 16) | (rest[1] << 8) | rest[2]
    else:
        rest = sock.recv(4)
        return struct.unpack("!I", rest)[0]

def _ros_read_sentence(sock) -> list:
    words = []
    while True:
        length = _ros_read_length(sock)
        if length == 0:
            break
        word = b""
        remaining = length
        while remaining > 0:
            chunk = sock.recv(remaining)
            if not chunk:
                raise ConnectionError("Socket closed mid-word")
            word += chunk
            remaining -= len(chunk)
        words.append(word.decode("utf-8", errors="replace"))
    return words

def _ros_login_v6(sock, username: str, password: str) -> None:
    """MD5 challenge/response login for RouterOS v6."""
    import hashlib
    sock.sendall(_ros_encode_sentence(["/login"]))
    resp = _ros_read_sentence(sock)
    challenge = None
    for word in resp:
        if word.startswith("=ret="):
            challenge = word[5:]
            break
    if challenge is None:
        raise ConnectionError(f"Login challenge not received: {resp}")
    challenge_bytes = bytes.fromhex(challenge)
    md5 = hashlib.md5()
    md5.update(b'\x00')
    md5.update(password.encode("utf-8"))
    md5.update(challenge_bytes)
    response = "00" + md5.hexdigest()
    sock.sendall(_ros_encode_sentence([
        "/login",
        f"=name={username}",
        f"=response={response}",
    ]))
    resp2 = _ros_read_sentence(sock)
    if not resp2 or resp2[0] != "!done":
        raise ConnectionError(f"Login failed: {resp2}")


def cable_test_raw(host: str, port: int, username: str, password: str,
                   interface: str, timeout: float = 8.0) -> dict:
    """
    Run /interface/ethernet/cable-test via raw socket with a hard timeout.
    RouterOS v6 sends one !re sentence then goes silent - no !done ever arrives.
    Returns a dict of the result fields, or raises RuntimeError.
    Typical completion: ~3 s no-link, ~5 s with link.
    """
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(timeout)
    try:
        sock.connect((host, port))
        _ros_login_v6(sock, username, password)

        sock.sendall(_ros_encode_sentence([
            "/interface/ethernet/cable-test",
            f"=numbers={interface}",
        ]))

        result = {}
        deadline = time.time() + timeout
        while time.time() < deadline:
            remaining = deadline - time.time()
            sock.settimeout(max(0.5, remaining))
            try:
                sentence = _ros_read_sentence(sock)
            except (socket.timeout, TimeoutError, OSError):
                break

            if not sentence:
                break

            reply = sentence[0]
            if reply == "!re":
                for word in sentence[1:]:
                    if word.startswith("=") and "=" in word[1:]:
                        key, _, val = word[1:].partition("=")
                        result[key] = val
                if result:
                    break
            elif reply == "!done":
                break
            elif reply == "!trap":
                msg = " ".join(sentence[1:])
                raise RuntimeError(f"Router error: {msg}")

        return result
    finally:
        try:
            sock.close()
        except Exception:
            pass


# ---------------------------------------------------------------------------
# Data classes
# ---------------------------------------------------------------------------

@dataclass
class ProbeConfig:
    ip_address: str
    username: str
    password: str
    test_interface: str
    use_ssl: bool = False
    port: int = 8728

@dataclass
class LinkStatusData:
    status: Optional[str] = None
    rate: Optional[str] = None
    full_duplex: Optional[str] = None
    auto_negotiation: Optional[str] = None

@dataclass
class TdrEntry:
    description: Optional[str] = None
    status: Optional[str] = None
    distance: Optional[str] = None

@dataclass
class CableTestSummary:
    status: str = ""
    entries: list = field(default_factory=list)
    raw: dict = field(default_factory=dict)

@dataclass
class InterfaceCounters:
    rx_bytes:   Optional[str] = None
    rx_packets: Optional[str] = None
    rx_errors:  Optional[str] = None
    rx_drops:   Optional[str] = None
    tx_bytes:   Optional[str] = None
    tx_packets: Optional[str] = None
    tx_errors:  Optional[str] = None
    tx_drops:   Optional[str] = None
    timestamp:  Optional[str] = None

@dataclass
class NeighborData:
    identity:       Optional[str] = None
    interface_name: Optional[str] = None
    discovered_by:  Optional[str] = None
    address:        Optional[str] = None
    mac_address:    Optional[str] = None
    vlan_id:        Optional[str] = None
    poe_class:      Optional[str] = None
    port_id:        Optional[str] = None

@dataclass
class PingTargetOutcome:
    target:      str = ""
    packet_loss: Optional[str] = None
    avg_rtt:     Optional[str] = None
    min_rtt:     Optional[str] = None
    max_rtt:     Optional[str] = None
    sent:        Optional[str] = None
    received:    Optional[str] = None
    error:       Optional[str] = None

@dataclass
class SpeedTestData:
    status:         Optional[str] = None
    ping:           Optional[str] = None
    tcp_download:   Optional[str] = None
    tcp_upload:     Optional[str] = None
    udp_download:   Optional[str] = None
    udp_upload:     Optional[str] = None
    warning:        Optional[str] = None
    server_address: Optional[str] = None

@dataclass
class NetworkData:
    mode:    str = "UNKNOWN"
    address: Optional[str] = None
    gateway: Optional[str] = None
    dns:     Optional[str] = None

@dataclass
class TestReport:
    probe_host:        str
    interface:         str
    timestamp:         str
    overall_status:    str
    board_name:        Optional[str] = None
    router_os_version: Optional[str] = None
    link_status:       Optional[LinkStatusData] = None
    network:           Optional[NetworkData] = None
    counters_before:   Optional[InterfaceCounters] = None
    counters_after:    Optional[InterfaceCounters] = None
    tdr:               list = field(default_factory=list)
    tdr_raw:           dict = field(default_factory=dict)
    neighbors:         list = field(default_factory=list)
    ping_targets:      list = field(default_factory=list)
    speed_test:        Optional[SpeedTestData] = None
    errors:            list = field(default_factory=list)


# ---------------------------------------------------------------------------
# MikroTik API client
# ---------------------------------------------------------------------------

class MikroTikClient:
    def __init__(self, probe: ProbeConfig) -> None:
        self.probe = probe
        api_port = 8729 if probe.use_ssl else 8728
        try:
            self.api = connect(
                host=probe.ip_address,
                username=probe.username,
                password=probe.password,
                port=api_port,
                timeout=30
            )
        except Exception as e:
            raise ConnectionError(f"Cannot connect to RouterOS API on port {api_port}: {e}")

    def _exec(self, command: str, **kwargs: Any) -> list:
        try:
            return list(self.api(command, **kwargs))
        except (ConnectionClosed, FatalError, TrapError) as e:
            raise RuntimeError(f"API error executing '{command}': {e}")
        except Exception as e:
            raise RuntimeError(f"API error executing '{command}': {e}")

    def get_system_info(self) -> dict:
        data = self._exec("/system/resource/print")
        return data[0] if data else {}

    def monitor_ethernet(self, interface: str) -> LinkStatusData:
        results = self._exec(
            "/interface/ethernet/monitor",
            **{"numbers": interface, "once": ""}
        )
        if not results:
            raise ValueError("No data from ethernet monitor")
        latest = results[-1]
        return LinkStatusData(
            status=latest.get("status"),
            rate=latest.get("rate"),
            full_duplex=latest.get("full-duplex"),
            auto_negotiation=latest.get("auto-negotiation"),
        )

    def reset_counters(self, interface: str) -> None:
        try:
            self._exec("/interface/ethernet/reset-counters",
                       **{"numbers": interface})
        except Exception:
            try:
                self._exec("/interface/reset-counters",
                           **{"numbers": interface})
            except Exception as e:
                raise RuntimeError(f"Could not reset counters: {e}")

    def get_counters(self, interface: str) -> InterfaceCounters:
        data = self._exec("/interface/print")
        for item in data:
            if item.get("name") == interface:
                return InterfaceCounters(
                    rx_bytes=item.get("rx-byte"),
                    rx_packets=item.get("rx-packet"),
                    rx_errors=item.get("rx-error"),
                    rx_drops=item.get("rx-drop"),
                    tx_bytes=item.get("tx-byte"),
                    tx_packets=item.get("tx-packet"),
                    tx_errors=item.get("tx-error"),
                    tx_drops=item.get("tx-drop"),
                    timestamp=datetime.now().isoformat(timespec="seconds"),
                )
        raise ValueError(f"Interface '{interface}' not found")

    def cable_test(self, interface: str) -> CableTestSummary:
        raw = cable_test_raw(
            host=self.probe.ip_address,
            port=self.probe.port,
            username=self.probe.username,
            password=self.probe.password,
            interface=interface,
            timeout=8.0,
        )
        if not raw:
            raise ValueError("No cable test results returned")

        raw_status = raw.get("status", "unknown")
        entries = []

        found = False
        for i in range(1, 5):
            status = raw.get(f"pair{i}-status")
            dist   = raw.get(f"pair{i}-length") or raw.get(f"pair{i}-distance")
            if status is not None:
                found = True
                entries.append(TdrEntry(
                    description=f"pair{i}",
                    status=status,
                    distance=str(dist) if dist is not None else None,
                ))
        if not found:
            for name in ("pair-ab", "pair-cd", "pair-ef", "pair-gh"):
                status = raw.get(f"{name}-status")
                dist   = raw.get(f"{name}-length")
                if status is not None:
                    found = True
                    entries.append(TdrEntry(
                        description=name,
                        status=status,
                        distance=str(dist) if dist is not None else None,
                    ))
        if not entries:
            entries = [TdrEntry(description="overall", status=raw_status)]

        return CableTestSummary(status=raw_status, entries=entries, raw=raw)

    def get_neighbors(self, interface: str) -> list:
        data = self._exec("/ip/neighbor/print")
        neighbors = []
        for item in data:
            if (item.get("interface") != interface and
                    item.get("interface-name") != interface):
                continue
            neighbors.append(NeighborData(
                identity=item.get("identity") or item.get("system-name"),
                interface_name=item.get("interface") or item.get("interface-name"),
                discovered_by=item.get("discovered-by"),
                address=item.get("address"),
                mac_address=item.get("mac-address"),
                vlan_id=item.get("vlan-id"),
                poe_class=item.get("poe-class"),
                port_id=item.get("port-id"),
            ))
        return neighbors

    def ping(self, target: str, count: int = 4) -> PingTargetOutcome:
        try:
            results = self._exec("/tool/ping",
                                 **{"address": target, "count": str(count)})
            summary = PingTargetOutcome(target=target)
            for r in results:
                if r.get("sent") is not None:
                    summary.sent        = r.get("sent")
                    summary.received    = r.get("received")
                    summary.packet_loss = r.get("packet-loss")
                    summary.avg_rtt     = r.get("avg-rtt")
                    summary.min_rtt     = r.get("min-rtt")
                    summary.max_rtt     = r.get("max-rtt")
            return summary
        except RuntimeError as e:
            if "no such command" in str(e).lower() or "unknown command" in str(e).lower():
                return self._ping_local(target, count)
            raise

    def _ping_local(self, target: str, count: int) -> PingTargetOutcome:
        try:
            result = subprocess.run(
                ["ping", "-n", str(count), target],
                capture_output=True, text=True, timeout=30
            )
            out = result.stdout
            outcome = PingTargetOutcome(target=target)
            for line in out.splitlines():
                ll = line.lower()
                if "lost" in ll and "%" in ll:
                    try:
                        for p in line.split(","):
                            p = p.strip()
                            if "sent" in p.lower():
                                outcome.sent = p.split("=")[-1].strip()
                            elif "received" in p.lower():
                                outcome.received = p.split("=")[-1].strip()
                        s = line.find("(")
                        e = line.find("%")
                        if s != -1 and e != -1:
                            outcome.packet_loss = line[s+1:e].strip()
                    except Exception:
                        pass
                if "minimum" in ll:
                    try:
                        for p in line.split(","):
                            p = p.strip()
                            if "minimum" in p.lower():
                                outcome.min_rtt = p.split("=")[-1].strip().replace("ms","").strip()
                            elif "maximum" in p.lower():
                                outcome.max_rtt = p.split("=")[-1].strip().replace("ms","").strip()
                            elif "average" in p.lower():
                                outcome.avg_rtt = p.split("=")[-1].strip().replace("ms","").strip()
                    except Exception:
                        pass
            return outcome
        except Exception as e:
            return PingTargetOutcome(target=target, error=f"Local ping failed: {e}")

    def speed_test(self, server: str, username: str = "admin",
                   password: str = "", duration: int = 5) -> SpeedTestData:
        results = self._exec(
            "/tool/speed-test",
            **{
                "address":       server,
                "user":          username,
                "password":      password,
                "test-duration": str(duration) + "s",
            }
        )
        if not results:
            raise ValueError("Empty speed test response")
        r = results[-1]
        ping_val = (
            r.get("ping-min-avg-max") or
            f"{r.get('min-ping','')}/{r.get('avg-ping','')}/{r.get('max-ping','')}"
        )
        return SpeedTestData(
            status=r.get("status"),
            ping=ping_val,
            tcp_download=r.get("tcp-download") or r.get("total-download"),
            tcp_upload=r.get("tcp-upload") or r.get("total-upload"),
            udp_download=r.get("udp-download"),
            udp_upload=r.get("udp-upload"),
            warning=r.get(".about") or r.get("warning"),
            server_address=server,
        )

    def get_network_info(self, interface: str) -> NetworkData:
        try:
            for addr in self._exec("/ip/address/print"):
                if addr.get("interface") == interface:
                    return NetworkData(mode="STATIC", address=addr.get("address"))
        except Exception:
            pass
        try:
            for entry in self._exec("/ip/dhcp-client/print"):
                if entry.get("interface") == interface:
                    return NetworkData(
                        mode="DHCP",
                        address=entry.get("address"),
                        gateway=entry.get("gateway"),
                        dns=entry.get("primary-dns"),
                    )
        except Exception:
            pass
        return NetworkData(mode="UNKNOWN")


# ---------------------------------------------------------------------------
# Runner
# ---------------------------------------------------------------------------

class MikLinkRunner:
    def __init__(self, client: MikroTikClient, probe: ProbeConfig) -> None:
        self.client = client
        self.probe  = probe

    def _log(self, msg: str) -> None:
        print(f"  {msg}")

    def run(
        self,
        run_link:    bool = True,
        run_tdr:     bool = True,
        run_lldp:    bool = True,
        ping_targets: Optional[list] = None,
        ping_count:  int = 4,
        speed_server: Optional[str] = None,
        speed_user:  str = "admin",
        speed_pass:  str = "",
        speed_dur:   int = 5,
    ) -> TestReport:
        iface     = self.probe.test_interface
        timestamp = datetime.now().isoformat(timespec="seconds")
        report    = TestReport(
            probe_host=self.probe.ip_address,
            interface=iface,
            timestamp=timestamp,
            overall_status="PASS",
        )

        # ── Probe connectivity ──────────────────────────────────────────────
        _section("Probe connectivity")
        try:
            info    = self.client.get_system_info()
            board   = info.get("board-name", "unknown")
            version = info.get("version", "unknown")
            report.board_name        = board
            report.router_os_version = version
            self._log(f"Board: {board}  RouterOS: {version}")
        except Exception as exc:
            self._log(f"FAIL - {exc}")
            report.overall_status = "FAIL"
            report.errors.append(str(exc))
            return report

        # ── Reset counters (before everything) ─────────────────────────────
        _section("Traffic counters - reset")
        try:
            self.client.reset_counters(iface)
            time.sleep(0.5)
            before = self.client.get_counters(iface)
            report.counters_before = before
            self._log(f"Reset at {before.timestamp}")
            self._log(
                f"Rx: pkts={before.rx_packets or 0}  "
                f"bytes={before.rx_bytes or 0}  "
                f"err={before.rx_errors or 0}  "
                f"drop={before.rx_drops or 0}"
            )
            self._log(
                f"Tx: pkts={before.tx_packets or 0}  "
                f"bytes={before.tx_bytes or 0}  "
                f"err={before.tx_errors or 0}  "
                f"drop={before.tx_drops or 0}"
            )
        except Exception as exc:
            self._log(f"FAIL - {exc}")
            report.errors.append(f"counters_reset: {exc}")

        # ── Link status ─────────────────────────────────────────────────────
        _section("Link status")
        if run_link:
            try:
                link = self.client.monitor_ethernet(iface)
                report.link_status = link
                ok = _is_link_up(link.status)
                if not ok:
                    report.overall_status = "FAIL"
                self._log(
                    f"Status:        {link.status or '-'}\n"
                    f"  Rate:        {link.rate or '-'}\n"
                    f"  Full-duplex: {link.full_duplex or '-'}\n"
                    f"  Auto-neg:    {link.auto_negotiation or '-'}\n"
                    f"  Result:      {'PASS' if ok else 'FAIL'}"
                )
            except Exception as exc:
                self._log(f"FAIL - {exc}")
                report.overall_status = "FAIL"
                report.errors.append(f"link_status: {exc}")
        else:
            self._log("Skipped (--no-link)")

        # ── Network config ──────────────────────────────────────────────────
        _section("Network config")
        try:
            net = self.client.get_network_info(iface)
            report.network = net
            self._log(
                f"Mode: {net.mode}  Address: {net.address or '-'}  "
                f"GW: {net.gateway or '-'}  DNS: {net.dns or '-'}"
            )
        except Exception as exc:
            self._log(f"FAIL - {exc}")
            report.errors.append(f"network: {exc}")

        # ── Cable TDR test ──────────────────────────────────────────────────
        _section("Cable TDR test  (raw socket, ~5-8 s)")
        if run_tdr:
            try:
                tdr = self.client.cable_test(iface)
                report.tdr     = tdr.entries
                report.tdr_raw = tdr.raw
                label = _tdr_status(tdr)
                if label == "FAIL":
                    report.overall_status = "FAIL"
                self._log(f"Overall: {tdr.status}  [{label}]")
                for entry in tdr.entries:
                    self._log(
                        f"  {entry.description or 'pair'}: "
                        f"status={entry.status or '-'}  "
                        f"dist={entry.distance or '-'}"
                    )
                if tdr.status and "link" in tdr.status.lower():
                    self._log(
                        "Tip: unplug far end for per-pair fault distances."
                    )
            except Exception as exc:
                self._log(f"FAIL - {exc}")
                report.errors.append(f"tdr: {exc}")
        else:
            self._log("Skipped (--no-tdr)")

        # ── LLDP / CDP neighbors ────────────────────────────────────────────
        _section("LLDP / CDP neighbors")
        if run_lldp:
            try:
                neighbors = self.client.get_neighbors(iface)
                report.neighbors = neighbors
                if neighbors:
                    for n in neighbors:
                        self._log(
                            f"  {n.identity or '-'}  "
                            f"addr={n.address or '-'}  "
                            f"mac={n.mac_address or '-'}  "
                            f"via={n.discovered_by or '-'}  "
                            f"port={n.port_id or '-'}  "
                            f"VLAN={n.vlan_id or '-'}  "
                            f"PoE={n.poe_class or '-'}"
                        )
                else:
                    self._log("No neighbors found")
            except Exception as exc:
                self._log(f"INFO - {exc}")
                report.errors.append(f"lldp: {exc}")
        else:
            self._log("Skipped (--no-lldp)")

        # ── Ping ────────────────────────────────────────────────────────────
        _section("Ping")
        targets = [t.strip() for t in (ping_targets or []) if t.strip()]
        if targets:
            for target in targets:
                try:
                    outcome = self.client.ping(target, count=ping_count)
                    report.ping_targets.append(outcome)
                    if outcome.error:
                        self._log(f"  {target}: {outcome.error}")
                        report.errors.append(f"ping_{target}: {outcome.error}")
                    else:
                        loss = outcome.packet_loss or "?"
                        self._log(
                            f"  {target}: "
                            f"sent={outcome.sent or '-'}  "
                            f"recv={outcome.received or '-'}  "
                            f"loss={loss}%  "
                            f"min/avg/max="
                            f"{outcome.min_rtt or '-'}/"
                            f"{outcome.avg_rtt or '-'}/"
                            f"{outcome.max_rtt or '-'} ms"
                        )
                        try:
                            if float(str(loss).replace("%","")) > 50:
                                report.overall_status = "FAIL"
                        except ValueError:
                            pass
                except Exception as exc:
                    self._log(f"  {target}: FAIL - {exc}")
                    report.ping_targets.append(
                        PingTargetOutcome(target=target, error=str(exc))
                    )
                    report.errors.append(f"ping_{target}: {exc}")
        else:
            self._log("Skipped (no --ping targets)")

        # ── Speed test ──────────────────────────────────────────────────────
        _section("Speed test")
        if speed_server:
            self._log(
                "Ensure on target: "
                "/tool bandwidth-server set enabled=yes authenticate=no"
            )
            try:
                speed = self.client.speed_test(
                    server=speed_server,
                    username=speed_user,
                    password=speed_pass,
                    duration=speed_dur,
                )
                report.speed_test = speed
                self._log(
                    f"TCP down: {speed.tcp_download or '-'}  "
                    f"up: {speed.tcp_upload or '-'}  "
                    f"ping: {speed.ping or '-'}"
                )
                if speed.warning:
                    self._log(f"Warning: {speed.warning}")
            except Exception as exc:
                self._log(f"FAIL - {exc}")
                report.errors.append(f"speed_test: {exc}")
        else:
            self._log("Skipped (no --speed-server)")

        # ── Final counter snapshot ──────────────────────────────────────────
        _section("Traffic counters - final snapshot")
        try:
            after = self.client.get_counters(iface)
            report.counters_after = after
            self._log(f"Captured at {after.timestamp}")
            self._log(
                f"Rx: pkts={after.rx_packets or 0}  "
                f"bytes={after.rx_bytes or 0}  "
                f"err={after.rx_errors or 0}  "
                f"drop={after.rx_drops or 0}"
            )
            self._log(
                f"Tx: pkts={after.tx_packets or 0}  "
                f"bytes={after.tx_bytes or 0}  "
                f"err={after.tx_errors or 0}  "
                f"drop={after.tx_drops or 0}"
            )
            def _int(v):
                try: return int(v or 0)
                except: return 0
            if _int(after.rx_errors) > 0 or _int(after.tx_errors) > 0:
                self._log("WARNING: interface errors detected")
                report.overall_status = "FAIL"
            if _int(after.rx_drops) > 0 or _int(after.tx_drops) > 0:
                self._log("WARNING: interface drops detected")
        except Exception as exc:
            self._log(f"FAIL - {exc}")
            report.errors.append(f"counters_after: {exc}")

        return report


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _section(title: str) -> None:
    print(f"\n{'='*60}\n  {title}\n{'='*60}")

def _is_link_up(status: Optional[str]) -> bool:
    if not status:
        return False
    return status.strip().lower() not in ("down", "no-link", "unknown", "")

def _tdr_status(summary: CableTestSummary) -> str:
    bad = {"open", "short", "impedance-mismatch", "fail", "no-cable"}
    for entry in summary.entries:
        if entry.status and entry.status.lower() in bad:
            return "FAIL"
    return "PASS"

def _int(v) -> int:
    try: return int(v or 0)
    except: return 0


# ---------------------------------------------------------------------------
# PDF report
# ---------------------------------------------------------------------------

def _generate_pdf(report: TestReport, path: str) -> None:
    try:
        from fpdf import FPDF
    except ImportError:
        print("fpdf2 not installed. Run: pip install fpdf2")
        return

    pdf = FPDF()
    pdf.set_auto_page_break(auto=True, margin=15)
    pdf.add_page()

    # ── Header ──────────────────────────────────────────────────────────────
    pdf.set_font("Helvetica", "B", 18)
    pdf.cell(0, 12, "MikLink Cable & Network Diagnostics Report",
             new_x="LMARGIN", new_y="NEXT")
    pdf.set_font("Helvetica", "", 10)
    pdf.cell(0, 5, f"Generated:  {report.timestamp}",
             new_x="LMARGIN", new_y="NEXT")
    pdf.cell(0, 5, f"Probe:      {report.probe_host}  "
                   f"RouterOS {report.router_os_version or '-'}",
             new_x="LMARGIN", new_y="NEXT")
    pdf.cell(0, 5, f"Board:      {report.board_name or '-'}",
             new_x="LMARGIN", new_y="NEXT")
    pdf.cell(0, 5, f"Interface:  {report.interface}",
             new_x="LMARGIN", new_y="NEXT")
    pdf.ln(4)

    color = (34, 139, 34) if report.overall_status == "PASS" else (200, 0, 0)
    pdf.set_text_color(*color)
    pdf.set_font("Helvetica", "B", 16)
    pdf.cell(0, 10, f"Overall Result: {report.overall_status}",
             new_x="LMARGIN", new_y="NEXT")
    pdf.set_text_color(0, 0, 0)
    pdf.ln(3)

    # ── Section / row helpers ────────────────────────────────────────────────
    def section(title: str) -> None:
        pdf.set_font("Helvetica", "B", 11)
        pdf.set_fill_color(40, 40, 40)
        pdf.set_text_color(255, 255, 255)
        pdf.cell(0, 7, f"  {title}", fill=True,
                 new_x="LMARGIN", new_y="NEXT")
        pdf.set_text_color(0, 0, 0)
        pdf.set_font("Helvetica", "", 10)

    def row(label: str, value, warn: bool = False) -> None:
        pdf.set_font("Helvetica", "B", 9)
        pdf.cell(52, 5, str(label))
        pdf.set_font("Helvetica", "", 9)
        if warn:
            pdf.set_text_color(200, 60, 0)
        pdf.cell(0, 5, str(value) if value else "-",
                 new_x="LMARGIN", new_y="NEXT")
        if warn:
            pdf.set_text_color(0, 0, 0)

    def pass_fail_row(label: str, ok: bool) -> None:
        pdf.set_font("Helvetica", "B", 9)
        pdf.cell(52, 5, str(label))
        pdf.set_font("Helvetica", "B", 9)
        pdf.set_text_color(34, 139, 34) if ok else pdf.set_text_color(200, 0, 0)
        pdf.cell(0, 5, "PASS" if ok else "FAIL",
                 new_x="LMARGIN", new_y="NEXT")
        pdf.set_text_color(0, 0, 0)

    def traffic_bar(label: str, value, max_val: int) -> None:
        v = _int(value)
        bar_w = 90.0   # mm
        filled = min(bar_w, bar_w * v / max(max_val, 1))
        x_start = pdf.get_x()
        pdf.set_font("Helvetica", "", 9)
        pdf.cell(42, 5, str(label))
        pdf.cell(28, 5, f"{v:,}")
        x = pdf.get_x()
        y = pdf.get_y()
        # background
        pdf.set_fill_color(210, 210, 210)
        pdf.rect(x, y + 0.5, bar_w, 4, style="F")
        # filled portion
        if filled > 0:
            pdf.set_fill_color(60, 120, 200)
            pdf.rect(x, y + 0.5, filled, 4, style="F")
        pdf.ln(6)

    # ── Link Status ──────────────────────────────────────────────────────────
    section("Link Status")
    if report.link_status:
        ok = _is_link_up(report.link_status.status)
        pass_fail_row("Status", ok)
        row("Rate",           report.link_status.rate)
        row("Full-duplex",    report.link_status.full_duplex)
        row("Auto-negotiate", report.link_status.auto_negotiation)
    else:
        pdf.cell(0, 5, "Skipped", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)

    # ── Network Config ───────────────────────────────────────────────────────
    section("Network Configuration")
    if report.network:
        row("Mode",    report.network.mode)
        row("Address", report.network.address)
        row("Gateway", report.network.gateway)
        row("DNS",     report.network.dns)
    else:
        pdf.cell(0, 5, "Not available", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)

    # ── Traffic Counters ─────────────────────────────────────────────────────
    section("Traffic Counters  (Rx / Tx graphs)")

    all_pkt  = []
    all_byte = []
    for c in (report.counters_before, report.counters_after):
        if c:
            all_pkt  += [_int(c.rx_packets), _int(c.tx_packets)]
            all_byte += [_int(c.rx_bytes),   _int(c.tx_bytes)]
    scale_pkt  = max(max(all_pkt,  default=1000), 1)
    scale_byte = max(max(all_byte, default=1000), 1)

    for label, counters in (
        ("After reset", report.counters_before),
        ("Final snapshot", report.counters_after),
    ):
        if counters:
            pdf.set_font("Helvetica", "B", 10)
            pdf.cell(0, 6, f"{label}  ({counters.timestamp})",
                     new_x="LMARGIN", new_y="NEXT")
            traffic_bar("Rx packets",  counters.rx_packets,  scale_pkt)
            traffic_bar("Tx packets",  counters.tx_packets,  scale_pkt)
            traffic_bar("Rx bytes",    counters.rx_bytes,    scale_byte)
            traffic_bar("Tx bytes",    counters.tx_bytes,    scale_byte)
            row("Rx errors", counters.rx_errors,
                warn=_int(counters.rx_errors) > 0)
            row("Rx drops",  counters.rx_drops,
                warn=_int(counters.rx_drops) > 0)
            row("Tx errors", counters.tx_errors,
                warn=_int(counters.tx_errors) > 0)
            row("Tx drops",  counters.tx_drops,
                warn=_int(counters.tx_drops) > 0)
            pdf.ln(3)

    if not report.counters_before and not report.counters_after:
        pdf.cell(0, 5, "Not available", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(1)

    # ── Cable TDR ────────────────────────────────────────────────────────────
    section("Cable TDR - Per-pair Strand Analysis")
    if report.tdr:
        bad_statuses = {"open", "short", "impedance-mismatch", "fail", "no-cable"}
        for entry in report.tdr:
            bad = (entry.status or "").lower() in bad_statuses
            row(entry.description or "pair",
                f"status = {entry.status or '-'}     "
                f"distance = {entry.distance or '-'}",
                warn=bad)
        if report.tdr_raw:
            pdf.ln(1)
            pdf.set_font("Helvetica", "I", 7)
            raw_str = "  ".join(f"{k}={v}" for k, v in report.tdr_raw.items())
            pdf.multi_cell(0, 4, f"Raw: {raw_str}",
                           new_x="LMARGIN", new_y="NEXT")
    else:
        pdf.cell(0, 5, "Skipped or no results", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)

    # ── Neighbors ────────────────────────────────────────────────────────────
    section("LLDP / CDP Neighbors")
    if report.neighbors:
        for n in report.neighbors:
            row("Identity",  n.identity)
            row("Address",   n.address)
            row("MAC",       n.mac_address)
            row("Via",       n.discovered_by)
            row("Port",      n.port_id)
            row("VLAN",      n.vlan_id)
            row("PoE class", n.poe_class)
            pdf.ln(2)
    else:
        pdf.cell(0, 5, "No neighbors found / skipped",
                 new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)

    # ── Ping ─────────────────────────────────────────────────────────────────
    section("Ping Results")
    if report.ping_targets:
        for outcome in report.ping_targets:
            if outcome.error:
                row(outcome.target, f"ERROR: {outcome.error}", warn=True)
            else:
                try:
                    bad_ping = float(
                        str(outcome.packet_loss or "0").replace("%","")
                    ) > 10
                except Exception:
                    bad_ping = False
                row(outcome.target,
                    f"sent={outcome.sent or '-'}  "
                    f"recv={outcome.received or '-'}  "
                    f"loss={outcome.packet_loss or '?'}%  "
                    f"min/avg/max="
                    f"{outcome.min_rtt or '-'}/"
                    f"{outcome.avg_rtt or '-'}/"
                    f"{outcome.max_rtt or '-'} ms",
                    warn=bad_ping)
    else:
        pdf.cell(0, 5, "Skipped (no targets)",
                 new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)

    # ── Speed Test ───────────────────────────────────────────────────────────
    section("Speed Test")
    if report.speed_test:
        s = report.speed_test
        row("Server",           s.server_address)
        row("TCP Download",     s.tcp_download)
        row("TCP Upload",       s.tcp_upload)
        row("UDP Download",     s.udp_download)
        row("UDP Upload",       s.udp_upload)
        row("Ping min/avg/max", s.ping)
        if s.warning:
            row("Warning", s.warning, warn=True)
    else:
        pdf.cell(0, 5, "Skipped (no server configured)",
                 new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)

    # ── Errors ───────────────────────────────────────────────────────────────
    if report.errors:
        section("Errors and Warnings")
        pdf.set_font("Helvetica", "", 8)
        for err in report.errors:
            pdf.set_text_color(180, 0, 0)
            pdf.multi_cell(0, 4, str(err), new_x="LMARGIN", new_y="NEXT")
            pdf.set_text_color(0, 0, 0)

    pdf.output(path)
    print(f"\nPDF report saved to: {path}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def _parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="MikLink v6 - RouterOS 6.47.x diagnostics"
    )
    p.add_argument("--host",         required=True)
    p.add_argument("--user",         default="admin")
    p.add_argument("--password",     default="")
    p.add_argument("--port",         type=int, default=8728)
    p.add_argument("--https",        action="store_true")
    p.add_argument("--interface",    default="ether1")
    p.add_argument("--no-link",      action="store_true")
    p.add_argument("--no-tdr",       action="store_true")
    p.add_argument("--no-lldp",      action="store_true")
    p.add_argument("--ping",         default="",
                   help="Comma-separated ping targets")
    p.add_argument("--ping-count",   type=int, default=4)
    p.add_argument("--speed-server", default="")
    p.add_argument("--speed-user",   default="admin")
    p.add_argument("--speed-pass",   default="")
    p.add_argument("--speed-dur",    type=int, default=5)
    p.add_argument("--pdf",          default="")
    p.add_argument("--json",         default="")
    return p.parse_args()


def main() -> None:
    args  = _parse_args()
    probe = ProbeConfig(
        ip_address=args.host,
        username=args.user,
        password=args.password,
        test_interface=args.interface,
        use_ssl=args.https,
        port=args.port if args.port else (8729 if args.https else 8728),
    )

    print(f"\nMikLink v6 - RouterOS 6.47.x Diagnostics")
    print(f"Probe:     {probe.ip_address}:{probe.port}")
    print(f"Interface: {probe.test_interface}")
    print(f"Started:   {datetime.now().isoformat(timespec='seconds')}")

    try:
        client = MikroTikClient(probe)
    except ConnectionError as e:
        print(f"\nFAIL - {e}")
        print("Checklist:")
        print("  /ip service enable api")
        print("  /ip service set api address=0.0.0.0/0")
        print("  Test-NetConnection 192.168.88.1 -Port 8728")
        sys.exit(1)

    runner       = MikLinkRunner(client, probe)
    ping_targets = [t.strip() for t in args.ping.split(",") if t.strip()]

    t0     = time.time()
    report = runner.run(
        run_link=not args.no_link,
        run_tdr=not args.no_tdr,
        run_lldp=not args.no_lldp,
        ping_targets=ping_targets,
        ping_count=args.ping_count,
        speed_server=args.speed_server or None,
        speed_user=args.speed_user,
        speed_pass=args.speed_pass,
        speed_dur=args.speed_dur,
    )
    elapsed = time.time() - t0

    print(f"\n{'='*60}")
    print(f"  Result: {report.overall_status}  (elapsed {elapsed:.1f}s)")
    print(f"{'='*60}\n")

    if args.json:
        def clean(obj: Any) -> Any:
            if hasattr(obj, "__dataclass_fields__"):
                return {k: clean(v) for k, v in asdict(obj).items()
                        if v is not None}
            if isinstance(obj, list):
                return [clean(i) for i in obj]
            return obj
        with open(args.json, "w", encoding="utf-8") as f:
            json.dump(clean(report), f, indent=2, ensure_ascii=False)
        print(f"JSON report saved to: {args.json}")

    if args.pdf:
        _generate_pdf(report, args.pdf)

    sys.exit(0 if report.overall_status == "PASS" else 1)


if __name__ == "__main__":
    main()