#define PIN_LED 13
#define PIN_CDS A0

#define ON 1
#define LIGHT_THRESHOLD 700

void setup() {
  Serial.begin(115200);
  pinMode(PIN_LED, OUTPUT);
  pinMode(PIN_CDS, INPUT);
}

boolean on = false;
int light = 0;
int p = 1;

void loop() {
  int state = analogRead(PIN_CDS);
  Serial.println(state);
  if (state > LIGHT_THRESHOLD && on
    || state < LIGHT_THRESHOLD && !on) {
      on = !on;
      light = 0;
      p = 1;
  }  
  if (on == true) {
    light += p;
    if (light == 0 || light == 255) {
      p *= -1;
    }
    delay(10);
  }
  analogWrite(PIN_LED, light);
}

