#define PIN_LED 13
#define PIN_CDS A0
#define PIN_TERM A1

#define ON 1
#define LIGHT_THRESHOLD 700
#define TERM_THRESHOLD 500

void setup() {
  Serial.begin(115200);
  pinMode(PIN_LED, OUTPUT);
  pinMode(PIN_CDS, INPUT);
  pinMode(PIN_TERM, INPUT);
}

boolean on = false;
int light = 0;
int p = 1;

void loop() {
  int cdsState = analogRead(PIN_CDS);
  int termState = analogRead(PIN_TERM);
  Serial.println(cdsState);
  boolean flg = (cdsState < LIGHT_THRESHOLD) && (termState < TERM_THRESHOLD);
  if (!flg && on || flg && !on) {
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

