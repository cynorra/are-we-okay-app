import { Shield } from "lucide-react";
import Link from "next/link";

export default function KVKKPage() {
  return (
    <div className="min-h-screen bg-[var(--color-ok-beige)] text-[var(--color-ok-black)] py-24 px-6 md:px-12">
      <div className="max-w-4xl mx-auto bg-white/80 backdrop-blur-xl p-8 md:p-16 rounded-[3rem] shadow-xl border border-white/40">
        <div className="flex items-center gap-4 mb-12">
          <Link href="/" className="w-12 h-12 bg-gray-50 rounded-full flex items-center justify-center border border-gray-200 hover:bg-gray-100 transition-colors">
            &larr;
          </Link>
          <Shield className="w-8 h-8 text-[var(--color-ok-orange)]" />
          <h1 className="text-3xl md:text-5xl font-bold tracking-tight">Aydınlatma Metni (KVKK)</h1>
        </div>

        <div className="prose prose-lg text-gray-700 space-y-6">
          <p className="font-semibold text-xl text-gray-900">Son Güncelleme: {new Date().toLocaleDateString('tr-TR')}</p>
          
          <p>
            <strong>Okayness</strong> olarak, kişisel verilerinizin güvenliği ve gizliliği en büyük önceliklerimizden biridir. 
            6698 sayılı Kişisel Verilerin Korunması Kanunu ("KVKK") uyarınca, veri sorumlusu sıfatıyla sizi aydınlatmak isteriz.
          </p>

          <h2 className="text-2xl font-bold text-gray-900 mt-8 mb-4">1. Hangi Verileri Topluyoruz?</h2>
          <p>Uygulamamızı kullanırken minimum düzeyde veri topluyoruz. Amacımız sizi anonim tutmaktır:</p>
          <ul className="list-disc pl-6 space-y-2">
            <li><strong>Kimlik ve İletişim:</strong> E-posta adresi (sadece giriş ve iletişim amacıyla, diğer kullanıcılara asla gösterilmez).</li>
            <li><strong>Kullanım Verileri:</strong> Duygu durumunuz (Check-in), yazdığınız anonim notlar ve platform içi etkileşimleriniz.</li>
          </ul>

          <h2 className="text-2xl font-bold text-gray-900 mt-8 mb-4">2. Verileri Hangi Amaçla İşliyoruz?</h2>
          <p>
            Toplanan kişisel verileriniz, platformun ana fonksiyonu olan "anonim duygu paylaşımı ve destek ağının" sağlanması, 
            uygulama güvenliğinin temini ve hizmetlerimizin iyileştirilmesi amaçlarıyla işlenmektedir.
          </p>

          <h2 className="text-2xl font-bold text-gray-900 mt-8 mb-4">3. Verilerin Aktarılması</h2>
          <p>
            Kişisel verileriniz, KVKK'nın 8. ve 9. maddelerinde belirtilen şartlar dahilinde, 
            yalnızca sunucu hizmeti aldığımız güvenilir yurt dışı teknoloji iş ortaklarımıza (örneğin: Supabase) aktarılabilir. 
            Verileriniz asla reklam veya pazarlama amacıyla 3. şahıslara satılmaz.
          </p>

          <h2 className="text-2xl font-bold text-gray-900 mt-8 mb-4">4. Haklarınız (KVKK Madde 11)</h2>
          <p>
            Kanun'un 11. maddesi uyarınca veri sahibi olarak; verilerinizin işlenip işlenmediğini öğrenme,
            silinmesini veya yok edilmesini talep etme hakkına sahipsiniz.
          </p>

          <div className="mt-12 p-6 bg-[var(--color-ok-teal-light)] rounded-2xl border border-[var(--color-ok-teal)]/20">
            <p className="font-medium text-[var(--color-ok-teal)]">
              İletişim: Talepleriniz için bize <a href="mailto:hello@areweokay.com" className="font-bold underline">hello@areweokay.com</a> adresinden ulaşabilirsiniz.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
