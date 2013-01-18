#include <Usb.h>
#include <AndroidAccessory.h>

#define PIN_LED 13

AndroidAccessory acc("C-LIS CO., LTD.",
                     "ADK Test",
                     "This is an ADK test?",
                     "1.0.0",
                     "http://www.c-lis.co.jp",
                     "1234567890");

void setup() {
  pinMode(PIN_LED, OUTPUT);
  acc.powerOn();
}

void loop() {
  byte receive_msg[1];
  
  if (acc.isConnected()) {
    int len = acc.read(receive_msg, sizeof(receive_msg), 1);
    if (len > 0) {
      if(receive_msg[0] == 1) {
        digitalWrite(PIN_LED, HIGH);
      } else {
        digitalWrite(PIN_LED, LOW);
      }
    }
  }
}

