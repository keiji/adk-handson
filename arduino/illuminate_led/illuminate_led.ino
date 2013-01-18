#define PIN_LED 13

void setup() {
  Serial.begin(115200);
  pinMode(PIN_LED, OUTPUT);
}

int light = 0;
int p = 1;

void loop() {
  light += p;
  if (light == 0 || light == 255) {
    p *= -1;
  }
  delay(10);
  analogWrite(PIN_LED, light);
}

