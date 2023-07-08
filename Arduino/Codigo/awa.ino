//Includes

#include <Servo.h>
#include <SoftwareSerial.h>   // Incluimos la librería  SoftwareSerial 


// Habilitacion de debug para la impresion por el puerto serial ...
//----------------------------------------------
//constantes
#define RXPIN 10 
#define TXPIN 11

SoftwareSerial BT(RXPIN,TXPIN);

#define SERIAL_BAUDS 9600
#define MAX_STATES                    4
#define MAX_EVENTS                    10
#define STATE_FIRST 0
#define MIN_EVENTS 0

#define SERIAL_DEBUG_ENABLED 1

#if SERIAL_DEBUG_ENABLED
  #define DebugPrint(str)\
      {\
        Serial.println(str);\
      }
#else
  #define DebugPrint(str)
#endif

#define DebugPrintEstado(estado,evento)\
      {\
        String est = estado;\
        String evt = evento;\
        String str;\
        str = "-----------------------------------------------------";\
        DebugPrint(str);\
        str = "EST-> [" + est + "]: " + "EVT-> [" + evt + "].";\
        DebugPrint(str);\
        str = "-----------------------------------------------------";\
        DebugPrint(str);\
      }
//----------------------------------------------


//RGB
const int LED_RED_PIN   =    5;
const int LED_BLUE_PIN  =   6;
const int LED_GREEN_PIN =   7;

const int ON = 255;
const int OFF = 0;
const int VIOLET = 159;

//HCSR04
const int DIST_SENSOR_TRIG = 4;
const int DIST_SENSOR_ECHO = 3;



// Rango mapeo de potenciometro simulando el caudalimetro
const int MAX_ANALOG_VALUE = 1023;
const int MIN_ANALOG_VALUE = 0;

const int MAX_FLOW_VALUE  = 100;
const int MIN_FLOW_VALUE  = 0;
const float MIN_WATER_PUMP_FLOW = 0.2;
const float MIN_FLOW_EXPECTED = 1;

const int FLOWMETER_PIN = 2;
const int measureInterval = 2500;
volatile int pulseConter;
 
// YF-S201
const float factorK = 3;

//Sensor de ultrasonido
//velocidad del sonido, en microsegundos por centímetros
const int SOUND_SPEED = 58.2;
//water tank levels in cm
const int MIN_MEDITION_VALUE = 0;
const int MEASUREMENT_ERROR_ALLOWED_CM = 2;

const int LOW_WATER_LEVEL   =  0 ;
const int EXPECTED_WATER_LEVEL   =  2 ;
const int HIGH_WATER_LEVEL   =  10 ;
const int TANK_DEPTH = 12;

const int TIMER_CERO = 0;
const int SOLENOIDE      =    13;
const int WATER_GATE_OPENING_ANGLE = 90;
const int WATER_GATE_CLOSE_ANGLE = 0;
 
const int RELAY_PIN = 12;
const int SERVO_TIMEOUT = 15; // MILLISECONDS
const int TIMEOUT = 50;
const int PUMP_TIMEOUT = 8000; // SECONDS
const int SERVICE_TIMEOUT = 2500; // SECONDS
const int FLOW_TIMEOUT = 2000; // MILLISECONDS
const int TEST_TIMEOUT = 3000; // MILLISECONDS
//------------------------------------------------------------

  // functions
  void state_machine();
  void initialize_sistem();
  void get_new_event();
  void get_new_event_gate_closed(double actual_water_distance, double actual_water_flow, long current_time);
  void get_new_event_gate_opened(double actual_water_distance, double actual_water_flow, long current_time);
  void get_new_event_pressurized_load(double actual_water_distance, double actual_water_flow, long current_time);
  void get_new_event_suspended_load(double actual_water_distance, double actual_water_flow, long current_time);
  float get_water_flow();
  double get_distance_to_the_water();
  void Color(int R, int G, int B);
  void color_timeout(int R, int G, int B);
  void turn_on_green_led();
  void turn_on_yellow_led();
  void turn_on_red_led();
  void turn_on_blue_led();
  void turn_on_white_led();
  void drop_flow_timer();

 typedef void (*transition)();
  
  //Actions
  void none                  ();// ??
  void error();
  void finish_load(); //Finaliza la carga, apaga la bomba y cierra la compuerta
  void start_load(); //Comeinza el cargado del tanque
  void start_presurized_load(); //Comienza el cargado presurizado
  void close_water_gate(); //Cierra la compuerta
  void start_water_pump(); //Prende la bomba
  void stop_water_pump(); //Apaga la bomba
  void show_level(); // Muestra el nivel del tanque
  void handle_service_down(); // Se encarga de apoagar la bomba para que no se queme por falta de agua, e inicializa un timer para testear estado del servicio de agua.
  void start_pump_cooldown(); // Apaga la bomba para que no se queme, cambia el estado a "Carga suspendida" e inicializa un timer para testear el sevicio de agua.
  void handle_hot_pump(); // Bipasea el estado "Carga Presurizada", cuando se quiere ir a este desde el estado "Compuerta Abierta" por "Sin Presion", en el caso que el período de enfriamiento no haya terminado.
  void setup_service_test_timer(); // Inicializa el timer para salir del estado sin servicio a ver si hay agua
  void drop_service_test_timer(); // Cancela el timer para salir del estado sin servicio a ver si hay agua, puesto que se salió por otros medios
  void drop_pump_timer(); // In the case that the pump is being running for only so long, but the caudal gets too low. It drops the heating pump timer
  void setup_pump_timer(); // Inicializa el timer para apagar la bomba después de X segundos de funcionamiento.sto que se salió por otros medios
  void drop_test_timer(); // In the case that the pump is being running for only so long, to test the water supply and there is no supply.
  void setup_test_timer(); // Inicializa el timer para apagar la bomba después de X segundos de funcionamiento para testear flujo.
  void test_service(); // Arranca la bomba a ver si hay presión

