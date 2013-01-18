#define PIN_LED 13
#define PIN_SWITCH 8

#define ON 1

void setup() {
  Serial.begin(115200);
  pinMode(PIN_LED, OUTPUT);
  pinMode(PIN_SWITCH, INPUT);
}

boolean on = false;
int prevState = 0;
int light = 0;
int p = 1;

void loop() {
  int state = digitalRead(PIN_SWITCH);
  if (state == ON && state != prevState) {
    on = !on;
    light = 0;
    p = 1;
    Serial.println("State change.");
  }
  prevState = state;
  
  if (on == true) {
    light += p;
    if (light == 0 || light == 255) {
      p *= -1;
    }
    delay(10);
  }
  analogWrite(PIN_LED, light);
}

