# BlinkStick-REST
A basic project for adding a REST front for a blinkstick device.

I'm a fairly average Java developer and as such I did the balance of the quickest REST server with the easiest components of BlinkStick technology.

**WARNING**
If you intend to use this with a raspberry pi... use this project (pure python):
https://github.com/camstillhere/BlinkStick-Restful-python-
HIDAPI jar that this bundles with is not easy to get working on pi..


# Launch your jar

jdk-11.0.6+10\bin\java.exe -jar blinkStickRest-1.0-SNAPSHOT-all.jar
This will display something like this:

```
BlinkStick Flex(32)
Starting up the controller
BS036121-3.1
Starting up the http server
http://192.168.1.112:80/list
http://192.168.1.112:80/web/index.html
All finished
```

The above shows that my BlinkStick Flex with 32 LED's has been detected, has a serial of BS036121-3.1 and can be interacted on 192.168.1.112
It's worth noting all IP's and hostname is supported.

## Launch a browser

Open the second link pointing to a /web/ folder
http://192.168.1.112:80/web/index.html

With this site you can run a number of tests to ensure that your blinkstick is functioning.
You can watch the api's being called and see how it works.

When you are ready to start integrating it into your projects:

## List all Methods
http://192.168.1.112:80

## List all blinkstick devices:
http://192.168.1.112:80/list

## Set all Off
http://192.168.1.112/setColor?deviceId=BS036121-3.1&r=0&g=0&b=0

## Set all On
http://192.168.1.112/setColor?deviceId=BS036121-3.1&r=255&g=255&b=255

## Set the first LED to RED
http://192.168.1.112/setColor?deviceId=BS036121-3.1&r=255&g=0&b=0&index=0

## Get all the current colors
http://192.168.1.112/getColors?deviceId=BS036121-3.1

## Set all individual colors at the same time
POST http://192.168.1.112/setColors?deviceId=BS036121-3.1
BODY (JSON ARRAY OF RGB Objects, same number as max LEDS)
```
[
    {"r":255,"g":0,"b":0},
    {"r":255,"g":125,"b":0},
    {"r":255,"g":255,"b":0},
    {"r":125,"g":255,"b":0},
    {"r":0,"g":255,"b":0},
    {"r":0,"g":255,"b":125},
    {"r":0,"g":255,"b":255},
    {"r":0,"g":125,"b":255},
    {"r":0,"g":0,"b":255},
    {"r":125,"g":0,"b":255},
    {"r":255,"g":0,"b":255},
    {"r":255,"g":0,"b":125},
    {"r":255,"g":0,"b":0},
    {"r":255,"g":125,"b":0},
    {"r":255,"g":255,"b":0},
    {"r":125,"g":255,"b":0},
    {"r":0,"g":255,"b":0},
    {"r":0,"g":255,"b":125},
    {"r":0,"g":255,"b":255},
    {"r":0,"g":125,"b":255},
    {"r":0,"g":0,"b":255},
    {"r":125,"g":0,"b":255},
    {"r":255,"g":0,"b":255},
    {"r":255,"g":0,"b":125},
    {"r":255,"g":0,"b":0},
    {"r":255,"g":125,"b":0},
    {"r":255,"g":255,"b":0},
    {"r":125,"g":255,"b":0},
    {"r":0,"g":255,"b":0},
    {"r":0,"g":255,"b":125},
    {"r":0,"g":255,"b":255},
    {"r":0,"g":125,"b":255}
]
```
