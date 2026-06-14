import os

def replace_texts(filepath, replacements):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    for old, new in replacements.items():
        content = content.replace(old, new)
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

# AuthScreen.kt translations
auth_path = r'd:\android-projeler\are-we-okay-app\android\app\src\main\java\com\cynorra\areweokay\ui\auth\AuthScreen.kt'
auth_replacements = {
    '"Are We Okay?"': '"İyi Miyiz?"',
    '"A global wellbeing movement. Check in daily, share how you feel anonymously, and support each other with zero judgment."': '"Global bir iyilik hareketi. Her gün check-in yap, nasıl hissettiğini anonim paylaş ve yargılanmadan destek bul."',
    '"How is your day going?"': '"Günün nasıl geçiyor?"',
    '"We\'re Good"': '"İyiyiz"',
    '"We\'re Not"': '"İyi Değiliz"',
    '"Not Sure"': '"Emin Değilim"',
    '"Together, we\'re better. ❤️"': '"Birlikte daha iyiyiz. ❤️"'
}
replace_texts(auth_path, auth_replacements)

# HomeScreen.kt translations
home_path = r'd:\android-projeler\are-we-okay-app\android\app\src\main\java\com\cynorra\areweokay\ui\home\HomeScreen.kt'
home_replacements = {
    '"Feed"': '"Akış"',
    '"Check-In"': '"Check-In"',
    '"Friends"': '"Arkadaşlar"',
    '"Insights"': '"Analizler"',
    '"Settings"': '"Ayarlar"',
    '"How has your day been? Write it here..."': '"Günün nasıl geçti? Buraya yaz..."',
    '"Make this reflection public"': '"Bu paylaşımı herkese açık yap"',
    '"Your post will appear anonymously on the global feed."': '"Gönderiniz global akışta anonim olarak görünecek."',
    '"Complete Check-in"': '"Check-in\'i Tamamla"',
    '"Check-in successful!"': '"Check-in başarılı!"',
    '"The Global Feed"': '"Global Akış"',
    '"Be kind. Support others."': '"Nazik ol. Diğerlerine destek ol."',
    '"Write a supportive comment..."': '"Destekleyici bir yorum yaz..."',
    '"Add"': '"Ekle"',
    '"Accept"': '"Kabul Et"',
    '"Pending Requests"': '"Bekleyen İstekler"',
    '"Your Circle"': '"Çevren"',
    '"Build your support circle"': '"Destek çevreni oluştur"',
    '"Search profiles and add friends to see how they\'re doing."': '"Profil ara ve arkadaşlarının nasıl olduğunu görmek için ekle."',
    '"no check-in"': '"check-in yok"',
    '"Search profiles to connect..."': '"Bağlantı kurmak için profil ara..."',
    '"Weekly Reflection History"': '"Haftalık Geçmiş"',
    '"Mood Breakdown"': '"Ruh Hali Dağılımı"',
    '" check-ins (': '" check-in (',
    '"Current Streak"': '"Günlük Seri"',
    '"Support Shared"': '"Verilen Destek"',
    '" Days"': '" Gün"',
    '" Times"': '" Kez"',
    '"Edit Profile"': '"Profili Düzenle"',
    '"Display Username"': '"Kullanıcı Adı"',
    '"Choose Your Avatar:"': '"Avatarını Seç:"',
    '"Save Settings"': '"Ayarları Kaydet"',
    '"Sign Out"': '"Çıkış Yap"',
    '"To clear your session and sign in with a different profile, click below."': '"Oturumunuzu kapatmak ve farklı bir profil ile giriş yapmak için aşağıya tıklayın."',
    '"Profile updated successfully!"': '"Profil başarıyla güncellendi!"',
    '"Good"': '"İyi"',
    '"Not Okay"': '"İyi Değil"',
    '"Unsure"': '"Emin Değilim"'
}
replace_texts(home_path, home_replacements)

print('Translations applied.')
