#include <Smartcar.h>

const unsigned short LEFT_ODOMETER_PIN = 2;
const unsigned short RIGHT_ODOMETER_PIN = 3;
const int TRIGGER_PIN = 6; //D6
const int ECHO_PIN = 7; //D7

const float CAR_SPEED = 40.0;
const float SLOWER_SPEED = 20.0;
const int TURN_RIGHT = 10;
const int TURN_LEFT = -10;
const int ANGLE_CORRECTION = 13;
const int GYROSCOPE_OFFSET = 37;
const int BAUD_RATE = 9600;
const int PULSES_PER_METER = 100;
const int FULL_CIRCLE = 360; 
const int ZERO = 0;
const int EVEN = 2;

const unsigned int MAX_DISTANCE = 100;
const unsigned int MIN_DISTANCE = 20;
const unsigned int SHORT_DISTANCE = 10;

//bool noObstacle = true;
int obstacleCounter = ZERO;

BrushedMotor leftMotor(8, 10, 9);
BrushedMotor rightMotor(12, 13, 11);
DifferentialControl control(leftMotor, rightMotor);

SR04 frontSensor(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE);
GY50 gyroscope(GYROSCOPE_OFFSET);
DirectionlessOdometer leftOdometer(PULSES_PER_METER);
DirectionlessOdometer rightOdometer(PULSES_PER_METER);

SmartCar car(control, gyroscope, leftOdometer, rightOdometer);

void setup() {
  Serial.begin(9600);
  car.setAngle(ANGLE_CORRECTION);

  leftOdometer.attach(LEFT_ODOMETER_PIN, []() {
    leftOdometer.update();
  });
  rightOdometer.attach(RIGHT_ODOMETER_PIN, []() {
    rightOdometer.update();
  });

  car.enableCruiseControl();
}

void loop() {
  car.setSpeed(CAR_SPEED);
if(!isThereObstacle()){
  car.setSpeed(CAR_SPEED);
} else {
  if(obstacleCounter %2 == ZERO){
    car.setSpeed(-20);
    while (isThereObstacle()){
      rotate(TURN_RIGHT, SLOWER_SPEED);
    }
    //car.setSpeed(CAR_SPEED);
    //car.update();
  } else {
      car.setSpeed(-20);
      while (isThereObstacle()){
        rotate(TURN_LEFT, SLOWER_SPEED);
      }
      //car.setSpeed(CAR_SPEED);
      //car.update();
  }
}
}

bool isThereObstacle(){
  int distanceToObstacle = getObstacleDistance();

  if(distanceToObstacle < MIN_DISTANCE && distanceToObstacle > ZERO){
    obstacleCounter++;
    return true;
  } else {
    return false;
  }
}

int getObstacleDistance(){
  return frontSensor.getDistance();
}

/**
   Rotate the car at the specified degrees with the certain speed
   @param degrees   The degrees to turn. Positive values for clockwise
                    negative for counter-clockwise.
   @param speed     The speed to turn
*/
void rotate(int degrees, float speed) {
  
  speed = smartcarlib::utils::getAbsolute(speed);
  degrees %= 360; // Put degrees in a (-360,360) scale
  if (degrees == ZERO) {
    return;
  }

  car.setSpeed(speed);
  
  if (degrees > ZERO) {
    car.setAngle((TURN_RIGHT + ANGLE_CORRECTION));
  } else {
    car.setAngle((TURN_LEFT + ANGLE_CORRECTION));
  }

  unsigned int initialHeading = car.getHeading();
  bool hasReachedTargetDegrees = false;
  
  while (!hasReachedTargetDegrees) {
    car.update();
    int currentHeading = car.getHeading();
    if (degrees < ZERO && currentHeading > initialHeading) {
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

  car.setSpeed(ZERO);
}



/**
   Makes the car travel at the specified distance with a certain speed
   @param centimeters   How far to travel in centimeters, positive for
                        forward and negative values for backward
   @param speed         The speed to travel
*/
void goShort(long centimeters, float speed) {
  if (centimeters == ZERO) {
    return;
  }

  int distanceToObstacle = getObstacleDistance();
  if(distanceToObstacle > 20){ //if not blocked,
   // Ensure the speed is towards the correct direction
    speed = smartcarlib::utils::getAbsolute(speed) * ((centimeters < ZERO) ? -1 : 1);
    car.setAngle(13);
    car.setSpeed(speed);

    long initialDistance = car.getDistance();
    bool hasReachedTargetDistance = false;

    //unless the target distance is reached or there is an obstacle
    while (!hasReachedTargetDistance || distanceToObstacle > 20) 
    {
      //car.update();
      distanceToObstacle = getObstacleDistance();
      
      auto currentDistance = car.getDistance();
      auto travelledDistance = initialDistance > currentDistance ? initialDistance - currentDistance : currentDistance - initialDistance;
      hasReachedTargetDistance = travelledDistance >= smartcarlib::utils::getAbsolute(centimeters);
    }
  car.setSpeed(ZERO);
  } else {
    car.setSpeed(-30);
    }
}

  
