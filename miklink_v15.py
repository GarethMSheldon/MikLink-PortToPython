#!/usr/bin/env python3
"""
miklink_v15.py - Improved TDR via raw socket with proper RouterOS API login.
Fixes: challenge-response, robust socket I/O, correct neighbor command, OS ping, stop handling.
"""
from __future__ import annotations
import hashlib
import json
import os
import socket
import struct
import subprocess
import sys
import time
from dataclasses import dataclass, field, asdict
from datetime import datetime
from typing import Any, Optional, Callable, List
import threading
import customtkinter as ctk
from tkinter import filedialog, messagebox

try:
    from librouteros import connect
    from librouteros.exceptions import ConnectionClosed, FatalError, TrapError
except ImportError as e:
    print(f"ERROR: Failed to import 'librouteros'.\n {e}")
    print("Fix: python -m pip install librouteros")
    sys.exit(1)

# ---------------------------------------------------------------------------
# Data classes (unchanged)
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
    entries: List[TdrEntry] = field(default_factory=list)
    raw: dict = field(default_factory=dict)

@dataclass
class InterfaceCounters:
    rx_bytes: Optional[str] = None
    rx_packets: Optional[str] = None
    rx_errors: Optional[str] = None
    rx_drops: Optional[str] = None
    tx_bytes: Optional[str] = None
    tx_packets: Optional[str] = None
    tx_errors: Optional[str] = None
    tx_drops: Optional[str] = None
    timestamp: Optional[str] = None

@dataclass
class NeighborData:
    identity: Optional[str] = None
    interface_name: Optional[str] = None
    discovered_by: Optional[str] = None
    address: Optional[str] = None
    mac_address: Optional[str] = None
    vlan_id: Optional[str] = None
    poe_class: Optional[str] = None
    port_id: Optional[str] = None

@dataclass
class PingTargetOutcome:
    target: str = ""
    packet_loss: Optional[str] = None
    avg_rtt: Optional[str] = None
    min_rtt: Optional[str] = None
    max_rtt: Optional[str] = None
    sent: Optional[str] = None
    received: Optional[str] = None
    error: Optional[str] = None

@dataclass
class SpeedTestData:
    status: Optional[str] = None
    ping: Optional[str] = None
    tcp_download: Optional[str] = None
    tcp_upload: Optional[str] = None
    udp_download: Optional[str] = None
    udp_upload: Optional[str] = None
    warning: Optional[str] = None
    server_address: Optional[str] = None

@dataclass
class NetworkData:
    mode: str = "UNKNOWN"
    address: Optional[str] = None
    gateway: Optional[str] = None
    dns: Optional[str] = None

@dataclass
class TestReport:
    probe_host: str
    interface: str
    timestamp: str
    overall_status: str
    board_name: Optional[str] = None
    router_os_version: Optional[str] = None
    link_status: Optional[LinkStatusData] = None
    network: Optional[NetworkData] = None
    counters_before: Optional[InterfaceCounters] = None
    counters_after: Optional[InterfaceCounters] = None
    tdr: List[TdrEntry] = field(default_factory=list)
    tdr_raw: dict = field(default_factory=dict)
    neighbors: List[NeighborData] = field(default_factory=list)
    ping_targets: List[PingTargetOutcome] = field(default_factory=list)
    speed_test: Optional[SpeedTestData] = None
    errors: List[str] = field(default_factory=list)

