#include <Smartcar.h>
#include <SPI.h>
#include <RFID.h> 
#include <stdlib.h>

//GPS     
String data = "";
boolean Mark_Start = false;
boolean valid = false;
const long arraySize = 500;  //if needed, the size can be increased
long savedLocations = 0;
String coordinates[500];    
String GGAUTCtime, GGAlatitude, GGAlongitude, GPStatus, SatelliteNum, HDOPfactor, Height,
PositionValid, RMCUTCtime, RMClatitude, RMClongitude, Speed, Direction, Date, Declination, Mode;
const String NOT_FOUND = "Not yet identified.";
const String NOT_WORKING = "gps module connection problem.";
const String TEST_VALUE = "c-83.582704 -142.186889/";
String gpsLocation = NOT_FOUND;
#define max_char 200       // number of characters to be saved
char message[max_char];    // stores the characters message from app
char r_char;               // reads each character from app sent as digital boundary
byte index = 0;            // for array
String backLat = ""; //boundary 1
String frontLat = ""; //boundary 2
String leftLon = ""; //boundary 3
String rightLon = ""; //boundary 4
String currentLat = ""; //current position of car
String currentLon = ""; //current position of car
int minDistance = 100; //safe distance to allow the car drive without boundary unless specified
boolean startStoring = false; //to signal data can be stored

//Odometer:
const unsigned short LEFT_ODOMETER_PIN = 2;
const unsigned short RIGHT_ODOMETER_PIN = 3;
const int PULSES_PER_METER = 30; //for odometer
DirectionlessOdometer leftOdometer(PULSES_PER_METER);
DirectionlessOdometer rightOdometer(PULSES_PER_METER);

//Ultrasonic sensor:
boolean obstacle = false;
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
const char MOVE_FSPEED1 = 'a';
const char MOVE_FSPEED2 = 'b';
const char GET_LOCATION = 'c';
const char MOVE_FSPEED3 = 'd';
const char MOVE_FSPEED4 = 'e';
const char MOVE_BACKWARD = 'f';
const char SHARP_LEFT = 'g';
const char STEER_LEFT = 'h';
const char STEER_RIGHT = 'i';
const char SHARP_RIGHT = 'j';
const char STAND_STILL = 'k';
const char AUTOMATIC_MODE = 'l';
const char MANUAL_MODE = 'n';
const char REPORT_MINE = 'm';
const char OUT_OF_BOUNDARY = 'o';


//Smartcar:
const int BAUD_RATE = 9600; //for serial
const int ANGLE_CORRECTION = 13;  //offset
const int BACK_ANGLE_CORRECTION = 26; //backwards offset
boolean automode = false;
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
  inputHandler();
  if(rfid.isCard()){                            //Detects mine        
    detectingMine();
  }
  if(automode == true){                         //Automatic mode
    autoMode();
  }
  else if (automode == false){                  //Manual mode
    manualMode();
  }
  //Serial.println(gpsLocation);          //for testing
}

void inputHandler(){
    index = 0;
    
    //Reading location 
    while(Serial3.available() > 0){
      r_char = Serial3.read();      // Reads a character
      
      if(r_char == '#'){        //signal as a boundary info begins
        startStoring = true;
        if(automode)
          car.setSpeed(ZERO);
      }
        
      if(startStoring){
        if(automode)
          car.setSpeed(ZERO);
        message[index++] = r_char;
        delay(10);        //to slow a bit so that data could not be missed
      }
      
      if (r_char == '*')      //signal as a boundary info ends
      {
        String toBeSent = "";
        startStoring = false;
        if (index > 4) {  //minimum of 4 numbers for each latitude and longitude
          for (int i = 0; i < index; i++){    //preparing a string of characters
            toBeSent = toBeSent + message [i];
          }
          setBoundary(toBeSent);
          if(automode)  //check gps
            lookForGPS();
          toBeSent = "";
          index = 0;
        }
      }
    }
}

void detectingMine(){
  //if(isNewLocation()){  //uncomment if not testing
    car.setSpeed(reduceSpeed);
    car.setSpeed(ZERO);
    Serial3.write(REPORT_MINE);
    
    while(!Serial3.available()){}
    char command = Serial3.read();
    handleLocation();                           //sends GPS 
    while(command != REPORT_MINE){
      command = Serial3.read();
    }
    car.setSpeed(fSpeed);
    delay(500);
    car.setSpeed(reduceSpeed);
    car.setSpeed(ZERO);
  //}    //uncomment if not testing
}

