# Troubleshooting

## Connection refused / timeout on port 8728

1. Confirm API is enabled: in WinBox go to IP > Services, check that `api` is enabled.
2. Confirm your PC's IP is in the allowed address range:
   ```
   /ip service print
   ```
   If the `address` column for `api` shows a specific subnet, make sure your PC is in it.
3. Check firewall:
   ```
   /ip firewall filter print where chain=input
   ```
   Look for any `drop` rules above your accept rules that might block port 8728.
4. Test from PowerShell:
   ```powershell
   Test-NetConnection 192.168.88.1 -Port 8728
   ```

## Login failed: invalid user name or password

The TDR raw-socket login uses MD5 challenge/response which is specific to
RouterOS v6. If you see this error:

- Double-check the `--password` value. Note that an empty password must be
  passed as `--password ""` not omitted entirely.
- Ensure the username exists: `/user print`
- Ensure the user has at minimum `read` and `api` policy:
  ```
  /user set admin policy=local,telnet,ssh,ftp,reboot,read,write,policy,test,winbox,password,web,sniff,sensitive,api,!romon,!tikapp
  ```

## TDR returns no results

- The interface name must match exactly. Use `--interface ether1` not `eth1`.
- Confirm the port is a physical copper ethernet port — SFP and wireless
  interfaces do not support TDR.
- Try from the WinBox terminal first:
  ```
  /interface ethernet cable-test ether1
  ```
  If this also returns nothing, the port's PHY does not support TDR.

## Ping shows no RTT values (min/avg/max = -)

RouterOS API ping in v6 returns individual echo replies and a summary sentence.
MikLink reads the summary (`sent`, `received`, `packet-loss`, `avg-rtt` fields).
Some firmware builds include RTT in the summary, others do not. The packet
counts and loss percentage are always present. The fallback local ping (when
`/tool/ping` is unavailable via API) parses Windows `ping -n` output and
always provides min/avg/max.

## Speed test: authentication failed

The bandwidth-server on the target router requires authentication to be
disabled, or you must pass matching credentials:

```
/tool bandwidth-server set enabled=yes authenticate=no
```

Or run with credentials:

```
python miklink.py ... --speed-server 192.168.88.2 \
  --speed-user admin --speed-pass yourpass
```

## PDF not generated

Ensure `fpdf2` is installed:

```
pip install fpdf2
```

Note: `fpdf` (version 1) and `fpdf2` are different packages. MikLink requires
`fpdf2` which provides the `from fpdf import FPDF` import used internally.

## librouteros import errors

```
pip install --force-reinstall --no-cache-dir librouteros
```

If using Python 3.14, librouteros 4.0.0 is compatible. Older versions may not
have `TrapError` in `librouteros.exceptions` — always use 4.0.0 or later.
