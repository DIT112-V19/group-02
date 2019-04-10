#include <Smartcar.h>

BrushedMotor leftMotor(8, 10, 9);
BrushedMotor rightMotor(12, 13, 11);
DifferentialControl control(leftMotor, rightMotor);

SimpleCar car(control);

const int TRIGGER_PIN = 6; //D6
const int ECHO_PIN = 7; //D7
const unsigned int MAX_DISTANCE = 30;
SR04 front(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE);
bool signal = true;

void setup() {
  Serial.begin(9600);
}

void loop() {
  
if(signal){
  car.setSpeed(50);
}

int x = front.getDistance();

if(x<20 && x>0){
  car.setSpeed(0);
  signal = false;
} else {
  signal = true;
}

}
