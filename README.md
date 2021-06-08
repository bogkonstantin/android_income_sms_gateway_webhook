# Incoming SMS to URL forwarder

## How to use
Set sender phone number or name and URL. It should match the number or name you see in the SMS messenger app. 
If you want to send any SMS to URL, use * (asterisk symbol) as a name.  

Every incoming SMS will be sent immediately to the provided URL. 
If the response code is not 2XX or the request ended with a connection error, the app will try to send again up to 10 times.
Minimum first retry will be after 10 seconds, later wait time will increase exponentially.
If the phone is not connected to the internet, the app will wait for the connection before the next attempt.

## Screenshots
<img alt="Incoming SMS Webhook Gateway screenshot 1" src="https://raw.githubusercontent.com/bogkonstantin/android_income_sms_gateway_webhook/master/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="30%"/> <img alt="Incoming SMS Webhook Gateway screenshot 2" src="https://raw.githubusercontent.com/bogkonstantin/android_income_sms_gateway_webhook/master/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="30%"/> <img alt="Incoming SMS Webhook Gateway screenshot 3" src="https://raw.githubusercontent.com/bogkonstantin/android_income_sms_gateway_webhook/master/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="30%"/>

## Download apk
Download apk from [release page](https://github.com/bogkonstantin/android_income_sms_gateway_webhook/releases)

<a href="https://apt.izzysoft.de/fdroid/index/apk/tech.bogomolov.incomingsmsgateway">
  <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" height="70px">
</a>
<br>
<a href="https://amzn.to/3cpxXGk">
  &nbsp;&nbsp;<img src="https://images-na.ssl-images-amazon.com/images/G/01/mobile-apps/devportal2/res/images/amazon-appstore-badge-english-black.png" height="50px">
</a>

## Recommendation
Use external monitoring tools like [UpTime.onl](https://uptime.onl/) to monitor your webhook URL and prevent it from downtime.