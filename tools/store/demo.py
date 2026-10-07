"""Demo tiles for the store screenshots, in every language of the app.

Each tile: key, group (None for ungrouped), colour, icon, size, time since it was last done (seconds),
press count, reminder (every, unit) or None. Names come from NAMES, by store locale.
PHOTOS gives some tiles a photo background, from this folder.
"""

M, H, D = 60, 3600, 86400

GROUPS = ["home", "health", "pets"]

TILES = [
    # key, group, colour, icon, size, elapsed, presses, reminder
    ("plants", None, "sage", "sprout", "wide", 3 * D + 5 * H + 12 * M, 42, (4, "days")),
    ("cat", None, "peach", "cat", "tall", 2 * H + 15 * M + 20, 318, None),
    ("grandma", None, "sky", "phone", "small", 8 * D + 3 * H + 40 * M, 27, (1, "weeks")),
    ("haircut", None, "rose", "scissors", "small", 34 * D + 6 * H + 5 * M, 9, None),
    ("oil", None, "slate", "car", "wide", 377 * D + 4 * H, 4, None),
    ("sheets", "home", "lavender", "bed", "small", 9 * D + 2 * H + 30 * M, 18, (2, "weeks")),
    ("trash", "home", "sand", "trash", "small", 14 * H + 20 * M, 87, None),
    ("coffee", "home", "coral", "coffee", "wide", 41 * D + 7 * H, 5, None),
    ("fridge", "home", "teal", "refrigerator", "small", 58 * D + 4 * H, 6, None),
    ("vitamins", "health", "butter", "pill", "small", 20 * H + 40 * M, 156, (1, "days")),
    ("run", "health", "lime", "footprints", "small", 1 * D + 2 * H + 10 * M, 64, None),
    ("toothbrush", "health", "orchid", "toothbrush", "small", 71 * D + 9 * H, 7, None),
    ("flea", "pets", "butter", "paw-print", "small", 23 * D + 11 * H, 11, (4, "weeks")),
]

# Tile key -> photo in tools/store/ (see README for its licence).
PHOTOS = {"plants": "plant.jpg", "cat": "cat.jpg"}

# Store locale -> Android locale the app is switched to.
ANDROID_LOCALE = {
    "en-US": "en-US", "fr-FR": "fr-FR", "es-ES": "es-ES", "de-DE": "de-DE", "it-IT": "it-IT",
    "pt-BR": "pt-BR", "nl-NL": "nl-NL", "pl-PL": "pl-PL", "ru-RU": "ru-RU", "tr-TR": "tr-TR",
    "id": "in-ID", "ar": "ar-EG", "hi-IN": "hi-IN", "ja-JP": "ja-JP", "ko-KR": "ko-KR", "zh-CN": "zh-CN",
}

# Groups first (home, health, pets), then tiles in TILES order.
_KEYS = GROUPS + [t[0] for t in TILES]

