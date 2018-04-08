#!/bin/bash
echo Welcome to the SK-IDE Ubuntu/debian installer, press Control + C anytime to cancel
echo Processing in 3 Seconds
sleep 3
echo Clearing potential old data
sudo rm -rf /usr/share/skide
echo Creating app folder
sudo mkdir /usr/share/skide
sudo mkdir /usr/share/skide/bin
cd /usr/share/skide/bin
echo Downloading SK-IDE
sudo wget https://liz3.net/sk/depot/linux.tar.gz
echo Unpacking SK-IDE
sudo tar xf linux.tar.gz
sudo chmod +x jre1.8.0_162/bin/java
echo Creating Desktop Entry
sudo cp SKIDE.desktop /usr/share/applications/
sudo cp skide-uninstall.desktop /usr/share/applications/
sudo chmod +x uninstall.sh
echo Done!