//States
enum states          {  ST_GATE_CLOSED  ,  ST_GATE_OPENED ,  ST_PRESSURIZED_LOAD ,  ST_SUSPENDED_LOAD } current_state;
String states_s [] = { "ST_GATE_CLOSED", "ST_GATE_OPENED", "ST_PRESSURIZED_LOAD", "ST_SUSPENDED_LOAD"};
enum states last_state;

//Events
enum events          { 
    EV_CONT  ,  EV_EXPECTED_WATER_LEVEL ,  EV_NO_PRESSURE ,  EV_HOT_PUMP ,  EV_PRESSURE ,  EV_SERVICE_TIMEOUT ,  EV_PUMP_TIMEOUT ,  EV_FLOW_TIMEOUT,  EV_TEST_TIMEOUT,   EV_UNKNOWN
  } new_event;
String events_s [] = { 
    "EV_CONT", "EV_EXPECTED_WATER_LEVEL", "EV_NO_PRESSURE", "EV_HOT_PUMP", "EV_PRESSURE", "EV_SERVICE_TIMEOUT", "EV_PUMP_TIMEOUT", "EV_FLOW_TIMEOUT",  "EV_TEST_TIMEOUT", "EV_UNKNOWN" 
  };
enum events last_event;

transition state_table_actions[MAX_STATES][MAX_EVENTS] =
{
  {show_level, finish_load, start_load             , start_load         , start_load         , error       , drop_pump_timer       , drop_flow_timer,  drop_test_timer, error } , // state ST_GATE_CLOSED
  {none      , finish_load, start_presurized_load  , handle_hot_pump    , none               , error       , drop_pump_timer       , drop_flow_timer,  drop_test_timer, error } , // state ST_GATE_OPENED
  {none      , finish_load, handle_service_down    , handle_service_down, none               , error       , start_pump_cooldown   , drop_flow_timer,  drop_test_timer, error } , // state ST_PRESSURIZED_LOAD
  {none      , finish_load, none                   , none               , start_load         , test_service, start_presurized_load , drop_flow_timer,  drop_test_timer, error } , // state ST_SUSPENDED_LOAD
};

bool timeout;
long past_time;
long service_timer;
long pump_timer;
long servo_timer;
long flow_timer;
long test_timer;
float previous_water_flow = 0;
double previous_water_distance = 0;
//Variables de tiempo de LEDs
long current_time_LED;
int waiting_time = 15;
//Variables de tiempo de bomba de agua
long current_time_PUMP_2;
long current_time_PUMP_10;
int waiting_time_PUMP_2=2;
int waiting_time_PUMP_10=10;
int solenoideActivate = 1;
long timer_solenoide = 0;
long timer_actual;
const int FM_REFRESH_RATE = 2000; // ms
int lastRefreshTime = 0;
int lastFlow = 0;
double actual_water_distance;
//-----------------------------------------------------

void setup()
{
  initialize_sistem();
}

void loop()
{
  state_machine();
}


//-----------------------------------------------------
void state_machine()
{
  get_new_event();

  if( (new_event >= MIN_EVENTS) && (new_event < MAX_EVENTS) && (current_state >= STATE_FIRST) && (current_state < MAX_STATES) )
  {
    if( new_event != EV_CONT )
    {
	    
    }
    
    state_table_actions[current_state][new_event]();
  }
  else
  {
	  
  }
  // Consumo el evento...
  new_event   = EV_CONT;
  last_state = current_state;
}


