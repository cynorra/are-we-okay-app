# Are We Okay / iyimiyiz: Psikolog Sosyal Platformu + Mood Takibi Donusum Plani

Tarih: 2026-11-23. Kapsam: sadece arastirma ve plan (kod yok). Not: Serena/graphify yerine hedefli dosya listesi + grep kullanildi (token tasarrufu).

## 1. Mevcut durum (ozet)

Repo yapisi: `are-we-okay-app/` = android (Kotlin, Compose, Navigation3) + web (Next.js, TS). `iyimiyiz-global/` = Next.js web, next-intl ile 18 dil (messages/*.json), android-app klasoru da var. Backend: **Supabase** (README Firebase diyor ama kod/SQL Supabase: `supabase_schema.sql`, `SupabaseApi.kt`). Google ile giris, anonim kullanici adi + avatar emoji.

Mevcut ozellikler:
- Giris: Google ID token (Supabase auth), profil (username, avatar_emoji), role alani (default 'user').
- Check-in: 3 durumlu mood (good/bad/unsure), kisa not, is_public bayragi.
- Feed: anonim/isimli post, status (pending/approved/rejected), soft delete, 4 tur reaksiyon (hug, feel_this, strength, you_got_this), yorumlar.
- Arkadaslik: arama, istek gonder/kabul, arkadas mood'lari.
- Insights: kullanici istatistigi, streak hesabi (`calculateStreak`), haftalik mood.
- Harita (web `map`), cron-feed (seed icerik), KVKK/privacy/terms sayfalari, AdMob banner.
- Eksikler: 3 mood cok kaba, journaling/etiket yok, hatirlatma yok, paylasim karti yok, kriz akisi yok, raporlama/moderasyon arayuzu yok, psikolog kavrami yok (sadece `role` alani var), Android'de feed mock/in-memory cache agirlikli (DataRepository `getFeedPosts` sync liste).

## 2. Rakip ozellik matrisi

Kaynak notu: arama sonuclari tutarsiz fiyat veriyor (affiliate siteler); fiyatlar yaklasiktir, magaza sayfalarindan dogrulanmali. BetterHelp/Talkspace/Blueprint/Tebra/Pskolog bilgileri genel bilgidir, bu oturumda dogrulanmadi.

| Urun | Mood takip | Journal | Streak/oyun | Nefes/meditasyon | Topluluk | Terapist rehberi/randevu | AI | Not |
|---|---|---|---|---|---|---|---|---|
| Daylio | Evet (5 seviye+aktivite) | Mikro-gunluk | Seri | Hayir | Hayir | Hayir | Hayir | Hizli, istatistik, PIN kilidi |
| Moodnotes | Evet | Evet (CBT dusunce tuzaklari) | Hayir | Hayir | Hayir | Hayir | Hayir | iOS agirlikli |
| Finch | Evet | Evet | Evet (evcil hayvan) | Evet | Sinirli | Hayir | Hayir | Gen Z, Plus ~$7/ay |
| How We Feel | Evet (renk/duygu tekerlegi) | Hafif | Hayir | Evet | Hayir | Hayir | Hayir | Yale kaynakli, ucretsiz |
| Wysa | Evet | Evet | Hayir | Evet | Hayir | Insan koc ekleme | Evet (CBT bot) | Kurumsal B2B guclu |
| Calm/Headspace | Hafif | Hafif | Seri | Cok guclu | Hayir | Headspace terapi ekler | Headspace Ebb | ~$70/yil |
| 7 Cups | Hayir | Hayir | Hayir | Hayir | Evet (dinleyici sohbeti) | Ucretli terapist | Hafif | Gonullu dinleyici modeli |
| BetterHelp/Talkspace | Hayir | Hayir | Hayir | Hayir | Hayir | Evet (abonelik, mesaj/video) | Hayir | Ucretli, dis lisans dogrulama |
| Psikolog dizinleri (Psychology Today vb.) | Hayir | Hayir | Hayir | Hayir | Hayir | Dizin + profil | Hayir | Lisans, uzmanlik filtresi |
| Blueprint/Tebra | Hayir | Hayir | Hayir | Hayir | Hayir | Klinik yazilim (not, faturalama) | Evet | Terapist backoffice, biz rakip degiliz |
| TR: Hiwell, Healmeup, Psikologca, Meditopia | Kismen | Kismen | Hayir | Meditopia guclu | Hayir | Evet (Hiwell 2000+ uzman iddiasi) | Hafif | Odemeli seans, kisa on gorusme |

Bosluk (firsat): hicbiri "mood takibi + psikolog icerik/topluluk + anonim akran destegi"yi tek yerde sunmuyor. Terapist tarafinda icerik paylasan, mood ile ilgili psiko-egitim yazan bir "LinkedIn+Instagram for psychologists" ve bu icerigi kullanicinin mood'una gore sunan akis bos. Ilk surumde seans/odeme/video YAPMA (regulasyon + maliyet agir); randevu = iletisim talebi/harici link.

## 3. Onerilen ozellikler

### Genel kullanici
- Mood check-in v2: 5 seviye + duygu etiketleri + aktivite/uyku etiketi, gunluk hatirlatma.
- Streak + yumusak oyunlastirma (rozet, "kendine nazik" streak freeze; cezalandirici degil).
- Journaling: sablonlu (minnet, CBT dusunce kaydi), kilit/PIN, yalniz-yerel secenegi.
- Nefes egzersizi (4-7-8, kutu nefes, animasyon), 3-5 dk, ses opsiyonel.
- Insights: haftalik/aylik trend, aktivite-mood korelasyonu, PDF/paylasim ozeti (psikologa gostermek icin).
- Topluluk: odalar (anksiyete, uyku, ogrenci vb.), anonim post (mevcut), reaksiyonlar (mevcut), rapor/engelle.
- Arkadas davet: deep link + referrer, "bir arkadasina nasilsin sor" tek dokunus.
- Paylasilabilir mood karti: gunun mood'u/seri/alinti, marka filigranli, kisisel veri icermez.
- Kriz yolu: sabit "Yardim al" butonu, ulke bazli hat numaralari (TR: 112, Alo 183 sosyal destek; ABD 988), kriz kelimesi tespitinde nazik uyari.
- Psikolog icerik akisi: takip, kaydet, "bugunku ruh haline uygun ipucu".

### Psikolog
- Profil: foto, unvan, uzmanlik alanlari, dil, yaklasim (BDT, EMDR...), sehir/online, sozlesmeli ucret araligi (opsiyonel).
- Dogrulama: diploma/lisans belgesi yukleme + manuel admin onayi, "Dogrulandi" rozeti (TR: Psikolog unvani icin diploma, klinik psikoloji icin ilgili belge; ABD: eyalet lisans no). Dogrulanmamis hesap icerik paylasabilir ama rozetsiz, danisan iletisimi kapali.
- Icerik: kisa yazi, ipucu karti, mini egzersiz (nefes/dusunce kaydi sablonu) paylasimi; moderasyon + zorunlu "Bu icerik tibbi tavsiye degildir/tani koymaz" etiketi.
- Randevu/mesaj: P1'de "iletisim talebi" (kullanici mesaj+tercih gonderir, psikolog yanitlar); harici takvim linki (Calendly vb.). P2'de uygulama ici sifreli mesaj. Seans/odeme/video YOK (ilk fazda).
- Topluluk: psikolog-psikolog kapali oda, vaka paylasimi YASAK (anonimlestirilmis dahi), superviz sorulari genel formatta.
- Analitik: icerik goruntulenme, kaydetme, takipci (kisisel mood verisine erisim YOK).
- Etik/gizlilik: kullanici izin vermedikce danisan mood verisi psikologa gorunmez; paylasim kullanici tarafindan, sureli ve iptal edilebilir ("psikologumla paylas" linki).

## 4. Viral / engagement mekanikleri
- Gunluk mood karti paylasimi (Story boyutu 9:16) + deep link ile "sen nasilsin?" daveti.
- Arkadas daveti, karsilikli "bugun nasilsin" sinyali (arkadaslik zaten var), 2 kisilik ortak seri.
- Haftalik "mood ozeti" kutlama kartlari (Spotify Wrapped tarzi, yillik).
- Psikologlar kendi profil linkini/QR'ini danisanlarina verir: danisan -> uygulama (B2B2C buyume kanali, en guclu buyume levyesi).
- Push: gunluk nudge (zaman secmeli), azalan frekans, uyku/aksam secenegi; manipulatif kayip korkusu kullanma.
- ASO: yerel anahtar kelimeler (ruh hali takibi, gunluk, duygu gunlugu, nefes egzersizi, psikolog), 18 dil altyapisi var, dolayisiyla coklu dil magaza listesi hazir.
- Gelir: ilk faz AdMob banner (mevcut) ama ruh sagligi kategorisinde reklam hassasiyeti: reklamlari hassas ekranlarda (kriz, journal) gizle, ilgi tabanli kisisellestirme kapat; P2: Premium (tema, PDF rapor, ekstra egzersiz) + psikologlar icin Pro profil.

## 5. Politika / hukuk riskleri

Google Play:
- Health apps declaration zorunlu; "Mental and Behavioral Health" kategorisi secilir, in-app + store'da gizlilik politikasi linki (kaynak: Play Console health apps policy). Saglik verisi minimum toplanir.
- Yaniltici saglik iddialari yasak: "tedavi eder/tani koyar" dili kullanma, uygulama icinde ve listing'de tibbi tavsiye degildir uyarisi. Play'in tibbi cihaz (MDCG) uyumlu guncellemesi AB'yi hedefleyen yazilimlar icin dikkat: tani/klinik karar destegi YAPMA.
- UGC politikasi (sosyal): rapor, engelleme, moderasyon, icerik filtresi, kullanim kosullari zorunlu. Hesap silme: uygulama ici + web silme URL'si, Data Safety formu guncel olmali.
- Cocuk: hedef kitle 13+/18+ (psikolog kismi 18+), Families politikasina girme.
- Reklam: saglik verisiyle reklam kisisellestirmesi yasak.
- Dogrulanmamis: UGC icin bu oturumda 2026'ya ozgu yeni kural bulunamadi, yayin oncesi Play Policy Deadlines sayfasi kontrol edilmeli.

KVKK (TR):
- Saglik verisi ve ruh sagligi = ozel nitelikli kisisel veri (m.6). KVK Kurumu 2025 rehberi: acik riza (ayri, ozel, sureli geri alinabilir) veya m.6 sartlari; 7499 sayili kanun degisikligi sonrasi isleme sartlarini guncel hukuk danismanindan dogrula.
- Aydinlatma metni, VERBIS kaydi esigi (veri sorumlusu), yurt disi aktarim (Supabase/AB/ABD bolgesi icin acik riza veya standart sozlesme), saklama ve imha politikasi, veri ihlali 72 saat bildirimi.
- Psikolog: meslek sirri; danisan mood paylasimi sadece kullanici onayiyla.

GDPR (AB/global):
- Art. 9 ozel kategori: acik riza; DPIA (buyuk olcek/hassas veri), veri silme/tasima (export), DPO esigi, veri islemcileri (Supabase DPA), cocuk 16 yas esigi (ulke bazli 13-16).
- Bolge: Supabase proje bolgesi AB (Frankfurt) onerilir; TR kullanici verisi icin yurt disi aktarim metni.
- ABD: HIPAA genelde uygulanmaz (covered entity degiliz) ama FTC Health Breach Notification Rule ve eyalet yasalari (WA My Health My Data) geçerli: reklam/analitik SDK'larina mood verisi GONDERME.

Operasyonel riskler: kriz/intihar icerigi (otomatik tespit + insan moderasyonu + hat yonlendirme), sahte psikolog (manuel dogrulama), tibbi tavsiye sorumlulugu (disclaimer + icerik kurallari), spam/taciz, "tek gelistirici moderasyon yuku".

## 6. Onceliklendirilmis plan

Efor: 1 gelistirici, gun bazli (tahmini, gercek hizi kullanici/CI sureci etkiler).

### P0 (temel, 5-6 hafta ~ 25-30 gun)
| # | Is | Efor |
|---|---|---|
| P0.1 | Hukuki temel: KVKK/GDPR aydinlatma+riza akisi, hesap silme (app+web), disclaimer, Play Health declaration, Data Safety | 4 g |
| P0.2 | Mood v2 (5 seviye+duygu etiketi+aktivite), eski 3-mood geri uyum migrasyonu | 3 g |
| P0.3 | Journaling (sablon, yerel/sifreli secenek) | 4 g |
| P0.4 | Streak + rozet + gunluk hatirlatma (WorkManager) | 3 g |
| P0.5 | Nefes egzersizi (3 teknik, animasyon) | 2 g |
| P0.6 | Paylasilabilir mood karti + davet deep link | 3 g |
| P0.7 | Kriz yolu (sabit buton, ulke hatlari) | 1 g |
| P0.8 | Moderasyon temel: rapor/engelle, kelime filtresi, admin panel (web) | 5 g |
| P0.9 | Reklam hassasiyeti ayarlari (kriz/journal ekranlarinda reklam yok) | 1 g |

### P1 (psikolog katmani, 6-8 hafta ~ 30-40 gun)
| # | Is | Efor |
|---|---|---|
| P1.1 | Rol sistemi (user/psychologist/admin), psikolog onboarding | 3 g |
| P1.2 | Dogrulama akisi (belge yukleme, admin onay, rozet) | 5 g |
| P1.3 | Psikolog profili + dizin (uzmanlik/dil/sehir filtre, arama) | 6 g |
| P1.4 | Icerik paylasimi (post tipleri, etiket, disclaimer, moderasyon kuyrugu), takip, kaydet | 7 g |
| P1.5 | Iletisim talebi (inbox, bildirim) + harici randevu linki | 5 g |
| P1.6 | Topluluk odalari (konu bazli) + rapor, rol rozetleri | 5 g |
| P1.7 | Insights v2 + "psikologumla paylas" sureli link/PDF ozet (kullanici onayli) | 5 g |
| P1.8 | Web sitesi: psikolog public profil sayfalari (SEO), 18 dilde | 4 g |

### P2 (buyume/gelir, 6+ hafta)
- Uygulama ici sifreli mesajlasma (E2E veya sunucu tarafi sifreleme) ~ 10 g.
- Premium abonelik (Play Billing) + psikolog Pro profil ~ 8 g.
- AI yansima/ozet (on-device veya KVKK uyumlu API, tani yok) ~ 8 g.
- Yillik/haftalik "wrapped" kartlari, ortak seri, arkadas widget'i ~ 6 g.
- Video seans / odeme / randevu takvimi (en sona, regulasyon + PCI + lisans gerekir) ~ 20+ g.
- Kurumsal/B2B (universite, sirket) paneli.

Basari metrikleri: D1/D7/D30 retention, gunluk check-in orani, davet basina kurulum (k-factor), onayli psikolog sayisi, psikolog basina icerik/hafta, rapor/moderasyon suresi.

## 7. Veri modeli ozeti (Supabase, mevcut tablolara ek)

Mevcut: users, checkins, posts, reactions, friendships, comments.

Degisiklik:
- `users`: role (user|psychologist|admin), locale, country, onboarding_done, consent_version/consent_at (ozel nitelikli veri acik riza kaydi), deleted_at.
- `checkins`: mood smallint (1-5; eski good/bad/unsure -> 4/1/3 map), emotions text[], activities text[], sleep_hours, is_public, share_with_psychologist bool.
- `journal_entries` (id, user_id, template, body encrypted/nullable, mood_id, created_at, local_only flag).
- `streaks` / `badges`, `user_badges` (turetilebilir, cache).
- `psychologist_profiles` (user_id pk, title, bio, specialties text[], languages text[], approaches text[], city, online bool, booking_url, verified_status [none|pending|verified|rejected], verified_at, license_country, license_no_hash).
- `verification_documents` (id, user_id, storage_path (private bucket), status, reviewer_id, reason, created_at; erisim sadece admin; onay sonrasi belge sil/saklama suresi).
- `articles`/`posts` genisletme: author_role, kind (post|tip|exercise|article), tags, room_id, status moderation, disclaimer_ack.
- `rooms` (id, slug, title_i18n jsonb, kind), `room_members`.
- `follows` (follower_id, followee_id).
- `saved_items` (user_id, post_id).
- `contact_requests` (id, from_user, psychologist_id, message, share_token nullable, status, created_at).
- `data_shares` (id, user_id, psychologist_id, scope [summary|checkins], expires_at, revoked_at) - psikolog verisi sadece buradan, RLS ile.
- `reports` (id, reporter_id, target_type, target_id, reason, status), `blocks` (blocker_id, blocked_id).
- `referrals` (inviter_id, invitee_id, source, created_at).
- `notifications`/`device_tokens`, `reminder_prefs`.
- `audit_log` (admin eylemleri, belge erisimi).

RLS ilkeleri: default-deny; checkins/journal sadece sahibi; psikolog `data_shares` varsa ve suresi dolmamissa okur; belgeler private storage + admin rolu; tum silmelerde cascade + soft delete -> N gun sonra hard delete (KVKK/GDPR silme hakki).

## 8. Teknik notlar / ilk adimlar
- Android tarafinda DataRepository arayuzu var; yeni modeller icin ayri repository (PsychologistRepository, JournalRepository) onerilir, tek dev dosya sismesin.
- README Firebase diyor ama Supabase kullaniliyor: dokumani duzelt; iki repo (are-we-okay-app web ve iyimiyiz-global web) kod tekrari var, tek monoreponun i18n'li surumune birlestirmeyi dusun.
- Mock/seed (cron-feed) icerigi gercek kullanici yokken kalsin, ama "gercek psikolog" zannettirmemek icin etiketle (aldatici icerik riski, Play politikasi).
- CI/derleme kullanicinin kurallarina uygun (yerel build yerine CI) - bu repo icin dogrulanmadi.
- Ilk karar noktalari: (1) seans/odeme ilk fazda olmayacak mi, (2) Supabase bolgesi, (3) psikolog dogrulama manuel mi, (4) marka (Are We Okay vs iyimiyiz) TR/global ayrimi.
