package com.cameron.stiller.blinkStick;

import com.codeminders.hidapi.HIDDevice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class BlinkStick {
    private final HIDDevice device;
    private Integer ledCount = -1;
    private String deviceType;
    private double brightness = 0.05;
    private long millisecondWaitTime = 10; //should wait at least 25 millisecond before sending next command.
    private ArrayList<HashMap<Character, Integer>> lastState = new ArrayList<>();
    private static final HashMap<Character, Integer> off = new HashMap<>() {{
        put('r', 0);
        put('g', 0);
        put('b', 0);
    }};

    /*
      BlinkStick serial number has software version major and minor numbers.
      For example BS000000-1.4 has 1 as major and 4 as minor software version number.
      Here is the full LED number logic for all BlinkStick devices.
      1.x - BlinkStick 5 with single LED pixel
      2.x - BlinkStick Pro (number of LEDs depend on mode)
      Mode 0 (RGB) - 1 LED
      Mode 1 (RGB inverse) - 1 LED
      Mode 2 (Multi-LED) - up to 64 LEDs on each R, G and B channel
      3.x - BlinkStick Strip or BlinkStick Square
      Mode 2 (Multi-LED) - 8 LEDs
      Mode 3 (Multi-LED Mirror) - 8 LEDs all light up with the same color
       */
    public BlinkStick(HIDDevice device) {
        if (device == null) {
            throw new NullPointerException();
        }
        this.device = device;
        String[] softwareComponents = getSerial().split("-")[1].split("\\.");
        String major = softwareComponents[0];
        String minor = softwareComponents[1];
        switch (major) {
            case "1":
                deviceType = "BlinkStick 5";
                ledCount = 5;
                break;
            case "2":
                deviceType = "BlinkStick Pro";
                switch (minor) {
                    case "0":
                    case "1":
                        ledCount = 1;
                        break;
                    case "2":
                        ledCount = 64;
                }
                break;
            case "3":
                switch (minor) {
                    case "0":
                    case "1":
                        deviceType = "BlinkStick Flex";
                        ledCount = 32;
                        break;
                    case "2":
                    case "3":
                        deviceType = "BlinkStick Square";
                        ledCount = 8;
                }
        }
        for (int i = 0; i < ledCount; i += 1) {
            lastState.add(off);
        }
        setColor(0, 0, 0);
        System.out.println(deviceType + "(" + ledCount + ")");
    }

    public void setFrameRate(long millisecondWaitTime) {
        this.millisecondWaitTime = millisecondWaitTime;
    }

    public void setBrightness(double brightness) {
        this.brightness = brightness;
        setIndexedColors(lastState);
    }

    private byte adjustBrightness(double rgb) {
        return (byte) (brightness * rgb);
    }

    public int determineMaxLeds() {
        return ledCount;
    }

    public void setIndexedColor(int index, int r, int g, int b) {
        int channel = 0;

        try {
            synchronized (device) {
                HashMap<Character, Integer> state = new HashMap<>() {{
                    put('r', r);
                    put('g', g);
                    put('b', b);
                }};
                lastState.set(index, state);
                device.sendFeatureReport(new byte[]{5, (byte) channel, (byte) index, adjustBrightness(r), adjustBrightness(g), adjustBrightness(b)});
                TimeUnit.MILLISECONDS.sleep(millisecondWaitTime);
            }
        } catch (IOException e) {
            millisecondWaitTime = millisecondWaitTime + 1;
            setIndexedColor(index, r, g, b);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getSerial() {
        try {
            return device.getSerialNumberString();
        } catch (Exception e) {
            return "";
        }
    }

    public void setColor(int r, int g, int b) {

        try {
            synchronized (device) {
                byte[] rgbPackage = new byte[(ledCount * 3) + 2];
                rgbPackage[0] = (byte) 8;
                rgbPackage[1] = (byte) 0;
                for (int i = 0; i < ledCount; i += 1) {
                    rgbPackage[(i*3)+2] = adjustBrightness(g);
                    rgbPackage[(i*3) + 3] = adjustBrightness(r);
                    rgbPackage[(i*3) + 4] = adjustBrightness(b);
                    HashMap<Character, Integer> state = new HashMap<>() {{
                        put('r', r);
                        put('g', g);
                        put('b', b);
                    }};
                    lastState.set(i, state);
                }
                device.sendFeatureReport(rgbPackage);
                TimeUnit.MILLISECONDS.sleep(millisecondWaitTime);
            }
        } catch (IOException e) {
            millisecondWaitTime = millisecondWaitTime + 1;
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Blinkstick has a method for retrieving colors from the device HOWEVER... it chews up bandwidth.
    // If we cache it and send it back, we can do faster animations!
    // drawback, does not work on a reboot (the lights will still be on but the laststate will be all off
    // so... to combat we'll just set them all off on load.
    public ArrayList<HashMap<Character, Integer>> getColors()
    {
        return lastState;
    }

    public void setIndexedColors(ArrayList<HashMap<Character, Integer>> indexedColors) {

        try {
            synchronized (device) {
                byte[] rgbPackage = new byte[(ledCount * 3) + 2];
                rgbPackage[0] = (byte) 8;
                rgbPackage[1] = (byte) 0;
                for (int i = 0; i < ledCount; i += 1) {
                    rgbPackage[(i * 3) + 3] = adjustBrightness(indexedColors.get(i).get('r'));
                    rgbPackage[(i * 3) + 2] = adjustBrightness(indexedColors.get(i).get('g'));
                    rgbPackage[(i * 3) + 4] = adjustBrightness(indexedColors.get(i).get('b'));
                    int finalI = i;
                    HashMap<Character, Integer> state = new HashMap<>() {{
                        put('r', indexedColors.get(finalI).get('r'));
                        put('g', indexedColors.get(finalI).get('g'));
                        put('b', indexedColors.get(finalI).get('b'));
                    }};
                    lastState.set(i,state);
                }
                device.sendFeatureReport(rgbPackage);
                TimeUnit.MILLISECONDS.sleep(millisecondWaitTime);
            }
        } catch (IOException e) {
            millisecondWaitTime = millisecondWaitTime + 1;
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
