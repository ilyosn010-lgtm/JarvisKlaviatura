# JARVIS Klaviatura (Android)

O'zbekcha klaviatura:
- tepada 1-0 raqamlar qatori,
- tepada 3 ta o'girish tugmasi: yangi harflar (ÖŞ), eski harflar (OʻSh), kirill (ЎШ),
- nusxalangan matn bo'lsa "Joylash" qatori chiqadi,
- raqam / kod / telefon maydonlarida faqat raqamli klaviatura chiqadi,
- parol va kod maydonlarida o'girish tugmalari va "Joylash" qatori ko'rinmaydi, bufer o'qilmaydi.

## APK yasash: GitHub orqali (kompyuterga hech narsa o'rnatish shart emas)
1. github.com da bepul hisob oching.
2. New repository ni bosing, nomi masalan JarvisKlaviatura. Public yoki Private, ikkalasi ham bo'ladi.
3. "uploading an existing file" havolasini bosing va shu papkadagi HAMMA fayl va papkalarni (app, .github, build.gradle.kts, settings.gradle.kts, gradle.properties) ichiga sudrab tashlang. Commit changes ni bosing.
4. Yuqoridagi Actions bo'limiga kiring, "APK yasash" ni tanlang. 3-6 daqiqada yashil belgi chiqadi.
5. Tayyor yozuvni oching, pastdagi Artifacts ichidan JarvisKlaviatura-APK ni yuklab oling (zip, ichida .apk).
6. APK ni telefonga o'tkazing, o'rnating ("noma'lum manbalardan o'rnatish"ga ruxsat bering).
7. Ilovani oching: "Klaviaturani yoqish" va "Klaviaturani tanlash" tugmalarini ketma-ket bosing.

Qizil xato chiqsa, Actions oynasidagi xato matnini yuboring.

## Android Studio orqali
File > Open > shu papka > Build > Build APK(s). APK app/build/outputs/apk/debug/ ichida chiqadi.
