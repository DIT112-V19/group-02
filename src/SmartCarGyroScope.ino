#include <Smartcar.h>

BrushedMotor leftMotor(8, 10, 9);
BrushedMotor rightMotor(12, 13, 11);
DifferentialControl control(leftMotor, rightMotor);
const int TRIGGER_PIN = 6; //D6
const int ECHO_PIN = 7; //D7
const unsigned int READ_DISTANCE = 20;
SR04 front(TRIGGER_PIN, ECHO_PIN, READ_DISTANCE);

GY50 gyro(-10);
float currAngle = 0;
SimpleCar car(control);

void setup() {
  // Move forward for 3 seconds
  Serial.begin(9600);
  Serial.println("Calibrating gyroscope, this might take some seconds");
  int offset = gyro.getOffset();
  Serial.print("This gyro's offset value is: ");
  Serial.println(offset);
  Serial.print("Please initialize Gyroscope with the above value as: GY50 gyro(");
  Serial.print(offset);
  Serial.println("); or another similar value that works better according to your experimentation.");
  }
  
void loop() {

  // this is just to test the gyroscope
  gyro.update();
  Serial.println(gyro.getHeading());
  currAngle = gyro.getHeading();
  Serial.println(currAngle);

   //if the heading is over 180 the car should stop
  if(currAngle > 180){
    car.setSpeed(0);
  }else{
    car.setSpeed(50);
  }

}
