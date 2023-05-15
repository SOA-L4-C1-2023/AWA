//Includes

#include <Servo.h>


// Habilitacion de debug para la impresion por el puerto serial ...
//----------------------------------------------
//constantes
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

//HCSR04
const int DIST_SENSOR_TRIG = 2;
const int DIST_SENSOR_ECHO = 3;

//POTENTIOMETER
const int POTENTIOMETER    = 1;

// Rango mapeo de potenciometro simulando el caudalimetro
const int MAX_ANALOG_VALUE = 1023;
const int MIN_ANALOG_VALUE = 0;

const int MAX_FLOW_VALUE  = 100;
const int MIN_FLOW_VALUE  = 0;
const int MIN_WATER_PUMP_FLOW = 3;
const int MIN_FLOW_EXPECTED = 30;

//Sensor de ultrasonido
//velocidad del sonido, en microsegundos por centímetros
const int SOUND_SPEED = 58.2;
//water tank levels in cm
const int MIN_MEDITION_VALUE = 0;
const int MEASUREMENT_ERROR_ALLOWED_CM = 2;

const int LOW_WATER_LEVEL   =  100 ;
const int EXPECTED_WATER_LEVEL   =  250 ;
const int HIGH_WATER_LEVEL   =  290 ;
const int TANK_DEPTH = 300;

const int TIMER_CERO = 0;

const int SERVO_1_PIN      =    8 ;
const int WATER_GATE_OPENING_ANGLE = 90;
const int WATER_GATE_CLOSE_ANGLE = 0;
 
const int RELAY_PIN = 12;
const int SERVO_TIMEOUT = 15; // MILLISECONDS
const int TIMEOUT = 50;
const int PUMP_TIMEOUT = 6000; // SECONDS
const int SERVICE_TIMEOUT = 2500; // SECONDS
//-----------

Servo servo_1;
//------------------------------------------------------------

  // functions
  void state_machine();
  void initialize_sistem();
  void get_new_event();
  void get_new_event_gate_closed(double actual_water_distance, double actual_water_flow, long current_time);
  void get_new_event_gate_opened(double actual_water_distance, double actual_water_flow, long current_time);
  void get_new_event_pressurized_load(double actual_water_distance, double actual_water_flow, long current_time);
  void get_new_event_suspended_load(double actual_water_distance, double actual_water_flow, long current_time);
  int get_water_flow();
  double get_distance_to_the_water();
  void Color(int R, int G, int B);
  void color_timeout(int R, int G, int B);
  void turn_on_green_led();
  void turn_on_yellow_led();
  void turn_on_red_led();
  void turn_on_blue_led();
  void turn_on_white_led();

 typedef void (*transition)();
  
  //Actions
  void reset                  ();// ??
  void error();
  void finish_load(); //Finaliza la carga, apaga la bomba y cierra la compuerta
  void start_load(); //Comeinza el cargado del tanque
  void start_presurized_load(); //Comienza el cargado presurizado
  void close_water_gate(); //Cierra la compuerta
  void start_water_pump(); //Prende la bomba
  void stop_water_pump(); //Apaga la bomba
  void show_level(); // Muestra el nivel del tanque
  void handle_service_down(); // Se encarga de apoagar la bomba para que no se queme por falta de agua, e inicializa un timer para testear estado del servicio de agua.
  void handle_service_up(); // Descarta el timer de testeo de estado del servicio de agua y cambia el estado a "Compuerta Abierta"
  void start_pump_cooldown(); // Apaga la bomba para que no se queme, cambia el estado a "Carga suspendida" e inicializa un timer para testear el sevicio de agua.
  void handle_hot_pump(); // Bipasea el estado "Carga Presurizada", cuando se quiere ir a este desde el estado "Compuerta Abierta" por "Sin Presion", en el caso que el período de enfriamiento no haya terminado.
  void setup_service_test_timer(); // Inicializa el timer para salir del estado sin servicio a ver si hay agua
  void drop_service_test_timer(); // Cancela el timer para salir del estado sin servicio a ver si hay agua, puesto que se salió por otros medios
  void drop_pump_timer(); // In the case that the pump is being running for only so long, but the caudal gets too low. It drops the heating pump timer
  void setup_pump_timer(); // Inicializa el timer para apagar la bomba después de X segundos de funcionamiento.
  void test_service(); // Arranca la bomba a ver si hay presión

//States
enum states          {  ST_GATE_CLOSED  ,  ST_GATE_OPENED ,  ST_PRESSURIZED_LOAD ,  ST_SUSPENDED_LOAD } current_state;
String states_s [] = { "ST_GATE_CLOSED", "ST_GATE_OPENED", "ST_PRESSURIZED_LOAD", "ST_SUSPENDED_LOAD"};
enum states last_state;

//Events
enum events          { 
    EV_CONT, EV_EXPECTED_WATER_LEVEL, EV_LOW_WATER_LEVEL, EV_NO_PRESSURE, EV_SERVICE_UP, 
    EV_HOT_PUMP, EV_SERVICE_DOWN, EV_SERVICE_TIMEOUT, EV_PUMP_TIMEOUT,EV_UNK 
  } new_event;
