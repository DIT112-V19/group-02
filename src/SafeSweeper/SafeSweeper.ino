#include <Smartcar.h>
#include <SPI.h>
#include <RFID.h>   

//GPS     
String data = "";
boolean Mark_Start = false;
boolean valid = false;
String GGAUTCtime,GGAlatitude,GGAlongitude,GPStatus,SatelliteNum,HDOPfactor,Height,
PositionValid,RMCUTCtime,RMClatitude,RMClongitude,Speed,Direction,Date,Declination,Mode;

String gpsLocation = "c0 0/";

//Odometer:
const unsigned short LEFT_ODOMETER_PIN = 2;
const unsigned short RIGHT_ODOMETER_PIN = 3;
const int PULSES_PER_METER = 30; //for odometer
DirectionlessOdometer leftOdometer(PULSES_PER_METER);
DirectionlessOdometer rightOdometer(PULSES_PER_METER);

//Ultrasonic sensor:
bool obstacle = false;
const unsigned int MAX_DISTANCE = 60;  //recognizable by sensor
const unsigned int MIN_DISTANCE = 20; //distance to obstacle
const int TRIGGER_PIN = 6; //D6
const int ECHO_PIN = 7; //D7
SR04 frontSensor(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE);

//Gyroscope:
const int GYROSCOPE_OFFSET = 37;
GY50 gyroscope(GYROSCOPE_OFFSET);

//RFID reader:
#define SS_PIN 4
#define RST_PIN 5
RFID rfid(SS_PIN, RST_PIN);
int serNum[5];
int cards[][5] = {{129,243,229,47,184}};

//Automatic mode:
const float CAR_SPEED = 30.0;
const int ZERO = 0;
const int MIN_B = 5;
const int EVEN = 2;

//Manual mode:
const int vfSpeed = 75; //very fast speed
const int fSpeed = 50; //fast speed
const int mSpeed = 35; //medium speed
const int sSpeed = 20; //slow speed
const int reduceSpeed = 10; //to reduce speed
const int bSpeed = -50; //50% of the full speed backward
const int slDegrees = -75; //degrees to turn left
const int lDegrees = -50;
const int srDegrees = 75; //degrees to turn right
const int rDegrees = 50;

//Smartcar:
const int BAUD_RATE = 9600; //for serial
const int ANGLE_CORRECTION = 13;  //offset
const int BACK_ANGLE_CORRECTION = 26; //backwards offset
bool automode = false;
BrushedMotor leftMotor(8, 10, 9);
BrushedMotor rightMotor(12, 13, 11);
DifferentialControl control(leftMotor, rightMotor);
SmartCar car(control, gyroscope, leftOdometer, rightOdometer);

void setup() {
  Serial.begin(BAUD_RATE);  //The general serial
  Serial3.begin(BAUD_RATE); //Serial for bluetooth
  Serial1.begin(BAUD_RATE); //Serial for GPS
  SPI.begin();              //Initializes the pins for the RFID reader
  rfid.init();              //Initializes the reader
  car.setAngle(ANGLE_CORRECTION);
  pinMode(14,INPUT);        //Input pin for bluetooth
  pinMode(15,OUTPUT);       //Output pin for bluetooth

  leftOdometer.attach(LEFT_ODOMETER_PIN, []() {
    leftOdometer.update();
  });
  rightOdometer.attach(RIGHT_ODOMETER_PIN, []() {
    rightOdometer.update();
  });

  // if analog input pin 0 is unconnected, random analog
  // noise will cause the call to randomSeed() to generate
  // different seed numbers each time the sketch runs.
  // randomSeed() will then shuffle the random function.
  randomSeed(analogRead(0));
}

void loop() {
  if(rfid.isCard()){                            //Detects mine        
    detectingMine();
  }
  if(automode == true){                         //Automatic mode
    autoMode();
  }
  else if (automode == false){                  //Manual mode
    manualMode();
  }
}

void detectingMine(){
    car.setSpeed(reduceSpeed);
    car.setSpeed(ZERO);
    Serial3.write('m');
    
    while(!Serial3.available()){}
    char command = Serial3.read();
    handleLocation();                           //sends GPS 
    while(command != 'm'){
      command = Serial3.read();
    }
}

