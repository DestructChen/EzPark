#define blueToothSerial Serial2
#define SSDSIZE 7       // number of segments in 7SD
#define DIGSIZE 10      // number of different symbols to decode
#define  ssdpin(i)  (i+4) // a to pin 2, b to pin 3 etc so the pin offset is +2
#define numspaces 9

const int trigPin = 11;
const int echoPin = 12;
const int servoPin = 13;
const int ledPin = 1;
// defines variables
long duration;
int distance;
int pos = 0;
String data;
char* command;


#include <Servo.h>

Servo myservo;  // create servo object to control a servo

int availableSpaces = numspaces;
struct user{
    String platenumber;
    int assignedSpot;
    String start;
};
int *parkingSpaces;

int ssdtab[DIGSIZE][SSDSIZE] =
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
int currentuser = -1;
user userdatabase[10];
int ledState = 0; //current LED state
int lastLedState = 0; // previous LED state

void setup()
{
  
    Serial.begin(9600);
    setupBlueToothConnection();
    pinMode(trigPin, OUTPUT); // Sets the trigPin as an Output
    pinMode(echoPin, INPUT); // Sets the echoPin as an Input
    pinMode(ledPin, OUTPUT);
    myservo.attach(servoPin);  // attaches the servo on pin 9 to the servo object
    for (int j = 0; j < SSDSIZE; j++){
        pinMode(ssdpin(j), OUTPUT);
    }
    parkingSpaces = (int*)malloc(numspaces*sizeof(int));
    for(int i = 0;i<numspaces;i++){
      parkingSpaces[i]=0;
    }
    myservo.write(0); 
    digitalWrite(ledPin, LOW);
    blueToothSerial.flush();

    
}
 
void loop()
{
        
        if(blueToothSerial.available()>0)
        {

          String dataa = blueToothSerial.readString();
         
          Serial.println(dataa);
          char input[256];
          dataa.toCharArray(input,256);
          char* token = strtok(input," ");
          String receiveplatenumber = token;
          
          currentuser=-1;
          
          if((receiveplatenumber.length()<4 )|| (receiveplatenumber.substring(0)=="numberplate") || (receiveplatenumber.charAt(0)=='+')){
          
          }else
          {
          int nextavailableuser = -1;

          //find the current user
          for(int i = 0;i<numspaces;i++){
            if(userdatabase[i].platenumber.equals(receiveplatenumber)){
              currentuser=i;
              break;
            }
          }

          
          //find the next available user position
          for(int i = 0;i<numspaces;i++){
            if(userdatabase[i].platenumber.length()<4){
              nextavailableuser = i;
              break;
            }
          }
        
          
          token=strtok(NULL," ");
          command = token;
          
          if(strncmp("getStart",command,14)==0){
            if(currentuser!=-1){
            blueToothSerial.print(userdatabase[currentuser].start);
            }else{
              blueToothSerial.print("numberplate not found");
            }
              
              
          }else if(strncmp("registeruser",command,12)==0){
            if(currentuser==-1){
              token= strtok(NULL," ");
              int spot = atoi(token);
              token= strtok(NULL," ");
              String starttime = token;
              if(parkingSpaces[spot]==0){
                userdatabase[nextavailableuser]={receiveplatenumber, spot, starttime};
                  parkingSpaces[spot]=1;
                  availableSpaces--;
                  blueToothSerial.print("success");
                    doorOpenCloseSequence(1);
                    return;
              }else{
        
                blueToothSerial.print("space was taken");    
                
              }
           
              } else{
                blueToothSerial.print("numberplate already exists");
              }
          }else if(strncmp("getSpaces", command, 8)==0){
              blueToothSerial.print(numspaces);
              blueToothSerial.print(" ");
              for(int i = 0;i<numspaces-1;i++){
                blueToothSerial.print(parkingSpaces[i]);
                blueToothSerial.print(" ");
              }
                blueToothSerial.print(parkingSpaces[numspaces-1]);
                
          }else if(strncmp("paymentfinish",command,13)==0){
            int finishedspot = userdatabase[currentuser].assignedSpot;
            //open door 
              //open finishedspot door;
              userdatabase[currentuser]={"",-1,""};
              parkingSpaces[finishedspot]=0;
              blueToothSerial.print("dooropened");  
              availableSpaces++;
              doorOpenCloseSequence(0);
              
              
              
          }
          digitalWrite(ledPin, LOW);
           //Serial.println(dataa);
          blueToothSerial.flush();
          }
          
        
        }
  

ssddecode(availableSpaces%10);
delay(500);

}
void doorClosingLedWarning(){
    for(int i = 0;i<3;i++){
    digitalWrite(ledPin, HIGH);
    delay(200);
    digitalWrite(ledPin, LOW);
    delay(200);
    }
}

void doorOpenCloseSequence(int state){
     pos=90;
     myservo.write(pos);    
     pos=0;
     delay(1000);
     unsigned long currentTime = millis();
     while(millis()-currentTime<15000){
      checkDistance();
      delay(1000);
      if(state == 1 && distance<12){
             doorClosingLedWarning();
             myservo.write(pos);
             break;
            
     }else if(state == 0 && distance>12 ){
             doorClosingLedWarning();
             myservo.write(pos);
             break;
             

      }
 
  }
  doorClosingLedWarning();
  myservo.write(pos);
             
}

void checkDistance(){
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
      distance= duration*0.034/2;
}

void setupBlueToothConnection()
{
  blueToothSerial.begin(38400); //Set Bluetooth BaudRate to default baud rate 38400
  blueToothSerial.print("\r\n+STWMOD=0\r\n"); //set the bluetooth work in slave mode
  blueToothSerial.print("\r\n+STNA=PotatoMachine\r\n"); //set the bluetooth name as "SeeedBTSlave"
  blueToothSerial.print("\r\n+STOAUT=1\r\n"); // Permit Paired device to connect me
  blueToothSerial.print("\r\n+STAUTO=0\r\n"); // Auto-connection should be forbidden here
 // blueToothSerial.print("\r\n+STPIN=0000\r\n");
  delay(2000); // This delay is required.
  blueToothSerial.print("\r\n+INQ=1\r\n"); //make the slave bluetooth inquirable 
  Serial.println("The slave bluetooth is inquirable!");
  delay(2000); // This delay is required.
  blueToothSerial.flush();      
}

void ssddecode(int a)
{
  for (int j = 0; j < SSDSIZE; j++){
    digitalWrite(ssdpin(j),  ssdtab[a][j] == 1 ? HIGH : LOW);
  }
}
