# Pi Transfer Instructions

## 1. Flash SD Card

Download Raspberry Pi Imager → Select **Raspberry Pi OS 64-bit Lite** → flash to microSD.

Before ejecting, create:
```
# boot/wpa_supplicant.conf
ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
update_config=1
country=US

network={
    ssid="YOUR_WIFI"
    psk="YOUR_PASSWORD"
}
```

```bash
# boot/ssh (empty file)
touch ssh
```

## 2. First Boot

Insert SD, power on. Find IP:
```bash
ping raspberrypi.local
ssh pi@raspberrypi.local  # default password: raspberry
```

Update:
```bash
sudo apt update && sudo apt full-upgrade -y
```

## 3. Install Java 26

```bash
# Option A: Azul Zulu (recommended, has aarch64 builds)
curl -s https://repos.azul.com/azul-repo.key | sudo gpg --dearmor -o /usr/share/keyrings/azul.gpg
echo "deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main" | sudo tee /etc/apt/sources.list.d/zulu.list
sudo apt update
sudo apt install zulu26-jdk

java -version  # should print openjdk version "26"
```

## 4. Transfer Project

From laptop:
```bash
cd wildtype
rsync -avz --exclude='.gradle' --exclude='build' --exclude='captures' \
    --exclude='*.db' --exclude='notes' \
    . pi@raspberrypi.local:~/wildtype/
```

On Pi:
```bash
cd ~/wildtype/app
# Download ONNX model (too big for git, was gitignored)
mkdir -p src/main/resources/models
curl -L -o src/main/resources/models/mobilenet-ssd.onnx \
    "https://media.githubusercontent.com/media/onnx/models/main/validated/vision/object_detection_segmentation/ssd-mobilenetv1/model/ssd_mobilenet_v1_12.onnx"

# Build
./gradlew build
```

## 5. Run

```bash
cd ~/wildtype/app
./gradlew bootRun
```

Open `http://<pi-ip>:8081` from any device on the network.

## 6. Auto-start on Boot (Optional)

Create `/etc/systemd/system/wildtype.service`:
```ini
[Unit]
Description=Wildtype Wildlife Camera
After=network.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/wildtype/app
ExecStart=/home/pi/wildtype/gradlew bootRun
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable wildtype
sudo systemctl start wildtype
```

## 7. Enclosure

Print `enclosure/tray.scad` (30 min, 15g PLA). Mount Pi + webcam with M2.5 screws.
