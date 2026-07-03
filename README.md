# WiFi to Mobile Data Converter

WiFi to Mobile Data Converter is a Windows desktop application that simulates the constraints and characteristics of a mobile data connection (like 4G LTE) over a standard Wi-Fi connection. It is useful for developers and testers who need to test their applications in a strict Carrier-Grade NAT (CGNAT) and bandwidth-limited environment without requiring physical mobile hardware.

## Architecture: How We Convert WiFi to Data

The application operates by modifying Windows system network policies and firewall rules to emulate mobile data constraints. The conversion process focuses on several key areas:

1. **Bandwidth Throttling (Simulating 4G LTE Speed)**
   - **Mechanism:** The app uses Windows Quality of Service (QoS) policies via PowerShell (`New-NetQosPolicy`).
   - **Details:** It throttles system-wide application traffic to a cap of **15 Mbps**, which closely matches average 4G LTE speeds in real-world scenarios.

2. **Carrier-Grade NAT (CGNAT) & Strict NAT Emulation**
   - **Mechanism:** The app utilizes the Windows Advanced Firewall (`netsh advfirewall`) to enforce strict inbound rules.
   - **Details:** Typical mobile networks reside behind a CGNAT, preventing direct inbound connections. To simulate this, the application blocks common server hosting ports (e.g., HTTP/80, HTTPS/443, SSH/22, RDP/3389) and P2P UDP ports. This ensures your local machine behaves as if it has no public-facing IPv4 address for inbound traffic, closely matching the strict NAT type of cellular networks.

3. **DNS Redirection**
   - **Mechanism:** Adjusts the active network adapter's DNS settings.
   - **Details:** Replaces local ISP or router DHCP-assigned DNS servers with public DNS (Cloudflare `1.1.1.1` as primary, Google `8.8.8.8` as secondary) to bypass local network DNS resolutions and simulate an external carrier network lookup environment.

4. **IPv6 Disabling**
   - **Mechanism:** Disables the IPv6 component on the Wi-Fi adapter via PowerShell (`Disable-NetAdapterBinding`).
   - **Details:** Many mobile networks force IPv4-only communication or use NAT64. By disabling IPv6, we guarantee a pure IPv4 environment that forces applications to fall back to IPv4 routing.

5. **Privilege Escalation**
   - To apply these system-wide network changes, the application requires and verifies Administrator privileges upon execution.

## Technical Details & Tech Stack

The project is built entirely in **Java** and interacts heavily with native Windows command-line utilities.

### Core Tech Stack

* **Language:** Java 17
* **Build Tool:** Maven
* **User Interface:** Java Swing customized with **FlatLaf** (Flat Dark Look and Feel) for a modern, dark-themed native appearance.
* **Packaging:** The app is bundled into a standalone Windows executable (`.exe`) using the **Launch4j** Maven plugin, with an embedded application manifest to automatically request UAC (User Account Control) Administrator elevation.

### System Interactions

The application acts as a high-level GUI wrapper over complex underlying Windows commands:
* **PowerShell:** Used for querying active Wi-Fi adapters (`Get-NetAdapter`), identifying IP addresses (`Get-NetIPAddress`), configuring QoS throttling (`New-NetQosPolicy`, `Remove-NetQosPolicy`), and toggling IPv6 bindings (`Enable/Disable-NetAdapterBinding`).
* **Netsh:** The Network Shell utility is used for rapid firewall manipulation (`netsh advfirewall firewall add rule ...`) and DNS configuration (`netsh interface ip set dns ...`).

## Setup and Usage

1. **Prerequisites:** 
   - Windows 10 or 11.
   - Java 17+ (If running from source/jar).

2. **Running the Application:**
   - Execute the compiled `WiFiDataConverter.exe`.
   - The application will prompt for UAC Administrator privileges (required to apply QoS and Firewall rules).
   - Once opened, use the intuitive toggle switches to enable/disable features like Throttling, CGNAT Port Blocks, DNS Overrides, and IPv6.

3. **Building from Source:**
   ```bash
   mvn clean package
   ```
   This will compile the code, shade the dependencies, and output a `WiFiDataConverter.exe` in the `target/` directory using Launch4j.

## License

Copyright (c) 2025 WiFi Converter.