//-----------------------------------------------------
//Inicializamos los pins
void initialize_sistem()
{
  Serial.begin(SERIAL_BAUDS);

  pinMode(RXPIN, INPUT);
  pinMode(TXPIN, OUTPUT);

  BT.begin(9600);  

  pinMode(LED_RED_PIN     , OUTPUT);
  pinMode(LED_GREEN_PIN   , OUTPUT);
  pinMode(LED_BLUE_PIN    , OUTPUT);
 
  pinMode(DIST_SENSOR_TRIG, OUTPUT);
  pinMode(DIST_SENSOR_ECHO, INPUT);
  
  pinMode(RELAY_PIN       , OUTPUT); 
  pinMode(SOLENOIDE,OUTPUT);
  attachInterrupt(digitalPinToInterrupt(FLOWMETER_PIN), ISRCountPulse, RISING);
  
  timeout = false;
  past_time = millis();
  
  current_state = ST_GATE_CLOSED;
  previous_water_distance = get_distance_to_the_water();
  previous_water_flow = get_water_flow();

}

//-----------------------------------------------------

void get_new_event()
{
  actual_water_distance = TANK_DEPTH - get_distance_to_the_water();
  double actual_water_flow = get_water_flow();
  long current_time = millis();

  check_bluetooth();

  if( actual_water_distance >=  EXPECTED_WATER_LEVEL ) 
  { 
    new_event = EV_EXPECTED_WATER_LEVEL; // EV_EWL
  } else if ( flow_timer != NULL && flow_timer != TIMER_CERO && current_time - flow_timer >= FLOW_TIMEOUT ) {
    new_event = EV_FLOW_TIMEOUT;
  } else if ( pump_timer != NULL && pump_timer != TIMER_CERO && current_time - pump_timer >= PUMP_TIMEOUT) 
  {
    new_event = EV_PUMP_TIMEOUT;
  } else if ( service_timer != NULL && service_timer != TIMER_CERO && current_time - service_timer >= SERVICE_TIMEOUT ) 
  {
    new_event = EV_SERVICE_TIMEOUT;
  } else if ( test_timer != NULL && test_timer != TIMER_CERO && current_time - test_timer >= TEST_TIMEOUT ) 
  {
    new_event = EV_TEST_TIMEOUT;
  } else if (flow_timer != NULL && flow_timer != TIMER_CERO) {
    new_event = EV_CONT;
  } else
  {
    if( actual_water_flow < MIN_FLOW_EXPECTED ) 
    {
      if (pump_timer != NULL && pump_timer != TIMER_CERO) 
      {
        new_event = EV_HOT_PUMP; // EV_LWL_LP_HP
      } else if (test_timer != NULL && test_timer != TIMER_CERO)
      {
        new_event = EV_CONT;
      } else
      {
        new_event = EV_NO_PRESSURE; // EV_LWL_LP
      } 
    } else 
    {
      new_event = EV_PRESSURE;
    }
  }

  previous_water_distance = actual_water_distance;
  previous_water_flow = actual_water_flow;
}

//-----------------------------------------------------
//Read flowmeter(potentiometer) and return value
float get_water_flow()
{
  if( millis() - lastRefreshTime >= FM_REFRESH_RATE ){
    noInterrupts(); // desactivo las interrupciones para poder leer.
    lastFlow = pulseConter * 1000 / FM_REFRESH_RATE;
    lastFlow = lastFlow / factorK ; 
    pulseConter = 0;
    interrupts(); // vuelvo a capturar las interrupciones para contar nuevamente.
    lastRefreshTime = millis();
  }
  else{
    if( millis() - lastRefreshTime >= FM_REFRESH_RATE/3)
    {
    }
  }
  return lastFlow;
}

void ISRCountPulse()
{
   pulseConter++;
}
//-----------------------------------------------------
//-----------------------------------------------------
//Esta bien definir las variables adentro? sacar afuera
double get_distance_to_the_water()
{
  digitalWrite(DIST_SENSOR_TRIG,LOW);
  if(micros() - current_time_PUMP_2 >= waiting_time_PUMP_2)
  {
	digitalWrite(DIST_SENSOR_TRIG,HIGH);
	current_time_PUMP_2=micros();
  }
  
  if(micros() - current_time_PUMP_10 >= waiting_time_PUMP_10)
  {
	digitalWrite(DIST_SENSOR_TRIG,LOW);
	current_time_PUMP_10 = micros();
  }
  
  
  double duracion  = pulseIn(DIST_SENSOR_ECHO,HIGH);
  double distancia = (duracion/SOUND_SPEED);
  return distancia;
  
}
void none();
//-----------------------------------------------------
void show_level()
{
  double actual_water_distance = get_distance_to_the_water();
  
  if (actual_water_distance < LOW_WATER_LEVEL)
  {
	  turn_on_yellow_led();
  } else if (actual_water_distance >= LOW_WATER_LEVEL)
  {
    turn_on_green_led();
  } else {
    turn_on_red_led();
  }
}