String events_s [] = { 
    "EV_CONT", "EV_EXPECTED_WATER_LEVEL" , "EV_LOW_WATER_LEVEL","EV_NO_PRESSURE", "EV_SERVICE_UP", 
    "EV_HOT_PUMP", "EV_SERVICE_DOWN", "EV_SERVICE_TIMEOUT", "EV_PUMP_TIMEOUT", "EV_UNKNOW" 
  };
enum events last_event;

transition state_table_actions[MAX_STATES][MAX_EVENTS] =
{
  {show_level, none      , start_load, error                 , error            , error          , error              , error,        error              ,       error } , // state ST_GATE_CLOSED
  {none      , finish_load, error     , start_presurized_load , error            , handle_hot_pump, error              , error,        error              ,       error } , // state ST_GATE_OPENED
  {none      , finish_load, error     , error                 , error            , error          , handle_service_down, error,        start_pump_cooldown,       error } , // state ST_PRESSURIZED_LOAD
  {none      , error      , error     , error                 , handle_service_up, error          , error              , test_service, start_presurized_load,       error } , // state ST_SUSPENDED_LOAD
};

bool timeout;
long past_time;
long service_timer;
long pump_timer;
long servo_timer;
int previous_water_flow = 0;
double previous_water_distance = 0;
//Variables de tiempo de LEDs
long current_time_LED;
int waiting_time = 15;
//Variables de tiempo de bomba de agua
long current_time_PUMP_2;
long current_time_PUMP_10;
int waiting_time_PUMP_2=2;
int waiting_time_PUMP_10=10;
//-----------------------------------------------------

void setup()
{
  initialize_sistem();
  DebugPrintEstado(states_s[current_state], events_s[new_event]);
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
      DebugPrintEstado(states_s[current_state], events_s[new_event]);
    }
    
    state_table_actions[current_state][new_event]();
  }
  else
  {
    DebugPrintEstado(states_s[ST_SUSPENDED_LOAD], events_s[EV_UNK]);
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
  
  pinMode(LED_RED_PIN     , OUTPUT);
  pinMode(LED_GREEN_PIN   , OUTPUT);
  pinMode(LED_BLUE_PIN    , OUTPUT);
 
  pinMode(DIST_SENSOR_TRIG, OUTPUT);
  pinMode(DIST_SENSOR_ECHO, INPUT);
  
  pinMode(RELAY_PIN       , OUTPUT); 

  servo_1.attach(SERVO_1_PIN);
  servo_1.write(WATER_GATE_CLOSE_ANGLE);
  
  timeout = false;
  past_time = millis();
  
  
  current_state = ST_GATE_CLOSED;
  previous_water_distance = get_distance_to_the_water();
  previous_water_flow = get_water_flow();

}

//-----------------------------------------------------

void get_new_event()
{
  DebugPrint("Get new event");
  double actual_water_distance = get_distance_to_the_water();
  double actual_water_flow = get_water_flow();
  long current_time = millis();
  switch(last_state) {
    case ST_GATE_CLOSED:
      get_new_event_gate_closed(actual_water_distance, actual_water_flow, current_time);
      break;
    case ST_GATE_OPENED:
      get_new_event_gate_opened(actual_water_distance, actual_water_flow, current_time);
      break;
    case ST_PRESSURIZED_LOAD:
      get_new_event_pressurized_load(actual_water_distance, actual_water_flow, current_time);
      break;
    case ST_SUSPENDED_LOAD:
      get_new_event_suspended_load(actual_water_distance, actual_water_flow, current_time);
      break;
  }
  previous_water_distance = actual_water_distance;
  previous_water_flow = actual_water_flow;
}

void get_new_event_gate_closed(double actual_water_distance, double actual_water_flow, long current_time)
{
  if(actual_water_distance < LOW_WATER_LEVEL )
  { 
    new_event = EV_LOW_WATER_LEVEL;
  } else if(actual_water_distance >=  EXPECTED_WATER_LEVEL) 
  { 
    new_event = EV_EXPECTED_WATER_LEVEL;
  } else {
    new_event = EV_CONT;
  }
}

void get_new_event_gate_opened(double actual_water_distance, double actual_water_flow, long current_time)
{
  if(actual_water_distance >= EXPECTED_WATER_LEVEL) 
  { 
    new_event = EV_EXPECTED_WATER_LEVEL;
  } else if(actual_water_flow < MIN_FLOW_EXPECTED)
  { 
    if (pump_timer != NULL && pump_timer != TIMER_CERO)
    {
      new_event = EV_HOT_PUMP;
    } else {
      new_event = EV_NO_PRESSURE;
    }
  } else {
    new_event = EV_CONT;
  }
}

