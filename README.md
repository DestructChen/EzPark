# EzPark 
A uni project for University of Sydney UOS ELEC3607 - Embedded Systems
For this project we were to create a prototype embedded system
EzPark is a Bluetooth Parking Reservation System utilising the Arduino DUE as a server and a user is able to connect to it via our Android application with Bluetooth

### File Information
EzParkApp folder should be accessed via [Android Studio](https://developer.android.com/studio/index.html). Ensure you have read the [Terms & Conditions](https://developer.android.com/studio/terms.html) of using Android Studio.


The EzPark.ino file is an arduino code file. To open the file download the [Arduino software.](https://www.arduino.cc/en/Main/Software).
With a knowledge of Arduino and the C programming language, the EzPark.ino code should be straight forward to follow. The code was created on use of an Arduino DUE however, the code is simplistic and should be compatible with most Arduino devices.


##EzPark Arduino
For this project we used a [SeeedStudio Bluetooth Module](http://wiki.seeedstudio.com/wiki/Bluetooth_Shield)
For this Bluetooth Shield, jumper wires were requried to connect the RX pin of the bluetooth module to the TX pin of the arduino board and similarly the TX pin of the bluetooth module to the RX pin of the arduino board.

For this project, the Pin Assignment is as follows

| Hardware | Pin # |
| --- | --- |
| LED | 1 |
| Trig | 11 |
| Echo | 12 |
| Servo Motor | 13 |
