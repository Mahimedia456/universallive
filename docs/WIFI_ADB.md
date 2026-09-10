# Wi-Fi ADB setup

`PHONE_IP:PAIR_PORT` and `PHONE_IP:ADB_PORT` are examples/placeholders, not commands to paste literally.

## Phone

Settings → Developer options → Wireless debugging → Pair device with pairing code.

The pairing dialog shows something similar to:

```text
IP address & port: 192.168.1.45:37123
Wi-Fi pairing code: 654321
```

Run:

```powershell
adb pair 192.168.1.45:37123
```

Enter `654321` only if that is the current code shown by your phone.

After pairing, close the pairing dialog and look at the main Wireless debugging screen. It can show a DIFFERENT port, for example:

```text
IP address & Port
192.168.1.45:42987
```

Run:

```powershell
adb connect 192.168.1.45:42987
adb devices
```

You should see:

```text
192.168.1.45:42987    device
```

If pairing fails, generate a fresh pairing code and use the new pairing port/code. Keep PC and phone on the same local network.
