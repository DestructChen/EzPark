/*
  ______     _____           _
  |  ____|   |  __ \         | |
  | |__   ___| |__) |_ _ _ __| | __
  |  __| |_  /  ___/ _` | '__| |/ /
  | |____ / /| |  | (_| | |  |   <
  |______/___|_|   \__,_|_|  |_|\_\

  This is the arduino file for our EzPark project.
  The code was created on use of an Arduino DUE however, the code is simplistic and should be compatible with most Arduino devices with slight modifications.
  Note:The Arduino DUE has more than 1 Serial Ports so unlike some other Arduino we are able to utilise a Serial2 port referenced in the code.

*/




#include <Servo.h>
/*  <Servo Library>
    This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation;
    either version 2.1 of the License, or (at your option) any later version.
    Reference to  Official Documentation at :
        https://github.com/arduino-libraries/Servo
        https://www.arduino.cc/en/Reference/Servo
*/

#define blueToothSerial Serial2     //Second serial
#define SSDSIZE 7                   // number of segments in 7SD
#define DIGSIZE 10                  // number of different symbols to decode
#define ssdpin(i)  (i+4)            // a to pin 4, b to pin 5 etc so the pin offset is +4
#define numspaces 9                 //number of parking spaces
#define numuser 10                  //number of users, value should be 1 more than numspaces
#define WAITTIME 15000

const int trigPin   = 11;
const int echoPin   = 12;
const int servoPin  = 13;
const int ledPin    =  1;

struct user {
  String platenumber;
  int assignedSpot;
  String start;
};

// defines global variables
long duration;
int distance;
int pos;
char* command;
int *parkingSpaces;
int currentuser = -1;
Servo myservo;  // create servo object to control a servo
user userdatabase[numuser];
int ledState = 0; //current LED state
int lastLedState = 0; // previous LED state
int availableSpaces = numspaces;

//Array of integer arrays containing HIGH/LOW values for segments a-g for the numbers 0-9
//Ideally the seven segment display should be connected in a sequential order on the arduino for efficiency and compatability with the arduino code
//your seven segment displays may be configured differently. please the associated documentation that came with your hardware
int ssdtable[DIGSIZE][SSDSIZE] =
{
  {1, 1, 1, 1, 1, 1, 0},
  {0, 1, 1, 0, 0, 0, 0},
  {1, 1, 0, 1, 1, 0, 1},
  {1, 1, 1, 1, 0, 0, 1},
  {0, 1, 1, 0, 0, 1, 1},
  {1, 0, 1, 1, 0, 1, 1},
  {1, 0, 1, 1, 1, 1, 1},
  {1, 1, 1, 0, 0, 0, 0},
  {1, 1, 1, 1, 1, 1, 1},
  {1, 1, 1, 1, 0, 1, 1}
};



void setup()
{

  Serial.begin(9600);
  setupBlueToothConnection();
  pinMode(trigPin, OUTPUT);   // Sets the trigPin as an Output
  pinMode(echoPin, INPUT);    // Sets the echoPin as an Input
  pinMode(ledPin, OUTPUT);    // Sets the ledPin as an Output
  myservo.attach(servoPin);   // attaches the servo pin to the servo object

  for (int j = 0; j < SSDSIZE; j++) {
    pinMode(ssdpin(j), OUTPUT);     //Assigning each segment of the display as Output
  }
  parkingSpaces = (int*)malloc(numspaces * sizeof(int));
  for (int i = 0; i < numspaces; i++) {
    parkingSpaces[i] = 0;
  }
  myservo.write(0);               //default position of servo
  digitalWrite(ledPin, LOW);      //default Off for LED
  blueToothSerial.flush();

}