//-----------------------------------------------------
//SERVO functions
void open_water_gate()
{
  digitalWrite(SOLENOIDE,HIGH);
  
}
void close_water_gate()
{
  digitalWrite(SOLENOIDE,LOW);
  
}

//-----------------------------------------------------

//WATER PUMP functions
void start_water_pump()
{
  digitalWrite(RELAY_PIN, HIGH);
}
void stop_water_pump()
{
  digitalWrite(RELAY_PIN, LOW);
}

void error()
{
  if (last_state == ST_PRESSURIZED_LOAD) 
  {
    stop_water_pump();
  }
  drop_service_test_timer();
  turn_on_white_led();
  current_state = ST_GATE_CLOSED;
}

void none()
{
} 

//-----------------------------------------------------
void start_load()
{
  if (service_timer != NULL && service_timer != 0) 
  {
    drop_service_test_timer();
  }
  setup_flow_timer();
  turn_on_yellow_led();
  current_state = ST_GATE_OPENED;
}

void finish_load()
{
  // Apago la bomba
  if (last_state == ST_PRESSURIZED_LOAD)
  {
    stop_water_pump();
  }
  // Descarto el timer para chekcear el servicio
  if (service_timer != NULL && service_timer != 0) 
  {
    drop_service_test_timer();
  }
  close_water_gate();
  turn_on_green_led();
  current_state = ST_GATE_CLOSED;
}
//-----------------------------------------------------
void start_presurized_load()
{
  digitalWrite(SOLENOIDE,HIGH);
  start_water_pump();
  setup_flow_timer();
  setup_pump_timer();
  turn_on_yellow_led();
  current_state = ST_PRESSURIZED_LOAD;
}

void start_pump_cooldown()
{
  stop_water_pump();
  setup_flow_timer();
  setup_pump_timer();
  turn_on_blue_led();
  current_state = ST_SUSPENDED_LOAD;
}

void handle_hot_pump()
{
  setup_flow_timer();
  turn_on_blue_led();
  current_state = ST_SUSPENDED_LOAD;
}

void handle_service_down()
{
  stop_water_pump();
  drop_pump_timer();
  setup_flow_timer();
  setup_service_test_timer();
  turn_on_red_led();
  current_state = ST_SUSPENDED_LOAD;
}

void test_service()
{
  start_water_pump();
  drop_service_test_timer();
  setup_flow_timer();
  setup_test_timer();
  turn_on_yellow_led();
  current_state = ST_PRESSURIZED_LOAD;
}

void drop_pump_timer()
{
  pump_timer = NULL;
}

void drop_service_test_timer() 
{
  service_timer = NULL;
}

void setup_service_test_timer() 
{ 
  service_timer = millis();
}

void setup_pump_timer()
{
  pump_timer = millis();
}

void drop_flow_timer() 
{
  flow_timer = NULL;
}

void setup_flow_timer() 
{
  flow_timer = millis();
}

void drop_test_timer() 
{
  int flow = get_water_flow();
  if (flow > MIN_FLOW_EXPECTED)
  {
    start_presurized_load();
  }
  else
  {
    handle_service_down();
  }
  test_timer = NULL;
}

void setup_test_timer() 
{
  test_timer = millis();
}

//-----------------------------------------------------
//LEDS
void turn_on_green_led() // Nivel de agua "esperado"
{
  color_timeout( OFF, ON, OFF);
}
void turn_on_yellow_led() // Nivel de agua "bajo"
{
  color_timeout( ON, ON, OFF);
}
void turn_on_red_led() // Sin suministro, creo
{
  color_timeout( ON, OFF, OFF);
}
void turn_on_blue_led() // Bomba, en enfriamiento
{
  color_timeout( OFF, OFF, ON);
}
void turn_on_white_led() // Algo dió error
{
  color_timeout( ON, ON, ON);
}

//-----------------------------------------------------

void color_timeout(int R, int G, int B)
{
  
  if(millis() - current_time_LED >= waiting_time)
  {
    Color(R,G,B);
    current_state = ST_GATE_CLOSED;
    current_time_LED = millis();
  }
}

//-----------------------------------------------------
//RGB functions
void Color(int R, int G, int B)
{     
     analogWrite(LED_RED_PIN  , R) ;    // Red  
     analogWrite(LED_GREEN_PIN, G) ;    // Green
     analogWrite(LED_BLUE_PIN , B) ;    // Blue 
}

void check_bluetooth(){

  if(BT.available())    // Si llega un dato por el puerto BT se envía al monitor serial
  {
    char message = BT.read();
    if (message == 'A')
    {
      Serial.print(message);
      Color(VIOLET,OFF,ON);//violeta, el usuario ya recibio la informacion
    }
    if (message == 'B')
    {
      char buffer[10] = "";
      BT.println(dtostrf(actual_water_distance, 5, 3, buffer));
    }
  }
}