_NAMES = {
    "en-US": ["Home", "Health", "Pets",
              "Watered the plants", "Fed the cat", "Called Grandma", "Haircut", "Car oil change",
              "Changed the bedsheets", "Took out the trash", "Descaled the coffee machine", "Cleaned the fridge",
              "Took vitamins", "Went for a run", "New toothbrush", "Flea treatment"],
    "fr-FR": ["Maison", "Santé", "Animaux",
              "Arroser les plantes", "Nourrir le chat", "Appeler Mamie", "Coupe de cheveux", "Vidange de la voiture",
              "Changer les draps", "Sortir les poubelles", "Détartrer la cafetière", "Nettoyer le frigo",
              "Prendre les vitamines", "Course à pied", "Nouvelle brosse à dents", "Traitement antipuces"],
    "es-ES": ["Casa", "Salud", "Mascotas",
              "Regar las plantas", "Dar de comer al gato", "Llamar a la abuela", "Corte de pelo", "Cambio de aceite",
              "Cambiar las sábanas", "Sacar la basura", "Descalcificar la cafetera", "Limpiar la nevera",
              "Tomar vitaminas", "Salir a correr", "Cepillo de dientes nuevo", "Antipulgas"],
    "de-DE": ["Zuhause", "Gesundheit", "Haustiere",
              "Pflanzen gegossen", "Katze gefüttert", "Oma angerufen", "Friseur", "Ölwechsel",
              "Bettwäsche gewechselt", "Müll rausgebracht", "Kaffeemaschine entkalkt", "Kühlschrank geputzt",
              "Vitamine genommen", "Joggen gewesen", "Neue Zahnbürste", "Flohschutz"],
    "it-IT": ["Casa", "Salute", "Animali",
              "Annaffiare le piante", "Dare da mangiare al gatto", "Chiamare la nonna", "Taglio di capelli",
              "Cambio dell’olio", "Cambiare le lenzuola", "Portare fuori la spazzatura",
              "Decalcificare la macchina del caffè", "Pulire il frigo",
              "Prendere le vitamine", "Corsa", "Spazzolino nuovo", "Antipulci"],
    "pt-BR": ["Casa", "Saúde", "Pets",
              "Regar as plantas", "Dar comida ao gato", "Ligar para a vovó", "Corte de cabelo", "Troca de óleo",
              "Trocar a roupa de cama", "Levar o lixo para fora", "Descalcificar a cafeteira", "Limpar a geladeira",
              "Tomar vitaminas", "Correr", "Escova de dentes nova", "Antipulgas"],
    "nl-NL": ["Huis", "Gezondheid", "Huisdieren",
              "Planten water gegeven", "Kat gevoerd", "Oma gebeld", "Naar de kapper", "Olie ververst",
              "Bed verschoond", "Vuilnis buitengezet", "Koffiezetapparaat ontkalkt", "Koelkast schoongemaakt",
              "Vitamines genomen", "Hardgelopen", "Nieuwe tandenborstel", "Vlooienmiddel"],
    "pl-PL": ["Dom", "Zdrowie", "Zwierzęta",
              "Podlanie roślin", "Karmienie kota", "Telefon do babci", "Strzyżenie", "Wymiana oleju",
              "Zmiana pościeli", "Wyniesienie śmieci", "Odkamienianie ekspresu", "Mycie lodówki",
              "Witaminy", "Bieganie", "Nowa szczoteczka", "Preparat na pchły"],
    "ru-RU": ["Дом", "Здоровье", "Питомцы",
              "Полить цветы", "Покормить кота", "Позвонить бабушке", "Стрижка", "Замена масла",
              "Сменить постельное бельё", "Вынести мусор", "Очистить кофеварку от накипи", "Помыть холодильник",
              "Принять витамины", "Пробежка", "Новая зубная щётка", "Капли от блох"],
    "tr-TR": ["Ev", "Sağlık", "Evcil hayvanlar",
              "Bitkiler sulandı", "Kedi beslendi", "Anneanne arandı", "Saç kesimi", "Yağ değişimi",
              "Çarşaflar değişti", "Çöp çıkarıldı", "Kahve makinesinin kireci alındı", "Buzdolabı temizlendi",
              "Vitamin alındı", "Koşu", "Yeni diş fırçası", "Pire ilacı"],
    "id": ["Rumah", "Kesehatan", "Hewan peliharaan",
           "Menyiram tanaman", "Memberi makan kucing", "Menelepon nenek", "Potong rambut", "Ganti oli mobil",
           "Ganti seprai", "Buang sampah", "Membersihkan kerak mesin kopi", "Membersihkan kulkas",
           "Minum vitamin", "Lari pagi", "Sikat gigi baru", "Obat kutu"],
    "ar": ["المنزل", "الصحة", "الحيوانات الأليفة",
           "سقيت النباتات", "أطعمت القطة", "اتصلت بجدتي", "قصّ الشعر", "تغيير زيت السيارة",
           "غيّرت الشراشف", "أخرجت القمامة", "أزلت الترسبات من آلة القهوة", "نظّفت الثلاجة",
           "تناولت الفيتامينات", "ركضت", "فرشاة أسنان جديدة", "علاج البراغيث"],
    "hi-IN": ["घर", "सेहत", "पालतू जानवर",
              "पौधों को पानी दिया", "बिल्ली को खाना दिया", "दादी को फ़ोन किया", "बाल कटवाए", "कार का तेल बदलवाया",
              "चादरें बदलीं", "कूड़ा बाहर रखा", "कॉफ़ी मशीन की सफ़ाई", "फ़्रिज साफ़ किया",
              "विटामिन लिए", "दौड़ लगाई", "नया टूथब्रश", "पिस्सू की दवा"],
    "ja-JP": ["家", "健康", "ペット",
              "植物に水をやった", "猫にごはん", "おばあちゃんに電話", "散髪", "オイル交換",
              "シーツを替えた", "ゴミ出し", "コーヒーメーカーの洗浄", "冷蔵庫の掃除",
              "ビタミンを飲んだ", "ランニング", "歯ブラシ交換", "ノミ取り"],
    "ko-KR": ["집", "건강", "반려동물",
              "화분에 물 주기", "고양이 밥 주기", "할머니께 전화", "머리 자르기", "엔진오일 교환",
              "침대 시트 갈기", "쓰레기 버리기", "커피 머신 석회 제거", "냉장고 청소",
              "비타민 먹기", "달리기", "칫솔 교체", "벼룩 약"],
    "zh-CN": ["家务", "健康", "宠物",
              "给植物浇水", "喂猫", "给奶奶打电话", "理发", "汽车换机油",
              "换床单", "倒垃圾", "咖啡机除垢", "清洁冰箱",
              "吃维生素", "跑步", "换新牙刷", "驱跳蚤"],
}

assert all(len(v) == len(_KEYS) for v in _NAMES.values()), {k: len(v) for k, v in _NAMES.items()}
assert set(_NAMES) == set(ANDROID_LOCALE)

LOCALES = list(ANDROID_LOCALE)


def names(locale: str) -> dict[str, str]:
    """Group and tile names by key, for a store locale."""
    return dict(zip(_KEYS, _NAMES[locale]))
