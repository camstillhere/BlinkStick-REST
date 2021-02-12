package com.cameron.stiller.blinkStick;

import com.codeminders.hidapi.ClassPathLibraryLoader;
import com.codeminders.hidapi.HIDDevice;
import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Controller {

    /**
     * BlinkStick vendor ID
     */
    public final static int VENDOR_ID = 0x20a0;

    /**
     * BlinkStick product ID
     */
    public final static int PRODUCT_ID = 0x41e5;


    private HashMap<String, BlinkStick> devices = new HashMap<>();

    public Controller() {
        ClassPathLibraryLoader.loadNativeHIDLibrary();
        findAll();
    }

    /**
     * Update the list of devices connected to this computer
     */
    private void findAll() {
        HIDManager hidManager;
        try {
            hidManager = HIDManager.getInstance();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        HIDDeviceInfo[] hidDeviceInfos;
        try {
            hidDeviceInfos = hidManager.listDevices();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (HIDDeviceInfo hIDDeviceInfo : hidDeviceInfos) {
            if (hIDDeviceInfo.getVendor_id() == VENDOR_ID
                    && hIDDeviceInfo.getProduct_id() == PRODUCT_ID) {
                try {
                    HIDDevice myHidDevice = hIDDeviceInfo.open();
                    BlinkStick device = new BlinkStick(myHidDevice);
                    devices.put(device.getSerial(), device);
                } catch (NullPointerException | IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    public void setFrameRate(String serialNumber, long millisecondWaitTime) {
        devices.get(serialNumber).setFrameRate(millisecondWaitTime);
    }

    public Set<String> getConnectedDevices() {
        return devices.keySet();
    }

    public void setColor(String serialNumber, int index, int r, int g, int b) {
        devices.get(serialNumber).setIndexedColor(index, r, g, b);
    }

    public void setBrightness(String serialNumber, double brightness) {
        devices.get(serialNumber).setBrightness(brightness);
    }

    public void setColor(String serialNumber, int r, int g, int b) {
        devices.get(serialNumber).setColor(r, g, b);
    }

    private int getRandomColor() {
        return (int) (Math.random() * (256));
    }

    public void setRandomColor(String deviceId) {
        setColor(deviceId, getRandomColor(), getRandomColor(), getRandomColor());
    }

    public int determineMaxLeds(String serialNumber) {
        return devices.get(serialNumber).determineMaxLeds();
    }

    public void setRandomColor(String deviceId, int index) {
        devices.get(deviceId).setIndexedColor(index, getRandomColor(), getRandomColor(), getRandomColor());

    }

    public void setIndexedColors(String deviceId, ArrayList<HashMap<Character, Integer>> ledPackage) {
        devices.get(deviceId).setIndexedColors(ledPackage);
    }

    public void setRandomColors(String deviceId) {
        ArrayList<HashMap<Character, Integer>> ledPackage = new ArrayList<>();
        for (int i = 0; i < determineMaxLeds(deviceId); i += 1) {
            HashMap<Character, Integer> ledBundle = new HashMap<>();
            ledBundle.put('r', getRandomColor());
            ledBundle.put('g', getRandomColor());
            ledBundle.put('b', getRandomColor());
            ledPackage.add(ledBundle);
        }
        devices.get(deviceId).setIndexedColors(ledPackage);
    }

    public ArrayList<HashMap<Character,Integer>> getColors(String deviceId) {
        return devices.get(deviceId).getColors();
    }
}
