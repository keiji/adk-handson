#include <Usb.h>
#include <AndroidAccessory.h>

#define PIN_LED 13
#define PIN_CDS A0

#define ON 1
#define LIGHT_THRESHOLD 700

AndroidAccessory acc("C-LIS CO., LTD.",
                     "ADK Test",
                     "This is an ADK test?",
                     "1.0.0",
                     "http://www.c-lis.co.jp",
                     "1234567890");

void setup() {
  Serial.begin(115200);
  pinMode(PIN_LED, OUTPUT);
  pinMode(PIN_CDS, INPUT);
  acc.powerOn();
}

boolean init_flg = false;
boolean on = false;
int light = 0;
int p = 1;

void loop() {
  int state = analogRead(PIN_CDS);
  if (acc.isConnected()) {
    if (!init_flg) {
        analogWrite(PIN_LED, LOW);
        init_flg = true;
    }
    byte receive_msg[1];
    byte send_msg[1];
    send_msg[0] = state / 4;
    acc.write(send_msg, sizeof(send_msg));
    
    int len = acc.read(receive_msg, sizeof(receive_msg), 1);
    if (len > 0) {
      if(receive_msg[0] == 1) {
        analogWrite(PIN_LED, 255);
      } else {
        analogWrite(PIN_LED, 0);
      }
    }
  } else {
    init_flg = false;
    Serial.println(state);
    if (state > LIGHT_THRESHOLD && on
      || state < LIGHT_THRESHOLD && !on) {
        on = !on;
        light = 0;
        p = 1;
    }  
    if (on) {
      light += p;
      if (light == 0 || light == 255) {
        p *= -1;
      }
      delay(10);
    }
    analogWrite(PIN_LED, light);
  }
}