# ---------------------------------------------------------------------------
# MikroTik API client (full with fixed cable_test)
# ---------------------------------------------------------------------------
class MikroTikClient:
    def __init__(self, probe: ProbeConfig) -> None:
        self.probe = probe
        api_port = 8729 if probe.use_ssl else 8728
        try:
            self.api = connect(host=probe.ip_address, username=probe.username,
                               password=probe.password, port=api_port, timeout=30)
        except Exception as e:
            raise ConnectionError(f"Cannot connect to RouterOS API on port {api_port}: {e}")

    def _exec(self, command: str, **kwargs: Any) -> List[dict]:
        try:
            return list(self.api(command, **kwargs))
        except (ConnectionClosed, FatalError, TrapError) as e:
            raise RuntimeError(f"API error executing '{command}': {e}")

    def get_system_info(self) -> dict:
        data = self._exec("/system/resource/print")
        return data[0] if data else {}

    def monitor_ethernet(self, interface: str) -> LinkStatusData:
        results = self._exec("/interface/ethernet/monitor", numbers=interface, once=True)
        if not results:
            raise ValueError("No data from ethernet monitor")
        latest = results[-1]
        return LinkStatusData(status=latest.get("status"), rate=latest.get("rate"),
                              full_duplex=latest.get("full-duplex"),
                              auto_negotiation=latest.get("auto-negotiation"))

    def reset_counters(self, interface: str) -> None:
        try:
            self._exec("/interface/ethernet/reset-counters", numbers=interface)
        except Exception:
            # Fallback for older RouterOS
            self._exec("/interface/reset-counters", numbers=interface)

    def get_counters(self, interface: str) -> InterfaceCounters:
        data = self._exec("/interface/print")
        for item in data:
            if item.get("name") == interface:
                return InterfaceCounters(
                    rx_bytes=item.get("rx-byte"), rx_packets=item.get("rx-packet"),
                    rx_errors=item.get("rx-error"), rx_drops=item.get("rx-drop"),
                    tx_bytes=item.get("tx-byte"), tx_packets=item.get("tx-packet"),
                    tx_errors=item.get("tx-error"), tx_drops=item.get("tx-drop"),
                    timestamp=datetime.now().isoformat(timespec="seconds")
                )
        raise ValueError(f"Interface '{interface}' not found")

    # ========== FIXED CABLE TEST with proper RouterOS API login ==========
    @staticmethod
    def _ros_encode_len(n: int) -> bytes:
        """Encode length as RouterOS API word length."""
        if n < 0x80:
            return bytes([n])
        if n < 0x4000:
            return struct.pack("!H", n | 0x8000)
        if n < 0x200000:
            return struct.pack("!I", n | 0xC00000)[1:]
        return struct.pack("!I", n | 0xE0000000)

    @staticmethod
    def _ros_encode_word(word: str) -> bytes:
        b = word.encode("utf-8")
        return MikroTikClient._ros_encode_len(len(b)) + b

    @staticmethod
    def _ros_send(sock: socket.socket, *words: str) -> None:
        sock.sendall(b"".join(MikroTikClient._ros_encode_word(w) for w in words) + b"\x00")

    @staticmethod
    def _ros_read_len(sock: socket.socket) -> int:
        """Read a RouterOS API length field (may be multiple bytes)."""
        b = sock.recv(1)
        if not b:
            raise ConnectionError("Socket closed")
        first = b[0]
        if first < 0x80:
            return first
        if first < 0xC0:
            # two bytes: (first & 0x3F) << 8 + next byte
            nxt = sock.recv(1)
            if not nxt:
                raise ConnectionError("Incomplete length")
            return ((first & 0x3F) << 8) | nxt[0]
        if first < 0xE0:
            # three bytes: (first & 0x1F) << 16 + next two bytes
            nxt = sock.recv(2)
            if len(nxt) < 2:
                raise ConnectionError("Incomplete length")
            return ((first & 0x1F) << 16) | (nxt[0] << 8) | nxt[1]
        # four bytes
        nxt = sock.recv(3)
        if len(nxt) < 3:
            raise ConnectionError("Incomplete length")
        return ((first & 0x0F) << 24) | (nxt[0] << 16) | (nxt[1] << 8) | nxt[2]

    @staticmethod
    def _ros_read_sentence(sock: socket.socket) -> List[str]:
        """Read one RouterOS sentence (list of words)."""
        words = []
        while True:
            n = MikroTikClient._ros_read_len(sock)
            if n == 0:
                break
            buf = b""
            while len(buf) < n:
                chunk = sock.recv(n - len(buf))
                if not chunk:
                    raise ConnectionError("Socket closed mid-word")
                buf += chunk
            words.append(buf.decode("utf-8", errors="replace"))
        return words

    def cable_test(self, interface: str) -> CableTestSummary:
        """Perform ethernet cable test using raw socket with proper challenge-response login."""
        api_port = 8729 if self.probe.use_ssl else 8728
        sock = socket.create_connection((self.probe.ip_address, api_port), timeout=15)
        sock.settimeout(15)
        raw_data = {}
        try:
            # Step 1: send /login to get challenge
            self._ros_send(sock, "/login")
            login_response = self._ros_read_sentence(sock)
            if not login_response or login_response[0] != "!done":
                raise RuntimeError(f"Unexpected login challenge response: {login_response}")
            # Parse challenge: =ret=... word
            challenge = None
            for word in login_response[1:]:
                if word.startswith("=ret="):
                    challenge = word[5:]
                    break
            if challenge is None:
                raise RuntimeError("No challenge received from RouterOS")

            # Compute response: md5(challenge + password)
            md5_hash = hashlib.md5()
            md5_hash.update(challenge.encode("utf-8"))
            md5_hash.update(self.probe.password.encode("utf-8"))
            response = md5_hash.hexdigest().lower()

            # Step 2: send login with credentials
            self._ros_send(sock, "/login", f"=name={self.probe.username}", f"=response=00{response}")
            auth_reply = self._ros_read_sentence(sock)
            if not auth_reply or auth_reply[0] != "!done":
                raise RuntimeError(f"Login failed: {auth_reply}")

            # Step 3: send cable test command
            self._ros_send(sock, "/interface/ethernet/cable-test", f"=numbers={interface}")

            # Step 4: read sentences until we get a !re containing the result
            deadline = time.time() + 30
            while time.time() < deadline:
                sentence = self._ros_read_sentence(sock)
                if not sentence:
                    break
                tag = sentence[0]
                if tag == "!re":
                    # parse attributes (words starting with '=')
                    parsed = {}
                    for word in sentence[1:]:
                        if word.startswith("=") and "=" in word[1:]:
                            k, _, v = word[1:].partition("=")
                            parsed[k] = v
                    if parsed:
                        raw_data = parsed
                        # Got data, but RouterOS may still send !done later; we break early
                        break
                elif tag == "!done":
                    break
                elif tag == "!trap":
                    raise RuntimeError(f"Router error: {' '.join(sentence[1:])}")
            if not raw_data:
                raise ValueError("No cable test results returned")
        finally:
            sock.close()

        # Parse results (same as original)
        raw_status = raw_data.get("status", "unknown")
        entries = []
        for i in range(1, 5):
            status = raw_data.get(f"pair{i}-status")
            dist = raw_data.get(f"pair{i}-length") or raw_data.get(f"pair{i}-distance")
            if status is not None:
                entries.append(TdrEntry(description=f"pair{i}", status=status,
                                        distance=str(dist) if dist is not None else None))
        if not entries:
            for name in ("pair-ab", "pair-cd", "pair-ef", "pair-gh"):
                status = raw_data.get(f"{name}-status")
                dist = raw_data.get(f"{name}-length")
                if status is not None:
                    entries.append(TdrEntry(description=name, status=status,
                                            distance=str(dist) if dist is not None else None))
        if not entries:
            entries = [TdrEntry(description="overall", status=raw_status)]
        return CableTestSummary(status=raw_status, entries=entries, raw=raw_data)

    # ========== FIXED NEIGHBOR DISCOVERY (LLDP/MNDP) ==========
    def get_neighbors(self, interface: str) -> List[NeighborData]:
        """Retrieve LLDP/MNDP neighbors for a given interface."""
        # Try modern /interface/neighbor first (RouterOS v7+ includes LLDP)
        try:
            data = self._exec("/interface/neighbor/print")
        except Exception:
            # Fallback to legacy /ip neighbor
            data = self._exec("/ip/neighbor/print")
        neighbors = []
        for item in data:
            iface = item.get("interface") or item.get("interface-name")
            if iface != interface:
                continue
            neighbors.append(NeighborData(
                identity=item.get("identity") or item.get("system-name"),
                interface_name=iface,
                discovered_by=item.get("discovered-by"),
                address=item.get("address"),
                mac_address=item.get("mac-address"),
                vlan_id=item.get("vlan-id"),
                poe_class=item.get("poe-class"),
                port_id=item.get("port-id")
            ))
        return neighbors

    def ping(self, target: str, count: int = 4) -> PingTargetOutcome:
        try:
            results = self._exec("/tool/ping", address=target, count=str(count))
            summary = PingTargetOutcome(target=target)
            for r in results:
                if r.get("sent") is not None:
                    summary.sent = r.get("sent")
                    summary.received = r.get("received")
                    summary.packet_loss = r.get("packet-loss")
                    summary.avg_rtt = r.get("avg-rtt")
                    summary.min_rtt = r.get("min-rtt")
                    summary.max_rtt = r.get("max-rtt")
            return summary
        except RuntimeError as e:
            if "no such command" in str(e).lower():
                return self._ping_local(target, count)
            raise

    def _ping_local(self, target: str, count: int) -> PingTargetOutcome:
        """Fallback to OS ping with platform detection."""
        try:
            if os.name == "nt":
                cmd = ["ping", "-n", str(count), target]
            else:
                cmd = ["ping", "-c", str(count), target]
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
            out = result.stdout
            outcome = PingTargetOutcome(target=target)
            # Parse output (simplified, regex would be better but keep existing logic)
            for line in out.splitlines():
                ll = line.lower()
                if "lost" in ll and "%" in ll:
                    try:
                        parts = line.split(",")
                        for p in parts:
                            p = p.strip()
                            if "sent" in p:
                                outcome.sent = p.split("=")[-1].strip()
                            elif "received" in p:
                                outcome.received = p.split("=")[-1].strip()
                        # parse loss percentage
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
                            if "minimum" in p:
                                outcome.min_rtt = p.split("=")[-1].replace("ms", "").strip()
                            elif "maximum" in p:
                                outcome.max_rtt = p.split("=")[-1].replace("ms", "").strip()
                            elif "average" in p:
                                outcome.avg_rtt = p.split("=")[-1].replace("ms", "").strip()
                    except Exception:
                        pass
            return outcome
        except Exception as e:
            return PingTargetOutcome(target=target, error=f"Local ping failed: {e}")

    def speed_test(self, server: str, username: str = "admin", password: str = "", duration: int = 5) -> SpeedTestData:
        results = self._exec("/tool/speed-test", address=server, user=username, password=password,
                             **{"test-duration": f"{duration}s"})
        if not results:
            raise ValueError("Empty speed test response")
        r = results[-1]
        ping_val = r.get("ping-min-avg-max") or f"{r.get('min-ping','')}/{r.get('avg-ping','')}/{r.get('max-ping','')}"
        return SpeedTestData(
            status=r.get("status"),
            ping=ping_val,
            tcp_download=r.get("tcp-download") or r.get("total-download"),
            tcp_upload=r.get("tcp-upload") or r.get("total-upload"),
            udp_download=r.get("udp-download"),
            udp_upload=r.get("udp-upload"),
            warning=r.get(".about") or r.get("warning"),
            server_address=server
        )

    def get_network_info(self, interface: str) -> NetworkData:
        try:
            addrs = self._exec("/ip/address/print")
            for addr in addrs:
                if addr.get("interface") == interface:
                    return NetworkData(mode="STATIC", address=addr.get("address"))
        except Exception:
            pass
        try:
            clients = self._exec("/ip/dhcp-client/print")
            for entry in clients:
                if entry.get("interface") == interface:
                    return NetworkData(mode="DHCP", address=entry.get("address"),
                                       gateway=entry.get("gateway"), dns=entry.get("primary-dns"))
        except Exception:
            pass
        return NetworkData(mode="UNKNOWN")