void loop()
{
  if (blueToothSerial.available() > 0)
  {
    String data = blueToothSerial.readString();

    Serial.println(data);
    char input[256];
    data.toCharArray(input, 256);
    char* token = strtok(input, " ");
    String receiveplatenumber = token;

    currentuser = -1;

    if (receiveplatenumber.length() < 4 ) {

    } else
    {
      int nextavailableuser = -1;

      //find the current user with the given platenumber
      for (int i = 0; i < numspaces; i++) {
        if (userdatabase[i].platenumber.equals(receiveplatenumber)) {
          currentuser = i;
          break;
        }
      }

      //find the next available user position
      for (int i = 0; i < numspaces; i++) {
        if (userdatabase[i].platenumber.length() < 4) {
          nextavailableuser = i;
          break;
        }
      }


      token = strtok(NULL, " ");
      command = token;

      if (strncmp("getStart", command, 14) == 0) {            //return start time of reservation
        if (currentuser != -1) {
          blueToothSerial.print(userdatabase[currentuser].start);
        } else {
          blueToothSerial.print("numberplate not found");
        }


      } else if (strncmp("registeruser", command, 12) == 0) { //create new user with the given data
        if (currentuser == -1) {
          token = strtok(NULL, " ");
          int spot = atoi(token);
          token = strtok(NULL, " ");
          String starttime = token;

          if (parkingSpaces[spot] == 0) {
            userdatabase[nextavailableuser] = {receiveplatenumber, spot, starttime};
            parkingSpaces[spot] = 1;
            availableSpaces--;
            blueToothSerial.print("success");
            doorOpenCloseSequence(1);
            return;
          } else {
            blueToothSerial.print("space was taken");    //incase multiple users try to register the same spot at once.
          }
        } else {
          blueToothSerial.print("numberplate already exists"); //if currentuser is found in the database
        }
      }
      //return status of the available spaces
      //e.g. 10 1 0 0 0 1 0 0 1 0 1
      //first value is numspaces
      //1 = occupied
      //0 = vacant
      else if (strncmp("getSpaces", command, 8) == 0) {
        blueToothSerial.print(numspaces);
        blueToothSerial.print(" ");
        for (int i = 0; i < numspaces - 1; i++) {
          blueToothSerial.print(parkingSpaces[i]);
          blueToothSerial.print(" ");
        }
        blueToothSerial.print(parkingSpaces[numspaces - 1]);

      }//payment finished. aka remove user and vacate the parking space.
      else if (strncmp("paymentfinish", command, 13) == 0) {
        int finishedspot = userdatabase[currentuser].assignedSpot;

        userdatabase[currentuser] = {"", -1, ""};
        parkingSpaces[finishedspot] = 0;
        blueToothSerial.print("dooropened");
        availableSpaces++;
        doorOpenCloseSequence(0);

      }
      blueToothSerial.flush();
    }


  }

  ssddecode(availableSpaces % 10); //update seven segment display with available spaces
  delay(500);

}

//LED will "blink" to warn user that the door is closing
void closeDoor() {
  for (int i = 0; i < 5; i++) {
    digitalWrite(ledPin, HIGH);
    delay(200);
    digitalWrite(ledPin, LOW);
    delay(200);
  }
  myservo.write(pos);
}

//opens door and waits 15s or until something triggers the ultrasonic sensor before closing
//wait time can be modified. current value is used for prototyping
//state 1 wait for car to enter
//state 0 wait for car to leave
void doorOpenCloseSequence(int state) {
  pos = 90;
  myservo.write(pos);
  pos = 0;
  delay(1000);
  unsigned long currentTime = millis();
  while (millis() - currentTime < WAITTIME) {
    checkDistance();
    delay(1000);
    if (state == 1 && distance < 12) {
      break;
    } else if (state == 0 && distance > 12 ) {
      break;
    }
  }
  closeDoor();
}

void checkDistance() {
  // Clears the trigPin
  digitalWrite(trigPin, LOW);
  delayMicroseconds(2);
  // Sets the trigPin on HIGH state for 10 micro seconds
  digitalWrite(trigPin, HIGH);
  delayMicroseconds(10);
  digitalWrite(trigPin, LOW);
  // Reads the echoPin, returns the sound wave travel time in microseconds
  duration = pulseIn(echoPin, HIGH);
  // Calculating the distance
  distance = duration * 0.034 / 2; //divide by 2 to account for output + input sound wave
}

// Initialise bluetooth module connection
void setupBlueToothConnection()
{
  blueToothSerial.begin(38400);                         //Set Bluetooth BaudRate to default baud rate 38400
  blueToothSerial.print("\r\n+STWMOD=0\r\n");           //set the bluetooth work in slave mode
  blueToothSerial.print("\r\n+STNA=PotatoMachine\r\n"); //set the bluetooth name as "SeeedBTSlave"
  blueToothSerial.print("\r\n+STOAUT=1\r\n");           // Permit Paired device to connect me
  blueToothSerial.print("\r\n+STAUTO=0\r\n");           // Auto-connection should be forbidden here
  // blueToothSerial.print("\r\n+STPIN=0000\r\n");           OPTIONAL change to custom pin
  delay(2000);                                          // This delay is required.
  blueToothSerial.print("\r\n+INQ=1\r\n");              //make the slave bluetooth inquirable
  Serial.println("The slave bluetooth is inquirable!");
  delay(2000);                                          // This delay is required.
  blueToothSerial.flush();
}

//takes the n-th array from ssdtable and sends high/low to corresponding segment of the seven segment display
void ssddecode(int number)
{
  for (int j = 0; j < SSDSIZE; j++) {
    digitalWrite(ssdpin(j),  ssdtable[number][j] == 1 ? HIGH : LOW);
  }
}