void get_new_event_pressurized_load(double actual_water_distance, double actual_water_flow, long current_time)
{
  if(actual_water_distance >= EXPECTED_WATER_LEVEL) 
  { 
    new_event = EV_EXPECTED_WATER_LEVEL;
  } else if ( pump_timer != NULL && pump_timer != TIMER_CERO && current_time - pump_timer >= PUMP_TIMEOUT) 
  {
    new_event = EV_PUMP_TIMEOUT;
  } else if(actual_water_flow < MIN_WATER_PUMP_FLOW)
  {
    new_event = EV_SERVICE_DOWN;
  } else {
    new_event = EV_CONT;
  }
}

void get_new_event_suspended_load(double actual_water_distance, double actual_water_flow, long current_time)
{
  if (actual_water_flow > MIN_FLOW_EXPECTED)
  {
    new_event = EV_SERVICE_UP;
  } else if ( pump_timer != NULL && pump_timer != TIMER_CERO && current_time - pump_timer >= PUMP_TIMEOUT) 
  {
    new_event = EV_PUMP_TIMEOUT;
  } else if ( pump_timer == NULL && service_timer != NULL && service_timer != TIMER_CERO && current_time - service_timer >= SERVICE_TIMEOUT) 
  {
    new_event = EV_SERVICE_TIMEOUT;
  } else {
    new_event = EV_CONT;
  }
}

//-----------------------------------------------------
//Read flowmeter(potentiometer) and return value
int get_water_flow()
{
  int waterFlowValue = analogRead(POTENTIOMETER);
  return map(waterFlowValue, MIN_ANALOG_VALUE, MAX_ANALOG_VALUE, MIN_FLOW_VALUE, MAX_FLOW_VALUE);
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
  double distancia = TANK_DEPTH - (duracion/SOUND_SPEED);
  
  return distancia;
  
}

//-----------------------------------------------------
void show_level()
{
  DebugPrint("Show level");
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
  servo_1.write(WATER_GATE_OPENING_ANGLE);
  
}
void close_water_gate()
{
  servo_1.write(WATER_GATE_CLOSE_ANGLE);
  
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
  DebugPrintEstado(states_s[current_state], events_s[new_event]);
  DebugPrint("Something blew up!");
  if (last_state == ST_PRESSURIZED_LOAD) 
  {
    stop_water_pump();
  }
  drop_service_test_timer();
  close_water_gate();
  turn_on_white_led();
  current_state = ST_GATE_CLOSED;
}

void none()
{
} 

//-----------------------------------------------------
void start_load()
{
  DebugPrint("Start Load");
  open_water_gate();
	turn_on_yellow_led();
	current_state = ST_GATE_OPENED;
}

void finish_load()
{
  DebugPrint("Finish load");
  if (last_state == ST_PRESSURIZED_LOAD)
  {
    stop_water_pump();
  }
  close_water_gate();
	turn_on_green_led();
	current_state = ST_GATE_CLOSED;
}
//-----------------------------------------------------
void start_presurized_load()
{
  DebugPrint("Start presurized load");
  start_water_pump();
  setup_pump_timer();
  turn_on_yellow_led();
  current_state = ST_PRESSURIZED_LOAD;
}

void start_pump_cooldown()
{
  DebugPrint("Start water pump cooldown");
  stop_water_pump();
  setup_pump_timer();
  turn_on_blue_led();
  current_state = ST_SUSPENDED_LOAD;
}

void handle_hot_pump()
{
  DebugPrint("Handle hot pump");
  turn_on_blue_led();
  current_state = ST_SUSPENDED_LOAD;
}

void handle_service_down()
{
  DebugPrint("Handle service down");
  stop_water_pump();
  drop_pump_timer();
  setup_service_test_timer();
  turn_on_red_led();
  current_state = ST_SUSPENDED_LOAD;
}

void handle_service_up()
{
  DebugPrint("Handle service up");
  drop_service_test_timer();
  turn_on_yellow_led();
  current_state = ST_GATE_OPENED;
}

void test_service()
{
  DebugPrint("Handle test servie");
  drop_service_test_timer();
  turn_on_yellow_led();
  current_state = ST_PRESSURIZED_LOAD;
}

void drop_pump_timer()
{
  DebugPrint("Drop pump timer");
  pump_timer = NULL;
}

void drop_service_test_timer() 
{
  DebugPrint("Drop service test timer");
  service_timer = NULL;
}

void setup_service_test_timer() 
{
  DebugPrint("Setup servcice test timer");
  service_timer = millis();
}

void setup_pump_timer()
{
  DebugPrint("Set up pump timer");
  pump_timer = millis();
}

//-----------------------------------------------------
//LEDS
void turn_on_green_led() // Nivel de agua "esperado"
{
  color_timeout( LOW, HIGH, LOW);
}
void turn_on_yellow_led() // Nivel de agua "bajo"
{
  color_timeout( HIGH, HIGH, LOW);
}
void turn_on_red_led() // Sin suministro, creo
{
  color_timeout( HIGH, LOW, LOW);
}
void turn_on_blue_led() // Bomba, en enfriamiento
{
  color_timeout( LOW, LOW, HIGH);
}
void turn_on_white_led() // Algo dió error
{
  color_timeout( HIGH, HIGH, HIGH);
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