void manualMode(){
  char input = Serial3.read();
    if (input == '1'){                          //Makes it go foward in slow speed
      car.setSpeed(sSpeed);
      car.setAngle(ANGLE_CORRECTION);
    }
    else if (input == '2'){                     //Makes it go foward in medium speed
      car.setSpeed(mSpeed);
      car.setAngle(ANGLE_CORRECTION);
    }
    else if (input == '3'){                     //Makes it go foward in fast speed 
      car.setSpeed(fSpeed);
      car.setAngle(ANGLE_CORRECTION);
    }
    else if (input == '4'){                     //Makes it go foward in very fast speed
      car.setSpeed(vfSpeed);
      car.setAngle(ANGLE_CORRECTION);
    }    
    else if (input == '5'){                     //Makes it go backwards
      car.setSpeed(bSpeed);
      car.setAngle(BACK_ANGLE_CORRECTION);
    } 
    else if (input == 'w'){                     //Makes it turn sharp left
      car.setSpeed(fSpeed);
      car.setAngle(slDegrees);
    }
    else if (input == 'y'){                     //Makes it turn left
      car.setSpeed(fSpeed);
      car.setAngle(lDegrees);
    }
    else if (input == 'z'){                     //Makes it turn right
      car.setSpeed(fSpeed);
      car.setAngle(rDegrees);
    }
    else if (input == 'x'){                     //Makes it turn sharp right
      car.setSpeed(fSpeed);
      car.setAngle(srDegrees);
    }
    else if (input == '0'){                     //Makes it stop
      car.setSpeed(reduceSpeed);
      car.setSpeed(ZERO);
      car.setAngle(ZERO);
    }
    else if (input == '7'){                     //Switches to automode
      automode = true;
    }
    else if (input == 'c'){                     //Send GPS
    lookForGPS();                               //reads GPS 
    sendGPS();
    }
}



//Automatic mode methods:

void autoMode(){
  char input = Serial3.read();
    if(input == '6'){
      automode = false;
      car.setSpeed(reduceSpeed);
      car.setSpeed(ZERO);
    }
    if(!obstacleExists()&& automode){
      car.setAngle(ANGLE_CORRECTION);
      car.setSpeed(CAR_SPEED);
    } 
    else {                        
        rotateTillFree();
    }
}
/**
   Rotate the car at specified degrees with certain speed untill there is no obstacle
*/
void rotateTillFree() {
  int degrees = random(30, 160);
  
  while(obstacleExists()){
    unsigned int initialHeading = car.getHeading();
    bool hasReachedTargetDegrees = false;
    
    while (!hasReachedTargetDegrees) {
      
      rotateOnSpot(degrees, CAR_SPEED);
      int currentHeading = car.getHeading();
      
      if ( currentHeading > initialHeading) {
        // If we are turning left and the current heading is larger than the
        // initial one (e.g. started at 10 degrees and now we are at 350), we need to substract 360
        // so to eventually get a signed displacement from the initial heading (-20)
        currentHeading -= 360;
      } else if (degrees > ZERO && currentHeading < initialHeading) {
        // If we are turning right and the heading is smaller than the
        // initial one (e.g. started at 350 degrees and now we are at 20), so to get a signed displacement (+30)
        currentHeading += 360;
      }
      // Degrees turned so far is initial heading minus current (initial heading
      // is at least 0 and at most 360. To handle the "edge" cases we substracted or added 360 to currentHeading)
      int degreesTurnedSoFar = initialHeading - currentHeading;
      hasReachedTargetDegrees = smartcarlib::utils::getAbsolute(degreesTurnedSoFar) >= smartcarlib::utils::getAbsolute(degrees);
    }
  }
}
/**
   Measure and return the distance of obstacle within 100 cm
*/
int getObstacleDistance(){
  return frontSensor.getDistance();
}
/**
   Checks if there is an obstacle within 100cm
*/
bool obstacleExists(){
  int distanceToObstacle = getObstacleDistance();
  
  if(distanceToObstacle < MIN_DISTANCE && distanceToObstacle > MIN_B){
    return true;
  } else {
    return false;
  }
}

/**
   Rotate the car on spot at the specified degrees with the certain speed
   @param degrees   The degrees to rotate on spot. Positive values for clockwise
                    negative for counter-clockwise.
   @param speed     The speed to rotate
*/
void rotateOnSpot(int targetDegrees, int speed) {
  speed = smartcarlib::utils::getAbsolute(speed);
  targetDegrees %= 360; //put it on a (-360,360) scale
  if (!targetDegrees) return; //if the target degrees is 0, don't bother doing anything
  /* Let's set opposite speed on each side of the car, so it rotates on spot */
  if (targetDegrees > 0) { //positive value means we should rotate clockwise
    car.overrideMotorSpeed(speed, -speed); // left motors spin forward, right motors spin backward
  } else { //rotate counter clockwise
    car.overrideMotorSpeed(-speed, speed); // left motors spin backward, right motors spin forward
  }
  unsigned int initialHeading = car.getHeading(); //the initial heading we'll use as offset to calculate the absolute displacement
  int degreesTurnedSoFar = 0; //this variable will hold the absolute displacement from the beginning of the rotation
  while (abs(degreesTurnedSoFar) < abs(targetDegrees)) { //while absolute displacement hasn't reached the (absolute) target, keep turning
    if(rfid.isCard()){                            //Detects mine        
      detectingMine();
      if (targetDegrees > 0) { //positive value means we should rotate clockwise
        car.overrideMotorSpeed(speed, -speed); // left motors spin forward, right motors spin backward
      } else { //rotate counter clockwise
        car.overrideMotorSpeed(-speed, speed); // left motors spin backward, right motors spin forward
      } 
    }
    car.update(); //update to integrate the latest heading sensor readings
    int currentHeading = car.getHeading(); //in the scale of 0 to 360
    if ((targetDegrees < 0) && (currentHeading > initialHeading)) { //if we are turning left and the current heading is larger than the
      //initial one (e.g. started at 10 degrees and now we are at 350), we need to substract 360, so to eventually get a signed
      currentHeading -= 360; //displacement from the initial heading (-20)
    } else if ((targetDegrees > 0) && (currentHeading < initialHeading)) { //if we are turning right and the heading is smaller than the
      //initial one (e.g. started at 350 degrees and now we are at 20), so to get a signed displacement (+30)
      currentHeading += 360;
    }
    degreesTurnedSoFar = initialHeading - currentHeading; //degrees turned so far is initial heading minus current (initial heading
    //is at least 0 and at most 360. To handle the "edge" cases we substracted or added 360 to currentHeading)
  }
}