# ---------------------------------------------------------------------------
# Runner with improved stop handling
# ---------------------------------------------------------------------------
class MikLinkRunner:
    def __init__(self, client: MikroTikClient, probe: ProbeConfig,
                 log_callback: Optional[Callable[[str], None]] = None,
                 stop_event: Optional[threading.Event] = None) -> None:
        self.client = client
        self.probe = probe
        self._log = log_callback or (lambda x: print(f" {x}"))
        self.stop_event = stop_event

    def _check_stop(self) -> bool:
        """Return True if should stop. Does not raise exception."""
        return self.stop_event is not None and self.stop_event.is_set()

    def run(self, run_link: bool = True, run_tdr: bool = True, run_lldp: bool = True,
            ping_targets: Optional[List[str]] = None, ping_count: int = 4,
            speed_server: Optional[str] = None, speed_user: str = "admin",
            speed_pass: str = "", speed_dur: int = 5) -> TestReport:
        iface = self.probe.test_interface
        timestamp = datetime.now().isoformat(timespec="seconds")
        report = TestReport(probe_host=self.probe.ip_address, interface=iface,
                            timestamp=timestamp, overall_status="PASS")
        if self._check_stop():
            report.overall_status = "STOPPED"
            return report

        self._log("\n" + "=" * 60 + "\n Probe connectivity\n" + "=" * 60)
        try:
            info = self.client.get_system_info()
            report.board_name = info.get("board-name", "unknown")
            report.router_os_version = info.get("version", "unknown")
            self._log(f"Board: {report.board_name} RouterOS: {report.router_os_version}")
        except Exception as exc:
            self._log(f"FAIL - {exc}")
            report.overall_status = "FAIL"
            report.errors.append(str(exc))
            return report

        # Reset counters
        if not self._check_stop():
            self._log("\n" + "=" * 60 + "\n Traffic counters - reset\n" + "=" * 60)
            try:
                self.client.reset_counters(iface)
                time.sleep(0.5)
                before = self.client.get_counters(iface)
                report.counters_before = before
                self._log(f"Reset at {before.timestamp}")
                self._log(f"Rx: pkts={before.rx_packets or 0} bytes={before.rx_bytes or 0} "
                          f"err={before.rx_errors or 0} drop={before.rx_drops or 0}")
                self._log(f"Tx: pkts={before.tx_packets or 0} bytes={before.tx_bytes or 0} "
                          f"err={before.tx_errors or 0} drop={before.tx_drops or 0}")
            except Exception as exc:
                self._log(f"FAIL - {exc}")
                report.errors.append(f"counters_reset: {exc}")

        # Link status
        if not self._check_stop() and run_link:
            self._log("\n" + "=" * 60 + "\n Link status\n" + "=" * 60)
            try:
                link = self.client.monitor_ethernet(iface)
                report.link_status = link
                ok = _is_link_up(link.status)
                if not ok:
                    report.overall_status = "FAIL"
                self._log(f"Status: {link.status or '-'}\n Rate: {link.rate or '-'}\n "
                          f"Full-duplex: {link.full_duplex or '-'}\n Auto-neg: {link.auto_negotiation or '-'}\n "
                          f"Result: {'PASS' if ok else 'FAIL'}")
            except Exception as exc:
                self._log(f"FAIL - {exc}")
                report.overall_status = "FAIL"
                report.errors.append(f"link_status: {exc}")
        elif not run_link:
            self._log("\nLink status skipped")

        # Network config
        if not self._check_stop():
            self._log("\n" + "=" * 60 + "\n Network config\n" + "=" * 60)
            try:
                net = self.client.get_network_info(iface)
                report.network = net
                self._log(f"Mode: {net.mode} Address: {net.address or '-'} "
                          f"GW: {net.gateway or '-'} DNS: {net.dns or '-'}")
            except Exception as exc:
                self._log(f"FAIL - {exc}")
                report.errors.append(f"network: {exc}")

        # Cable TDR
        if not self._check_stop() and run_tdr:
            self._log("\n" + "=" * 60 + "\n Cable TDR test (~5-30 s)\n" + "=" * 60)
            try:
                tdr = self.client.cable_test(iface)
                report.tdr = tdr.entries
                report.tdr_raw = tdr.raw
                label = _tdr_status(tdr)
                if label == "FAIL":
                    report.overall_status = "FAIL"
                self._log(f"Overall: {tdr.status} [{label}]")
                for entry in tdr.entries:
                    self._log(f" {entry.description or 'pair'}: status={entry.status or '-'} "
                              f"dist={entry.distance or '-'}")
            except Exception as exc:
                self._log(f"FAIL - {exc}")
                report.errors.append(f"tdr: {exc}")
        elif not run_tdr:
            self._log("\nCable TDR skipped")

        # Neighbors
        if not self._check_stop() and run_lldp:
            self._log("\n" + "=" * 60 + "\n LLDP / CDP neighbors\n" + "=" * 60)
            try:
                neighbors = self.client.get_neighbors(iface)
                report.neighbors = neighbors
                if neighbors:
                    for n in neighbors:
                        self._log(f" {n.identity or '-'} addr={n.address or '-'} "
                                  f"mac={n.mac_address or '-'} via={n.discovered_by or '-'} "
                                  f"port={n.port_id or '-'} VLAN={n.vlan_id or '-'} PoE={n.poe_class or '-'}")
                else:
                    self._log("No neighbors found")
            except Exception as exc:
                self._log(f"INFO - {exc}")
                report.errors.append(f"lldp: {exc}")
        elif not run_lldp:
            self._log("\nNeighbors skipped")

        # Ping
        if not self._check_stop() and ping_targets:
            self._log("\n" + "=" * 60 + "\n Ping\n" + "=" * 60)
            for target in ping_targets:
                if self._check_stop():
                    break
                try:
                    outcome = self.client.ping(target, count=ping_count)
                    report.ping_targets.append(outcome)
                    if outcome.error:
                        self._log(f" {target}: {outcome.error}")
                        report.errors.append(f"ping_{target}: {outcome.error}")
                    else:
                        loss = outcome.packet_loss or "?"
                        self._log(f" {target}: sent={outcome.sent or '-'} recv={outcome.received or '-'} "
                                  f"loss={loss}% min/avg/max={outcome.min_rtt or '-'}/{outcome.avg_rtt or '-'}/{outcome.max_rtt or '-'} ms")
                        try:
                            if float(str(loss).replace("%", "")) > 50:
                                report.overall_status = "FAIL"
                        except Exception:
                            pass
                except Exception as exc:
                    self._log(f" {target}: FAIL - {exc}")
                    report.ping_targets.append(PingTargetOutcome(target=target, error=str(exc)))
                    report.errors.append(f"ping_{target}: {exc}")
        elif ping_targets:
            self._log("\nPing skipped")

        # Speed test
        if not self._check_stop() and speed_server:
            self._log("\n" + "=" * 60 + "\n Speed test\n" + "=" * 60)
            self._log("Ensure on target: /tool bandwidth-server set enabled=yes authenticate=no")
            try:
                speed = self.client.speed_test(server=speed_server, username=speed_user,
                                               password=speed_pass, duration=speed_dur)
                report.speed_test = speed
                self._log(f"TCP down: {speed.tcp_download or '-'} up: {speed.tcp_upload or '-'} "
                          f"ping: {speed.ping or '-'}")
                if speed.warning:
                    self._log(f"Warning: {speed.warning}")
            except Exception as exc:
                self._log(f"FAIL - {exc}")
                report.errors.append(f"speed_test: {exc}")
        elif speed_server:
            self._log("\nSpeed test skipped")

        # Final counters
        if not self._check_stop():
            self._log("\n" + "=" * 60 + "\n Traffic counters - final snapshot\n" + "=" * 60)
            try:
                after = self.client.get_counters(iface)
                report.counters_after = after
                self._log(f"Captured at {after.timestamp}")
                self._log(f"Rx: pkts={after.rx_packets or 0} bytes={after.rx_bytes or 0} "
                          f"err={after.rx_errors or 0} drop={after.rx_drops or 0}")
                self._log(f"Tx: pkts={after.tx_packets or 0} bytes={after.tx_bytes or 0} "
                          f"err={after.tx_errors or 0} drop={after.tx_drops or 0}")
                def _int(v):
                    try:
                        return int(v or 0)
                    except Exception:
                        return 0
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

