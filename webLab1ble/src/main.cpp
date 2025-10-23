
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define SERVICE_UUID              "9a8ca9ef-e43f-4157-9fee-c37a3d7dc12d" // ID сервиса
#define ELEM_UUID              "cc46b944-003e-42b6-b836-c4246b8f19a0" // ID характеристики
#define FIRE_ELEM_UUID              "c8f25217-425a-4285-8e75-6d92ce9f656a" // ID характеристики

#define DEVINFO_UUID              (uint16_t)0x180a
#define DEVINFO_MANUFACTURER_UUID (uint16_t)0x2a29
#define DEVINFO_NAME_UUID         (uint16_t)0x2a24
#define DEVINFO_SERIAL_UUID       (uint16_t)0x2a25

#define DEVICE_NAME         "furDISPLAY" // Имя устройства


#define SCREEN_WIDTH 128 // OLED display width, in pixels
#define SCREEN_HEIGHT 64 // OLED display height, in pixels

// Declaration for an SSD1306 display connected to I2C (SDA, SCL pins)
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);

class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    // Обработка подключения телефона к устройству
  display.clearDisplay();
    display.println("connected");
  display.display(); 
  };

  void onDisconnect(BLEServer* pServer) {
    // Обработка отключения
  display.clearDisplay();
    display.println("disconnected(((");
  display.display(); 
  }
};

class ElemCallbacs : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
      std::string value1 = pCharacteristic->getValue();
      display.clearDisplay();
  display.print("value: " );
  display.println(value1.c_str());
display.display(); 
  }
};

class FireCallbacs : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *pCharacteristic) {
    std::string value1 = pCharacteristic->getValue();
    if(value1 == "yes"){
      display.clearDisplay();
      display.println("YOU ARE GOOD FIRING!");
      display.display(); 

    } else {
      display.clearDisplay();
      display.println("Try again((");
      display.display(); 

    }
      
  }
};

void setup() {
  Serial.begin(115200);

  if(!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) { // Address 0x3D for 128x64
    Serial.println(F("SSD1306 allocation failed"));
    for(;;);
  }
  delay(2000);
  display.clearDisplay();

  display.setTextSize(1);
  display.setTextColor(WHITE);
  display.setCursor(0, 10);
  // Display static text
  display.println("Hello, world!");
  display.display(); 

  String devName = DEVICE_NAME; 
  String chipId = String((uint32_t)(ESP.getEfuseMac() >> 24), HEX);
  devName += '_';
  devName += chipId;

  BLEDevice::init(devName.c_str());  // Инициализация девайса 
  BLEServer *pServer = BLEDevice::createServer(); // Создание сервера
  pServer->setCallbacks(new MyServerCallbacks()); // Подключение Callback-а


  BLEService *pService = pServer->createService(SERVICE_UUID); // Cоздание сервиса

  BLECharacteristic *pElem;


  pElem = pService->createCharacteristic(FIRE_ELEM_UUID, BLECharacteristic::PROPERTY_WRITE);
  pElem->setCallbacks(new FireCallbacs());

  BLEDescriptor *pDescriptor = new BLEDescriptor(BLEUUID((uint16_t)0x2901));


  pDescriptor->setValue("Fire-result"); 

  pElem->addDescriptor(pDescriptor);
  

  pService->start();


  // ----- Advertising

  BLEAdvertising *pAdvertising = pServer->getAdvertising();

  BLEAdvertisementData adv;
  adv.setName(devName.c_str());
  pAdvertising->setAdvertisementData(adv);

  BLEAdvertisementData adv2;
  adv2.setCompleteServices(BLEUUID(SERVICE_UUID));
  pAdvertising->setScanResponseData(adv2);

  pAdvertising->start();


}

void loop() {
  
}