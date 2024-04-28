# Incoming SMS to URL forwarder

## How to use

Set up App Permissions for you phone after installation. For example, enable "Autostart" if needed
and "Display pop-up windows while running in the background" from Xiaomi devices.

Set sender phone number or name and URL. It should match the number or name you see in the SMS messenger app. 
If you want to send any SMS to URL, use * (asterisk symbol) as a name.  

Every incoming SMS will be sent immediately to the provided URL.
If the response code is not 2XX or the request ended with a connection error, the app will try to
send again up to 10 times (can be changed in parameters).
Minimum first retry will be after 10 seconds, later wait time will increase exponentially.
If the phone is not connected to the internet, the app will wait for the connection before the next
attempt.  

If at least one Forwarding config is created and all needed permissions granted - you should see F
icon in the status bar, means the app is listening for the SMS.

Press the Test button to make a test request to the server.

Press the Syslog button to view errors stored in the Logcat.

### Request info
HTTP method: POST  
Content-type: application/json; charset=utf-8  

Sample payload:  
```json
{
     "from": "%from%",
     "text": "%text%",
     "sentStamp": "%sentStamp%",
     "receivedStamp": "%receivedStamp%",
     "sim": "%sim%"
}
```

Available placeholders:
%from%
%text%
%sentStamp%
%receivedStamp%
%sim%

### Request example
Use this curl sample request to prepare your backend code
```bash
curl -X 'POST' 'https://yourwebsite.com/path' \
     -H 'content-type: application/json; charset=utf-8' \
     -d $'{"from":"1234567890","text":"Test"}'
```

### Send SMS to the Telegram

1. Create Telegram bot and channel to receive messages. [There](https://bogomolov.tech/Telegram-notification-on-SSH-login/) is short tutorial how to do that.  
2. Add new forwarding configuration in the app using this parameters:
   1. Any sender you need, * - on the screenshot
   2. Webhook URL - `https://api.telegram.org/bot<YourBOTToken>/sendMessage?chat_id=<channel_id>` - change URL using your token and channel id
   3. Use this payload as a sample `{"text":"sms from %from% with text: \"%text%\" sent at %sentStamp%"}`
   4. Save configuration

<img alt="Incoming SMS Webhook Gateway screenshot Telegram example" src="https://raw.githubusercontent.com/bogkonstantin/android_income_sms_gateway_webhook/master/fastlane/metadata/android/en-US/images/phoneScreenshots/telegram.png" width="30%"/> 

### Process Payload in PHP scripts

Since $_POST is an array from the url-econded payload, you need to get the raw payload. To do so use file_get_contents:
```php
$payload = file_get_contents('php://input');
$decoded = json_decode($payload, true);
```

## Screenshots
<img alt="Incoming SMS Webhook Gateway screenshot 1" src="https://raw.githubusercontent.com/bogkonstantin/android_income_sms_gateway_webhook/master/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="30%"/> <img alt="Incoming SMS Webhook Gateway screenshot 2" src="https://raw.githubusercontent.com/bogkonstantin/android_income_sms_gateway_webhook/master/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="30%"/> <img alt="Incoming SMS Webhook Gateway screenshot 3" src="https://raw.githubusercontent.com/bogkonstantin/android_income_sms_gateway_webhook/master/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="30%"/>

## Download apk

Download apk from [release page](https://github.com/bogkonstantin/android_income_sms_gateway_webhook/releases)

Or download it from F-Droid

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/tech.bogomolov.incomingsmsgateway/)

This repository contains a stable app with minimum functionality. It is not archived, but not actively developing. If you need an app with merged PRs - try [this fork](https://github.com/scottmconway/android_income_sms_gateway_webhook)