# ---------------------------------------------------------------------------
# PDF report (unchanged, but keep for completeness)
# ---------------------------------------------------------------------------
def _generate_pdf(report: TestReport, path: str) -> None:
    try:
        from fpdf import FPDF
    except ImportError:
        print("fpdf2 not installed. Run: python -m pip install fpdf2")
        return
    pdf = FPDF()
    pdf.set_auto_page_break(auto=True, margin=15)
    pdf.add_page()
    pdf.set_font("Helvetica", "B", 18)
    pdf.cell(0, 12, "MikLink Cable & Network Diagnostics Report", new_x="LMARGIN", new_y="NEXT")
    pdf.set_font("Helvetica", "", 10)
    pdf.cell(0, 5, f"Generated: {report.timestamp}", new_x="LMARGIN", new_y="NEXT")
    pdf.cell(0, 5, f"Probe: {report.probe_host} RouterOS {report.router_os_version or '-'}", new_x="LMARGIN", new_y="NEXT")
    pdf.cell(0, 5, f"Board: {report.board_name or '-'}", new_x="LMARGIN", new_y="NEXT")
    pdf.cell(0, 5, f"Interface: {report.interface}", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(4)
    color = (34, 139, 34) if report.overall_status == "PASS" else (200, 0, 0)
    pdf.set_text_color(*color)
    pdf.set_font("Helvetica", "B", 16)
    pdf.cell(0, 10, f"Overall Result: {report.overall_status}", new_x="LMARGIN", new_y="NEXT")
    pdf.set_text_color(0, 0, 0)
    pdf.ln(3)
    def section(title: str):
        pdf.set_font("Helvetica", "B", 11)
        pdf.set_fill_color(40, 40, 40)
        pdf.set_text_color(255, 255, 255)
        pdf.cell(0, 7, f" {title}", fill=True, new_x="LMARGIN", new_y="NEXT")
        pdf.set_text_color(0, 0, 0)
        pdf.set_font("Helvetica", "", 10)
    def row(label: str, value, warn: bool = False):
        pdf.set_font("Helvetica", "B", 9)
        pdf.cell(52, 5, str(label))
        pdf.set_font("Helvetica", "", 9)
        if warn:
            pdf.set_text_color(200, 60, 0)
        pdf.cell(0, 5, str(value) if value else "-", new_x="LMARGIN", new_y="NEXT")
        if warn:
            pdf.set_text_color(0, 0, 0)
    def pass_fail_row(label: str, ok: bool):
        pdf.set_font("Helvetica", "B", 9)
        pdf.cell(52, 5, str(label))
        pdf.set_font("Helvetica", "B", 9)
        pdf.set_text_color(34, 139, 34) if ok else pdf.set_text_color(200, 0, 0)
        pdf.cell(0, 5, "PASS" if ok else "FAIL", new_x="LMARGIN", new_y="NEXT")
        pdf.set_text_color(0, 0, 0)
    section("Link Status")
    if report.link_status:
        ok = _is_link_up(report.link_status.status)
        pass_fail_row("Status", ok)
        row("Rate", report.link_status.rate)
        row("Full-duplex", report.link_status.full_duplex)
        row("Auto-negotiate", report.link_status.auto_negotiation)
    else:
        pdf.cell(0, 5, "Skipped", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)
    section("Network Configuration")
    if report.network:
        row("Mode", report.network.mode)
        row("Address", report.network.address)
        row("Gateway", report.network.gateway)
        row("DNS", report.network.dns)
    else:
        pdf.cell(0, 5, "Not available", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)
    section("Traffic Counters")
    for label, counters in (("After reset", report.counters_before), ("Final snapshot", report.counters_after)):
        if counters:
            pdf.set_font("Helvetica", "B", 10)
            pdf.cell(0, 6, f"{label} ({counters.timestamp})", new_x="LMARGIN", new_y="NEXT")
            row("Rx packets", counters.rx_packets)
            row("Tx packets", counters.tx_packets)
            row("Rx bytes", counters.rx_bytes)
            row("Tx bytes", counters.tx_bytes)
            row("Rx errors", counters.rx_errors)
            row("Rx drops", counters.rx_drops)
            row("Tx errors", counters.tx_errors)
            row("Tx drops", counters.tx_drops)
            pdf.ln(3)
    if not report.counters_before and not report.counters_after:
        pdf.cell(0, 5, "Not available", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)
    section("Cable TDR - Per-pair Strand Analysis")
    if report.tdr:
        bad_statuses = {"open", "short", "impedance-mismatch", "fail", "no-cable"}
        for entry in report.tdr:
            bad = (entry.status or "").lower() in bad_statuses
            row(entry.description or "pair", f"status = {entry.status or '-'} distance = {entry.distance or '-'}", warn=bad)
        if report.tdr_raw:
            pdf.ln(1)
            pdf.set_font("Helvetica", "I", 7)
            raw_str = " ".join(f"{k}={v}" for k, v in report.tdr_raw.items())
            pdf.multi_cell(0, 4, f"Raw: {raw_str}", new_x="LMARGIN", new_y="NEXT")
    else:
        pdf.cell(0, 5, "Skipped or no results", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)
    section("LLDP / CDP Neighbors")
    if report.neighbors:
        for n in report.neighbors:
            row("Identity", n.identity)
            row("Address", n.address)
            row("MAC", n.mac_address)
            row("Via", n.discovered_by)
            row("Port", n.port_id)
            row("VLAN", n.vlan_id)
            row("PoE class", n.poe_class)
            pdf.ln(2)
    else:
        pdf.cell(0, 5, "No neighbors found / skipped", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)
    section("Ping Results")
    if report.ping_targets:
        for outcome in report.ping_targets:
            if outcome.error:
                row(outcome.target, f"ERROR: {outcome.error}", warn=True)
            else:
                try:
                    bad_ping = float(str(outcome.packet_loss or "0").replace("%","")) > 10
                except Exception:
                    bad_ping = False
                row(outcome.target, f"sent={outcome.sent or '-'} recv={outcome.received or '-'} loss={outcome.packet_loss or '?'}% min/avg/max={outcome.min_rtt or '-'}/{outcome.avg_rtt or '-'}/{outcome.max_rtt or '-'} ms", warn=bad_ping)
    else:
        pdf.cell(0, 5, "Skipped (no targets)", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)
    section("Speed Test")
    if report.speed_test:
        s = report.speed_test
        row("Server", s.server_address)
        row("TCP Download", s.tcp_download)
        row("TCP Upload", s.tcp_upload)
        row("UDP Download", s.udp_download)
        row("UDP Upload", s.udp_upload)
        row("Ping min/avg/max", s.ping)
        if s.warning:
            row("Warning", s.warning, warn=True)
    else:
        pdf.cell(0, 5, "Skipped (no server configured)", new_x="LMARGIN", new_y="NEXT")
    pdf.ln(3)
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
# GUI (unchanged except improved stop handling)
# ---------------------------------------------------------------------------
class MikLinkGUI:
    def __init__(self):
        ctk.set_appearance_mode("dark")
        ctk.set_default_color_theme("blue")
        self.root = ctk.CTk()
        self.root.title("MikLink v11 — 2026 RouterOS Diagnostics (Fixed API)")
        self.root.geometry("1280x820")
        self.root.protocol("WM_DELETE_WINDOW", self.on_close)

        left = ctk.CTkFrame(self.root, width=300)
        left.pack(side="left", fill="y", padx=10, pady=10)
        ctk.CTkLabel(left, text="ROUTER CONNECTION", font=ctk.CTkFont(size=16, weight="bold")).pack(pady=10)
        self.host_entry = ctk.CTkEntry(left, placeholder_text="192.168.88.1")
        self.host_entry.pack(pady=5, padx=20, fill="x")
        self.user_entry = ctk.CTkEntry(left, placeholder_text="admin")
        self.user_entry.pack(pady=5, padx=20, fill="x")
        self.pass_entry = ctk.CTkEntry(left, placeholder_text="password", show="*")
        self.pass_entry.pack(pady=5, padx=20, fill="x")
        self.port_entry = ctk.CTkEntry(left, placeholder_text="8728")
        self.port_entry.pack(pady=5, padx=20, fill="x")
        self.ssl_var = ctk.BooleanVar(value=False)
        ctk.CTkCheckBox(left, text="Use SSL (8729)", variable=self.ssl_var).pack(pady=8)
        self.iface_entry = ctk.CTkEntry(left, placeholder_text="ether1")
        self.iface_entry.pack(pady=5, padx=20, fill="x")

        right = ctk.CTkFrame(self.root)
        right.pack(side="right", fill="both", expand=True, padx=10, pady=10)
        ctk.CTkLabel(right, text="TEST OPTIONS", font=ctk.CTkFont(size=16, weight="bold")).pack(pady=10)
        self.link_var = ctk.BooleanVar(value=True)
        self.tdr_var = ctk.BooleanVar(value=True)
        self.lldp_var = ctk.BooleanVar(value=True)
        ctk.CTkCheckBox(right, text="Link Status", variable=self.link_var).pack(anchor="w", padx=20)
        ctk.CTkCheckBox(right, text="Cable TDR (raw)", variable=self.tdr_var).pack(anchor="w", padx=20)
        ctk.CTkCheckBox(right, text="LLDP / Neighbors", variable=self.lldp_var).pack(anchor="w", padx=20)
        ctk.CTkLabel(right, text="Ping targets (comma separated)").pack(anchor="w", padx=20, pady=(15,0))
        self.ping_entry = ctk.CTkEntry(right, placeholder_text="8.8.8.8,1.1.1.1")
        self.ping_entry.pack(pady=5, padx=20, fill="x")
        ctk.CTkLabel(right, text="Speed-test server (optional)").pack(anchor="w", padx=20, pady=(15,0))
        self.speed_entry = ctk.CTkEntry(right, placeholder_text="speedtest.example.com")
        self.speed_entry.pack(pady=5, padx=20, fill="x")

        btn_frame = ctk.CTkFrame(right)
        btn_frame.pack(pady=20)
        self.run_btn = ctk.CTkButton(btn_frame, text="RUN DIAGNOSTICS", width=200, height=40,
                                     font=ctk.CTkFont(size=14, weight="bold"), command=self.start_run)
        self.run_btn.grid(row=0, column=0, padx=5)
        self.stop_btn = ctk.CTkButton(btn_frame, text="STOP", width=120, height=40, fg_color="red",
                                      font=ctk.CTkFont(size=14, weight="bold"), command=self.stop_run, state="disabled")
        self.stop_btn.grid(row=0, column=1, padx=5)
        self.pdf_btn = ctk.CTkButton(btn_frame, text="Save PDF", width=120, state="disabled", command=self.save_pdf)
        self.pdf_btn.grid(row=0, column=2, padx=5)
        self.json_btn = ctk.CTkButton(btn_frame, text="Save JSON", width=120, state="disabled", command=self.save_json)
        self.json_btn.grid(row=0, column=3, padx=5)

        ctk.CTkLabel(right, text="LIVE LOG", font=ctk.CTkFont(size=14, weight="bold")).pack(anchor="w", padx=20, pady=(20,5))
        self.log_text = ctk.CTkTextbox(right, height=320, font=ctk.CTkFont(family="Consolas", size=11))
        self.log_text.pack(fill="both", expand=True, padx=20, pady=5)

        self.report = None
        self.stop_event = None
        self.root.mainloop()

    def on_close(self):
        if self.stop_event:
            self.stop_event.set()
        self.root.destroy()

    def log(self, msg: str):
        timestamp = datetime.now().strftime("%H:%M:%S")
        self.log_text.insert("end", f"[{timestamp}] {msg}\n")
        self.log_text.see("end")
        self.root.update_idletasks()

    def start_run(self):
        self.run_btn.configure(state="disabled")
        self.stop_btn.configure(state="normal")
        self.log_text.delete("1.0", "end")
        self.log("Starting MikLink v11...")
        self.stop_event = threading.Event()
        threading.Thread(target=self.run_diagnostics, daemon=True).start()

    def stop_run(self):
        if self.stop_event:
            self.stop_event.set()
        self.log("🛑 STOP requested - aborting current operation...")

    def run_diagnostics(self):
        try:
            probe = ProbeConfig(
                ip_address=self.host_entry.get().strip() or "192.168.88.1",
                username=self.user_entry.get().strip() or "admin",
                password=self.pass_entry.get().strip(),
                test_interface=self.iface_entry.get().strip() or "ether1",
                use_ssl=self.ssl_var.get(),
                port=int(self.port_entry.get().strip() or (8729 if self.ssl_var.get() else 8728))
            )
            self.log(f"Connecting to {probe.ip_address}:{probe.port}...")
            client = MikroTikClient(probe)
            if self.stop_event.is_set():
                self.log("Aborted during connection")
                return
            runner = MikLinkRunner(client, probe, log_callback=self.log, stop_event=self.stop_event)
            ping_targets = [t.strip() for t in self.ping_entry.get().split(",") if t.strip()]
            speed_server = self.speed_entry.get().strip() or None
            self.log("Running full diagnostics...")
            report = runner.run(
                run_link=self.link_var.get(),
                run_tdr=self.tdr_var.get(),
                run_lldp=self.lldp_var.get(),
                ping_targets=ping_targets,
                speed_server=speed_server
            )
            self.report = report
            self.log(f"FINISHED — Overall: {report.overall_status}")
            self.pdf_btn.configure(state="normal")
            self.json_btn.configure(state="normal")
        except Exception as e:
            if self.stop_event and self.stop_event.is_set():
                self.log("Operation stopped by user")
            else:
                self.log(f"CRITICAL FAILURE: {e}")
        finally:
            self.run_btn.configure(state="normal")
            self.stop_btn.configure(state="disabled")
            self.stop_event = None

    def save_pdf(self):
        if not self.report:
            return
        path = filedialog.asksaveasfilename(defaultextension=".pdf", filetypes=[("PDF", "*.pdf")])
        if path:
            _generate_pdf(self.report, path)
            messagebox.showinfo("Success", f"PDF saved to {path}")

    def save_json(self):
        if not self.report:
            return
        path = filedialog.asksaveasfilename(defaultextension=".json", filetypes=[("JSON", "*.json")])
        if path:
            def clean(obj):
                if hasattr(obj, "__dataclass_fields__"):
                    return {k: clean(v) for k, v in asdict(obj).items() if v is not None}
                if isinstance(obj, list):
                    return [clean(i) for i in obj]
                return obj
            with open(path, "w", encoding="utf-8") as f:
                json.dump(clean(self.report), f, indent=2, ensure_ascii=False)
            messagebox.showinfo("Success", f"JSON saved to {path}")

if __name__ == "__main__":
    MikLinkGUI()