void manualMode(){
  char input = Serial3.read();
  switch (input){
    case MOVE_FSPEED1:                          //Makes it go foward in slow speed
      car.setSpeed(sSpeed);
      car.setAngle(ANGLE_CORRECTION);
      break;
    case MOVE_FSPEED2:                          //Makes it go foward in medium speed
      car.setSpeed(sSpeed);
      car.setAngle(ANGLE_CORRECTION);
      break;
    case MOVE_FSPEED3:                          //Makes it go foward in fast speed 
      car.setSpeed(fSpeed);
      car.setAngle(ANGLE_CORRECTION);
      break;
    case MOVE_FSPEED4:                          //Makes it go foward in very fast speed
      car.setSpeed(vfSpeed);
      car.setAngle(ANGLE_CORRECTION);
      break;
    case MOVE_BACKWARD:                          //Makes it go backwards
      car.setSpeed(bSpeed);
      car.setAngle(BACK_ANGLE_CORRECTION);
      break;
    case SHARP_LEFT:                         //Makes it turn sharp left
      car.setSpeed(fSpeed);
      car.setAngle(slDegrees);
      break;
    case STEER_LEFT:                         //Makes it turn left
      car.setSpeed(fSpeed);
      car.setAngle(lDegrees);
      break;
    case STEER_RIGHT:                         //Makes it turn right
      car.setSpeed(fSpeed);
      car.setAngle(rDegrees);
      break;
    case SHARP_RIGHT:                        //Makes it turn sharp right
      car.setSpeed(fSpeed);
      car.setAngle(srDegrees);
      break;
    case STAND_STILL:                       //Makes it stop
      car.setSpeed(reduceSpeed);
      car.setSpeed(ZERO);
      car.setAngle(ZERO);
      break;
    case AUTOMATIC_MODE:                       //letter l Switches to auto mode
      automode = true;
      break;
    case GET_LOCATION:                       //Send GPS
      lookForGPS();                 //reads GPS 
      sendGPS();
      break;
  }
}

//Automatic mode methods:

