
#include <Smartcar.h>

const int fSpeed = 70; //70% of the full speed forward
const int bSpeed = -70; //70% of the full speed backward
const int lDegrees = -75; //degrees to turn left
const int rDegrees = 75; //degrees to turn right

BrushedMotor leftMotor(8, 10, 9);
BrushedMotor rightMotor(12, 13, 11);
DifferentialControl control(leftMotor, rightMotor);

SimpleCar car(control);

void setup() {
  
  Serial3.begin(9600);
  pinMode(14,INPUT);
  pinMode(15,OUTPUT);
}

void loop() {
  
    char input = Serial3.read();

    if (input == '0')
  {
    car.setSpeed(fSpeed);
    car.setAngle(0);
  } 
  else if (input == '1')
  {
    car.setSpeed(bSpeed);
    car.setAngle(0);
  } 
  else if (input == '2')
  {
    car.setSpeed(fSpeed);
    car.setAngle(lDegrees);
  }
  else if (input == '3')
  {
    car.setSpeed(fSpeed);
    car.setAngle(rDegrees);
  }
  else if (input == '9')
  {
    car.setSpeed(0);
    car.setAngle(0);
  }
}
