#include <Smartcar.h>

const unsigned short LEFT_ODOMETER_PIN = 2;
const unsigned short RIGHT_ODOMETER_PIN = 3;
const int TRIGGER_PIN = 6; //D6
const int ECHO_PIN = 7; //D7

const float CAR_SPEED = 40.0;
const float SLOWER_SPEED = 30.0;
const int TURN_RIGHT = 75;  //angle
const int ANGLE_CORRECTION = 13;  //offset
const int GYROSCOPE_OFFSET = 37;
const int BAUD_RATE = 9600; //for serial
const int PULSES_PER_METER = 30; //for odometer
const int FULL_CIRCLE = 360; //degree
const int ZERO = 0;
const int MIN_B = 5;
const int EVEN = 2;

const unsigned int MAX_DISTANCE = 60;  //recognizable by sensor
const unsigned int MIN_DISTANCE = 30; //distance to obstacle

BrushedMotor leftMotor(8, 10, 9);
BrushedMotor rightMotor(12, 13, 11);
DifferentialControl control(leftMotor, rightMotor);

SR04 frontSensor(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE);
GY50 gyroscope(GYROSCOPE_OFFSET);
DirectionlessOdometer leftOdometer(PULSES_PER_METER);
DirectionlessOdometer rightOdometer(PULSES_PER_METER);

HeadingCar car(control, gyroscope);

void setup() {
  Serial.begin(9600);
  car.setAngle(ANGLE_CORRECTION);

  leftOdometer.attach(LEFT_ODOMETER_PIN, []() {
    leftOdometer.update();
  });
  rightOdometer.attach(RIGHT_ODOMETER_PIN, []() {
    rightOdometer.update();
  });
}

void loop() {
  Serial.println(frontSensor.getDistance());
  if(!obstacleExists()){
    car.setSpeed(CAR_SPEED);
  } else {
    rotateTillFree();
  }
}

/**
   Rotate the car at specified degrees with certain speed untill there is no obstacle
*/
void rotateTillFree() {
  int degrees = TURN_RIGHT;
  
  while(obstacleExists()){
    unsigned int initialHeading = car.getHeading();
    bool hasReachedTargetDegrees = false;
    
    while (!hasReachedTargetDegrees) {
      rotateOnSpot(TURN_RIGHT, CAR_SPEED);
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
  car.setSpeed(ZERO);
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
  car.setSpeed(0); //we have reached the target, so stop the car
}
