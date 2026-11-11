package com.iffahshield.app;

/**
 * Second built-in blocklist (v2.1): AI "undress"/NSFW generators, more sites, seduction wording,
 * Arabic dialects and many more languages. Same rules as BuiltinWords: "=" = whole word only.
 */
final class BuiltinWords2 {
    static final String[] LIST = {
        // ---------- AI-generated nudity / "undress" tools ----------
        "undress ai", "undressai", "ai undress", "undresser", "undress app", "undress her", "nudify", "nudifier",
        "deepnude", "deep nude", "clothoff", "cloth off", "clothes remover", "remove clothes", "remove her clothes",
        "nsfw ai", "ai nsfw", "nsfw generator", "nsfw chat", "nsfw image", "nsfw art", "ai nude", "nude ai",
        "nude generator", "naked ai", "ai naked", "sexy ai", "ai sexy", "ai hentai", "ai girlfriend nsfw",
        "pornpen", "seduced.ai", "candy.ai", "crushon", "soulgen", "promptchan", "dreamgf", "spicychat",
        "nsfw character", "uncensored image", "ازاله الملابس", "ازاله ملابس", "خلع الملابس بالذكاء",
        "تعريه الصور", "تعريه صور", "تعريه بالذكاء", "صور عاريه بالذكاء", "بنات بالذكاء الاصطناعي عاري",

        // ---------- more sites (names without "porn"/"xnxx" in them) ----------
        "=beeg", "ixxx", "fuq.com", "tiava", "alohatube", "nudevista", "simpcity", "kemono.", "fikfap",
        "xxxtik", "sex4arab", "sexalarab", "xgroovy", "sexu.com", "bellesa", "lustery", "dirtyroulette",
        "luckycrush", "=cam4", "imlive", "mydirtyhobby", "xhamsterlive", "tiktits", "nudogram", "bunkr",
        "antarvasna", "savita bhabhi", "lolicon", "shotacon",

        // ---------- English: seduction / suggestive / acts ----------
        "=seduce", "=seduction", "=sensual", "=cleavage", "=underboob", "=sideboob", "=nip slip",
        "=wardrobe malfunction", "=g-string", "=micro bikini", "=swimsuit model", "=lingerie model",
        "=hot model", "=hot models", "=hot babes", "=babes", "=sexy dance", "=lap dance", "=pole dance",
        "=strip club", "=stripclub", "=nude scene", "=nude scenes", "=sex scene", "=sex scenes", "=hot scene",
        "=hot scenes", "=bold scene", "=bold scenes", "=hot kiss", "=erotic movie", "=adult movie",
        "=adult movies", "=adult film", "=adult films", "=adult site", "=adult chat", "=dirty talk",
        "=dirty chat", "=femdom", "=hotwife", "=cuckold", "=swingers", "=camel toe", "=cameltoe",
        "=booty shorts", "=thicc", "=thick girls", "=curvy girls", "=big tits", "=huge boobs", "=teen sex",
        "=barely legal", "=jailbait", "=doujin", "=doujinshi", "=oppai", "=ahegao", "=yaoi", "=r34",
        "=rule 34", "=lewds", "=escorts", "=escort service", "=prostitute", "=prostitutes", "=brothel",
        "=massage parlor", "=nuru", "=tantric massage", "=sext", "=hookups", "=one night stand",
        "=friends with benefits", "=casual sex", "=sex dating", "=adult dating", "=clit", "=clitoris",
        "=g-spot", "=squirting", "=rimjob", "=fisting", "=doggystyle", "=doggy style", "=kamasutra",
        "=kama sutra", "=sex positions", "=sex position", "=gooning", "=gooner", "=wank", "=wanking",
        "=cumslut", "=cum shot", "=cumming", "=jizz", "=boner", "=hard cock", "=wet pussy", "=horny girls",
        "=nudes leak", "=leaked video", "=viral mms", "=mms leaked", "=blue film", "=blue films",
        "=bf video", "=sexy video", "=hot web series", "=ullu", "=desi bhabhi", "=bhabhi hot", "=desi hot",
        "=hot aunty", "=sexy aunty", "=mallu hot", "=mallu aunty",

        // ---------- Arabic: seduction wording, body, dialects (normalized) ----------
        "فستان مثير", "ملابس مثيره", "لبس مثير", "صور مثيره", "مقاطع مثيره", "فيديو مثير", "بنات مثيرات",
        "رقص مثير", "جسم مثير", "مفاتن", "اطلاله جرييه", "فستان جريي", "مشهد جريي", "مشاهد جرييه",
        "لقطات جرييه", "صور جرييه", "فيلم جريي", "افلام جرييه", "بدون ملابس", "من غير هدوم", "بلا ملابس",
        "خلعت ملابسها", "تخلع ملابسها", "بالمايوه", "=مايوه", "=بكيني", "بيكيني", "قبله ساخنه",
        "قبلات ساخنه", "بوس ساخن", "مشهد ساخن", "مشاهد حميمه", "مشهد حميم", "لقطه حميمه", "رومانسي ساخن",
        "محتوي للبالغين", "للبالغين فقط", "=سكسيه", "=سكسات", "=بنيك", "=منيوكه", "تنيكه",
        "=بزها", "=نهود", "نهودها", "نهديها", "صدرها العاري", "=موخرتها", "موخره كبيره", "=ارداف", "اردافها",
        "افخاذها", "فخادها", "=مهتاجه", "=ممحونات", "=شبق", "=شبقه", "بنات ليل", "بنت ليل", "فتيات ليل",
        "بنات هوي", "بايعات هوي", "=مومس", "=مومسات", "بيت دعاره", "=خليعه", "=خليع", "=رقاصه",
        "=رقاصات", "لايف بنات", "بث مباشر بنات", "بنات كام", "كام بنات", "شات بنات", "ارقام بنات",
        "بنات للتعارف", "بنات واتساب", "قروب بنات", "جروب بنات", "=مزز", "بنات مزز", "مزه جامده",
        "=كحبه", "=كحاب", "=شرموطات", "=قحبات", "=لبوات", "=معرص", "=عرص", "مكوتها", "=تويرك",

        // ---------- Persian / Urdu ----------
        "=سکسی", "فیلم سکسی", "داستان سکسی", "عکس سکسی", "=کص", "=کوس", "=ممه", "=پستان", "=لخت",
        "=لختی", "دختر لخت", "=جق", "=جلق", "=شهوانی", "=پورن", "=پورنو", 
        "=ننگی", "ننگی تصاویر", "=چوت", "=لنڈ", "=گانڈ", "=چدائی", "=سیکسی", "=سیکس", "=رنڈی",

        // ---------- Turkish ----------
        "=sex izle", "=sikiş izle", "=seks izle", "=ifşa", "=liseli ifşa", "=türbanlı ifşa", "=escort bayan",
        "=azgın", "=azgin", "=orospu", "=fahişe", "=fahise", "=sakso", "=oral seks", "=anal seks", "=ciplak",
        "=frikik", "=erotik", "=erotik film", "=göt", "=götü", "=memeler",

        // ---------- French / Spanish / Portuguese / Italian ----------
        "=nichons", "=seins nus", "=pute", "=putes", "=branlette", "=fellation", "=sodomie", "=levrette",
        "=films x", "=sexe gratuit", "=femmes nues", "=fille nue", "=filles nues", "=coquine", "=coquines",
        "=follando", "=mamada", "=verga", "=polla", "=pollas", "=chicas desnudas", "=mujeres desnudas",
        "=tetonas", "=culonas", "=putita", "=zorra", "=zorras", "=cachonda", "=cachondas",
        "=buceta", "=bucetas", "=peitos", "=gostosa", "=gostosas", "=pelada", "=peladas", "=safada",
        "=safadas", "=transando", "=punheta", "=novinha", "=novinhas", "=rabuda",
        "=nuda", "=figa", "=tette", "=cazzo", "=troia", "=troie", "=scopata", "=scopare", "=pompino", "=sesso",

        // ---------- German / Dutch / Nordic / Polish / Hungarian ----------
        "=nackte", "=nacktbilder", "=muschi", "=geil", "=geile", "=sexfilme", "=bumsen", "=nutte", "=nutten",
        "=hure", "=huren", "=naakt", "=neuken", "=kutje", "=naken", "=knulla", "=fitta", "=porr",
        "=cycki", "=cipka", "=ruchanie", "=szex", "=meztelen", "=punci",

        // ---------- Russian ----------
        "порн", "голых", "сиськи", "сиски", "хентай", "секси", "стриптиз",
        "голышом", "обнажен", "дрочит", "мастурб", "интим",

        // ---------- Hindi (latin + Devanagari) / Bengali ----------
        "=chodna", "=choot", "=gaand", "=nangi", "=nangi photo", "=xxx video",
        "सेक्स", "सेक्सी", "नंगी", "चुदाई", "चूत", "लंड", "अश्लील", "पोर्न", "চটি", "সেক্স", "চুদাচুদি", "পর্ন",

        // ---------- Indonesian / Malay / Filipino / Vietnamese / Thai ----------
        "=bokep", "=ngentot", "=memek", "=kontol", "=toket", "=telanjang", "=bugil", "=colmek",
        "=tante girang", "=mesum", "=cewek bugil", "=film dewasa", "=video dewasa", "=lucah",
        "=bogel", "=awek bogel", "=seks melayu", "=kantutan", "=bold movies", "=pinay scandal", "=pinay sex",
        "=iyot", "=hubad", "=libog", "=phim sex", "=sex việt", "=địt", "=lồn", "=khiêu dâm", "=ảnh nóng",
        "=clip nóng", "=gái gọi", "หนังโป๊", "คลิปโป๊", "โป๊", "เย็ด", "ควย", "คลิปหลุด", "นมใหญ่",

        // ---------- Japanese / Chinese / Korean ----------
        "エッチ", "えっち", "おっぱい", "巨乳", "全裸", "ヌード", "AV女優", "中出し", "同人誌", "ポルノ",
        "オナニー", "熟女", "盗撮", "パイパン", "フェラ",
        "黄片", "黃片", "黄色视频", "成人视频", "成人电影", "成人网站", "成人片", "色情片", "裸体", "裸照", "裸聊",
        "做爱", "性爱", "性交", "口交", "肛交", "自慰", "手淫", "淫荡", "骚货", "情色", "三级片", "毛片",
        "約炮", "约炮", "援交", "嫖娼", "鸡巴", "奶子", "操逼", "無碼", "无码", "福利姬",
        "포르노", "야짤", "누드", "성인영상", "성인방송", "섹시", "19금", "벗방", "조건만남", "야설",

        // ---------- Hebrew / Greek ----------
        "סקס", "פורנו", "עירום", "זיון", "ציצים", "זונה", "σεξ", "πορνό", "γυμνές", "τσόντα",
    };

    private BuiltinWords2() { }
}
