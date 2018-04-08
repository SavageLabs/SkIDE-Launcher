#!/bin/bash
echo Removing SK-IDE, your projects will not be deleted though!
sudo rm -rf /usr/share/skide/
echo Deleting Desktop entries
sudo rm /usr/share/applications/SKIDE.desktop
sudo rm /usr/share/applications/skide-uninstall.desktop
