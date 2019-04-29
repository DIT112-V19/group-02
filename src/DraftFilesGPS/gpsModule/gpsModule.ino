//#include <LiquidCrystal.h> 
#include <SoftwareSerial.h> 
#include <TinyGPS.h> 
float lat = 28.5458, lon = 77.1703; // create variable for latitude and longitude object  (lat = 28.5458, lon = 77.1703)
SoftwareSerial gpsSerial(18,19);//rx,tx 
//LiquidCrystal lcd(A0,A1,A2,A3,A4,A5); 
TinyGPS gps; // create gps object 
void setup(){ 
Serial.begin(9600); // connect serial 
//Serial.println("The GPS Received Signal:"); 
gpsSerial.begin(9600); // connect gps sensor 
//lcd.begin(16,2); 
} 
void loop(){ 
  //Serial1.println(gpsSerial.read());
  while(gpsSerial.available()){ // check for gps data 
    //Serial1.println(gpsSerial.read());
    if(gps.encode(gpsSerial.read()))// encode gps data 
      {  
      gps.f_get_position(&lat, &lon); // get latitude and longitude 
      // display position 
      //lcd.clear(); 
      //lcd.setCursor(1,0); 
      //lcd.print("GPS Signal"); 
      //Serial.print("Position: "); 
      //Serial.print("Latitude:"); 
      //Serial.print(lat,6); 
      //Serial.print(";"); 
      //Serial.print("Longitude:"); 
      //Serial.println(lon,6);  
      //lcd.setCursor(1,0); 
      //lcd.print("LAT:"); 
      //lcd.setCursor(5,0); 
      //lcd.print(lat); 
      //Serial.print(lat); 
      //Serial.print(" "); 
      //lcd.setCursor(0,1); 
      //lcd.print(",LON:"); 
      //lcd.setCursor(5,1); 
      //lcd.print(lon); 
      } 
      //delay(1000); 
  } 
  String latitude = String(lat, 6); 
  String longitude = String(lon, 6); 
  Serial.println(latitude + "; " + longitude); 
  delay(1000); 
} 
