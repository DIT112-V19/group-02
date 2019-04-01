#include <Smartcar.h>

BrushedMotor leftMotor(8, 10, 9);
BrushedMotor rightMotor(12, 13, 11);
DifferentialControl control(leftMotor, rightMotor);

const int TRIGGER_PIN = 6; //D6
const int ECHO_PIN = 7; //D7
const unsigned int MAX_DISTANCE = 30;
SR04 front(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE);
bool obstacle = false;

SimpleCar car(control);

void setup() {
  car.setSpeed(50);
  car.setAngle(13);
}

void loop() {
  if (obstacle == false) {
    int currDistance = front.getDistance();
    if (currDistance < MAX_DISTANCE && currDistance > 0) {
      obstacle = true;
      car.setSpeed(0);
    }
  }
}