void autoMode(){
  if(backLat != "") //if boundary is defined
    lookForGPS();
  char input = Serial3.read();
    if(input == MANUAL_MODE){
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
    boolean hasReachedTargetDegrees = false;
    
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
  // Stop after it moves a length of safe distance
  if (car.getDistance() >= minDistance) {
    car.setSpeed(0);
    car.update();
  } else
    return frontSensor.getDistance();
}

/**
   Checks if there is an obstacle within 100cm
*/
boolean obstacleExists(){
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

//search for gps
void lookForGPS(){
  if (Serial1.available()> 0){
    gpsLocation = NOT_FOUND;
    
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
        currentLat = convertedLat;
        currentLon = convertedLon;
        int minDis = setMinDistance(); //to record the safe distance
        if (minDis < 0){
          outOfBoundary();
        }
        valid = false;
      } 
    }
    
    if(Serial1.find("$")){
      //Serial.println("capture");
      Mark_Start = true;
    }
  } else {
    gpsLocation = NOT_WORKING;
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
  
  if(lastChar == 'W' || lastChar == 'S'){
      newString = "-" + newString;
  }
  
  return newString;
}

//Sends the Current GPS Location 
void sendGPS(){
  String gpsToBeSent = gpsLocation;
  int lengthOfChar = gpsToBeSent.length();
  if(gpsToBeSent.equals(NOT_FOUND)){
    gpsToBeSent = TEST_VALUE;  //comment if not testing
    //gpsToBeSent = "x";    //uncomment if not testing
  } else if (gpsToBeSent.equals(NOT_WORKING)){
    gpsToBeSent = "y";
  }
  
  lengthOfChar = gpsToBeSent.length();
  for(int i = 0; i < lengthOfChar; i++){
    char shoot = gpsToBeSent.charAt(i);
    Serial3.write(shoot);
  }
}

//looking for location when mine is detected
void handleLocation(){
  Serial3.end();
  lookForGPS();                             
  Serial3.begin(BAUD_RATE);
  sendGPS();
}

//check if the mine location is new
boolean isNewLocation(){
  if(gpsLocation != NOT_FOUND && gpsLocation != NOT_WORKING /*&& gpsLocation != TEST_VALUE && !isInArray()*/){  //uncomment if out in field
    if(savedLocations < arraySize && gpsLocation != TEST_VALUE)  //test value active only for test purpose
      coordinates[savedLocations++] = gpsLocation;
    return true;
  } else
    return false;
}

//convert the string of boundaries to separate latitudes and longitudes
void setBoundary(String text){
  //preparing for extraction
  text = text.substring(text.indexOf('#') + 1);
  text = text.substring(0, text.indexOf('*'));
  text = text + " ";
  
  Serial.println("new coordinate: " + text);
  
  //first part
  backLat = text.substring(0,text.indexOf(" "));
  text = text.substring(text.indexOf(" ") + 1);
  //Serial.println("new txt: " + text);

  //second part
  frontLat = text.substring(0,text.indexOf(" "));
  text = text.substring(text.indexOf(" ") + 1);
  //Serial.println("new txt: " + text);

  //third part
  leftLon = text.substring(0,text.indexOf(" "));
  text = text.substring(text.indexOf(" ") + 1);
  //Serial.println("new txt: " + text);

  //last part
  rightLon = text.substring(0,text.indexOf(" "));
  Serial.println("The locations are: ");  //for debuging
  Serial.println(backLat);
  Serial.println(frontLat);
  Serial.println(leftLon);
  Serial.println(leftLon);
}

//to check if a coordinate is already existing
boolean isInArray(){
  for (int i = 0; i < savedLocations; i++){
    if(coordinates[i].equals(gpsLocation))
      return true; 
  }
  return false;
}

//min distance to the digital boundaries
int setMinDistance (){
  int lat1Distance = getLat1Distance();
  int lat2Distance = getLat2Distance();
  int lon1Distance = getLon1Distance();
  int lon2Distance = getLon2Distance();
  
  minDistance = min(min(min(lat1Distance, lat2Distance), lon1Distance), lon2Distance);
  return minDistance;
}

//distance between car and latitude1
int getLat1Distance(){
  double latFactorLindholmen = 100000 * 1.125; //distance multiplier around lindholmen
  if(backLat != "" && currentLat != ""){
    double num1 = strtod(currentLat.c_str(), NULL);
    double num2 = strtod(backLat.c_str(), NULL);
    return (int)((num1 - num2) * latFactorLindholmen); //to convert to meters
  }
  else
    return 100;
}

//distance between car and latitude2
int getLat2Distance(){
  double latFactorLindholmen = 100000 * 1.125; //distance multiplier around lindholmen
  if(frontLat != "" && currentLat != ""){
    double num1 = strtod(currentLat.c_str(), NULL);
    double num2 = strtod(frontLat.c_str(), NULL);
    return (int) ((num2 - num1) * latFactorLindholmen); //to convert to meters
  }
  else
    return 100;
}

//distance between car and longitude1
int getLon1Distance(){
  double lonFactorLindholmen = 100000*0.6316; //distance multiplier around lindholmen
  if(leftLon != "" && currentLon != ""){
    double num1 = strtod(currentLon.c_str(), NULL);
    double num2 = strtod(leftLon.c_str(), NULL);
    return (int)((num1 - num2) * lonFactorLindholmen); //to convert to meters
  }
  else
    return 100;
}

//distance between car and longitude2
int getLon2Distance(){
  double lonFactorLindholmen = 100000*0.6316; //distance multiplier around lindholmen
  if(rightLon != "" && currentLon != ""){
    double num1 = strtod(currentLon.c_str(), NULL);
    double num2 = strtod(rightLon.c_str(), NULL);
    return (int) abs((num2 - num1) * lonFactorLindholmen);  //meters
  }
  else
    return 100;
}

//if sweeper is out of boundary
void outOfBoundary(){
    car.setSpeed(reduceSpeed);
    car.setSpeed(ZERO);
    Serial3.write(OUT_OF_BOUNDARY); //send info to app
    
    while(!Serial3.available()){}
    char command = Serial3.read();
    
    while(command != MANUAL_MODE){  //wait until manual mode is enabled
      command = Serial3.read();
    }
}