//
void lookForGPS(){
  if (Serial1.available()> 0){
    gpsLocation = "c0 0/";
    
    if(Mark_Start){
      data = reader();
      if(data.equals("GPGGA")){
        GGAUTCtime = reader();
        GGAlatitude = reader();
        GGAlatitude+=reader();
        GGAlongitude = reader();
        GGAlongitude+=reader();
        GPStatus = reader();
        SatelliteNum = reader();
        HDOPfactor = reader();
        Height = reader();
        Mark_Start = false;
        valid = true;
        data = "";
      } else if(data.equals("GPGSA")){
        Mark_Start = false;
        data = "";
      } else if(data.equals("GPGSV")){
        Mark_Start = false;
        data = "";
      } else if(data.equals("GPRMC")){
        RMCUTCtime = reader();
        PositionValid = reader();
        RMClatitude = reader();
        RMClatitude+=reader();
        RMClongitude = reader();
        RMClongitude+=reader();
        Speed = reader();
        Direction = reader();
        Date = reader();
        Declination = reader();
        Declination+=reader();
        Mode = reader();
        valid = true;
        Mark_Start = false;
        data = "";
      } else if(data.equals("GPVTG")){
        Mark_Start = false;
        data = "";
      } else{
        Mark_Start = false;
        data = "";
      }
    }
    
    if(valid){
      if(PositionValid == "A"){
        Serial.print("Latitude:");
        //Serial.print(RMClatitude);
        String convertedLat = convertData(RMClatitude);
        //Serial.print("   ");
        Serial.println(convertedLat);
        Serial.print("Longitude:");
        //Serial.print(RMClongitude);
        String convertedLon = convertData(RMClongitude);
        //Serial.print("   ");
        Serial.println(convertedLon);
        Serial.println(" ");
        gpsLocation = "c" + convertedLat + " " + convertedLon + "/";
        valid = false;
      } 
    }
    
    if(Serial1.find("$")){
      //Serial.println("capture");
      Mark_Start = true;
    }
  }
}

//reader of GPS if triggered to read
String reader(){
  String value = "";
  int temp;
  while (Serial1.available() > 0){
    delay(2);
    temp = Serial1.read();
    if((temp == ',')||(temp == '*')){
      if(value.length()){
        return value;
      } else {
        return "";
      }     
    } else if(temp == '$'){
      //Serial.println("failure");
      Mark_Start = false;
    } else {
      //Serial.println("add");
      value+=char(temp);
    }
  }
}

//data from GPS will be converted to suitable string
String convertData(String rawString){
  char lastChar = rawString.charAt(rawString.length()-1);
  int dotAt = rawString.indexOf(".");
  String leftPart = rawString.substring(0,dotAt);
  String rightPart = rawString.substring(dotAt+1, (rawString.length()-1));
  int leftDigit = leftPart.length();
  String left1 = leftPart.substring(0, leftDigit - 2);
  String left2 = leftPart.substring(leftDigit-2);
  String newString = left1 + "." + left2 + rightPart;
  /*while(newString.charAt(0) == '0'){
      newString = newString.substring(1);
  }*/
  
  if(lastChar == 'W' || lastChar == 'S'){
      newString = "-" + newString;
  }
  
  return newString;
}

//Sends the Current GPS Location 
void sendGPS(){
  String gpsToBeSent = gpsLocation;
  int lengthOfChar = gpsToBeSent.length();
  if(lengthOfChar==5){
    gpsToBeSent = "c22.22222 011.111111/";  //temporary if not found
  }
  lengthOfChar = gpsToBeSent.length();
  for(int i = 0; i < lengthOfChar; i++){
    char shoot = gpsToBeSent.charAt(i);
    Serial3.write(shoot);
  }
}

void handleLocation(){
  Serial3.end();
  lookForGPS();                                 
  Serial3.begin(BAUD_RATE);
  sendGPS();
}
