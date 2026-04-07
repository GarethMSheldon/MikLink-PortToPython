# Router Setup Guide

## Minimum configuration

Connect to your MikroTik via WinBox, WebFig, or SSH and run:

```
/ip service enable api
/ip service set api address=0.0.0.0/0
```

The second command allows API connections from any IP. To restrict to only
your laptop's IP (more secure):

```
/ip service set api address=192.168.88.10/32
```

## Verify API is accessible

From Windows PowerShell on the machine running MikLink:

```powershell
Test-NetConnection 192.168.88.1 -Port 8728
```

`TcpTestSucceeded: True` means you are ready.

## Enable bandwidth-server for speed tests

```
/tool bandwidth-server set enabled=yes authenticate=no
```

With `authenticate=no` you can run speed tests without passing credentials.
If you prefer authentication:

```
/tool bandwidth-server set enabled=yes authenticate=yes
```

Then pass `--speed-user` and `--speed-pass` when running MikLink.

## Using API-SSL (port 8729)

```
/ip service enable api-ssl
/ip service set api-ssl address=0.0.0.0/0
```

Then run MikLink with `--https` (automatically uses port 8729).

## Firewall considerations

If your router has input firewall rules, ensure port 8728 is allowed from
the management interface. Example rule to add at the top of the input chain:

```
/ip firewall filter add chain=input protocol=tcp dst-port=8728 \
    src-address=192.168.88.0/24 action=accept comment="Allow API from LAN" \
    place-before=0
```

## TDR test notes

Cable TDR is supported on physical copper ports with a TDR-capable PHY.
Most MikroTik hAP, RB, and CRS hardware supports it.

- **With a device connected:** the test returns `link-ok` but no fault distances,
  because the cable is terminated and the signal cannot reflect.
- **To measure fault distances:** unplug the cable at the far end, then run
  MikLink with `--no-link` to prevent the tool from stopping early on no-link.

The raw values returned by RouterOS look like:

```
status=link-ok
pair1-status=link-ok
pair1-length=
pair2-status=link-ok
pair2-length=
pair3-status=link-ok
pair3-length=
pair4-status=link-ok
pair4-length=
```

On a cable with a fault at 14 m on pair 2:

```
pair2-status=open
pair2-length=14
```
