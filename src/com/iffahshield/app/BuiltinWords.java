package com.iffahshield.app;

/**
 * Built-in blocklist. Hidden from the UI on purpose (seeing it can itself be a trigger).
 * "=" prefix = whole word only (avoids Scunthorpe-style false hits); otherwise substring.
 * Arabic is written in normalized form: ة→ه، أ/إ/آ→ا، ى→ي.
 */
final class BuiltinWords {
    static final String[] LIST = {
        // ---------- adult sites / brands / domains (substring) ----------
        "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "tube8", "spankbang", "eporner",
        "txxx", "hclips", "hdzog", "upornia", "porntrex", "pornone", "porn300", "4tube", "porntube",
        "tnaflix", "empflix", "drtuber", "sunporno", "nuvid", "motherless.com", "heavy-r", "thumbzilla",
        "youjizz", "porndig", "pornhat", "hqporner", "daftsex", "sxyprn", "porngo", "perfectgirls",
        "anysex", "cumlouder", "xxxbunker", "slutload", "keezmovies", "extremetube", "mofosex",
        "faphouse", "fapello", "fapopedia", "thothub", "coomer.", "leakedmodels", "onlyfans", "fansly",
        "manyvids", "clips4sale", "chaturbate", "stripchat", "bongacams", "livejasmin", "camsoda",
        "myfreecams", "flirt4free", "streamate", "jerkmate", "camster", "xcams", "brazzers", "bangbros",
        "realitykings", "naughtyamerica", "teamskeet", "digitalplayground", "evilangel", "kink.com",
        "nubiles", "metart", "hegre", "playboy", "penthouse", "rule34", "nhentai", "hentaihaven",
        "hanime", "hentai", "gelbooru", "sankakucomplex", "8muses", "multporn", "f95zone", "literotica",
        "imagefap", "erome.", "scrolller", "redgifs", "pornpics", "sexvid", "freesex", "sexcam", "sexchat",
        "sexstories", "xxxvideo", "xxxhd", "hdsex", "sextube", "fuckbook", "adultfriendfinder",
        "ashleymadison", "pornstar", "porn star", "porn", "pr0n", "p0rn", "nsfw", "=xxx", "vixen.com",
        "tushy.com", "blacked.com", "babes.com", "sex.com", "bdsm", "aznude", "celebjihad", "nudostar",
        "camwhores", "xmoviesforyou", "porntop", "pornky", "pornoxo", "javhd", "javhub", "jav.guru",
        "missav", "supjav", "javlibrary", "xozilla", "pornovideos", "pornoxxx", "sexyporn",

        // ---------- English (whole words) ----------
        "=sex", "=sexy", "=sexting", "=sexcam", "=nude", "=nudes", "=nudity", "=naked", "=porno",
        "=milf", "=milfs", "=dilf", "=boobs", "=boobies", "=tits", "=titties", "=titty", "=pussy",
        "=pussies", "=cumshot", "=cumshots", "=creampie", "=blowjob",
        "=blowjobs", "=handjob", "=footjob", "=deepthroat", "=gangbang", "=bukkake", "=orgasm",
        "=orgasms", "=orgy", "=orgies", "=threesome", "=horny", "=slut", "=sluts", "=slutty",
        "=whore", "=whores", "=hooker", "=hookers", "=stripper", "=strippers", "=striptease",
        "=lingerie", "=nipple", "=nipples", "=erotica", "=erotic", "=fetish", "=bondage",
        "=dominatrix", "=futanari", "=ecchi", "=camgirl", "=camgirls", "=livesex", "=thot", "=thots",
        "=masturbate", "=masturbating", "=masturbation", "=fap", "=fapping", "=jerk off", "=jerking off",
        "=+18", "=hot girls", "=hot girl", "=sexy girl", "=sexy girls", "=big ass", "=big boobs",
        "=thong", "=bikini", "=bikinis", "=twerk", "=twerking", "=lewd", "=upskirt", "=voyeur",
        "=camshow", "=sugar daddy", "=hookup", "=hook up tonight", "=stepsis", "=step sister sex",
        "=incest", "=jav", "=hardcore porn", "=softcore", "=sex tape", "=sextape", "=leaked nudes",
        "=nude pics", "=send nudes", "=only fans", "=onlyfans leak", "=nsfw content", "=adult video",
        "=adult videos", "=adult content", "=xxx videos", "=sex video", "=sex videos", "=hot video",
        "=hot videos", "=seductive", "=topless", "=bottomless", "=busty",
        "=nudist", "=naturist", "=pornographic", "=pornography", "=escort girls", "=call girls",
        "=call girl", "=happy ending massage", "=erotic massage", "=body massage", "=sexual content",
        "=sexually explicit", "=explicit content", "=dick pic", "=dick pics", "=boob", "=booty",
        "=ass pics", "=hot body", "=feet pics", "=sugar baby", "=sex chat", "=sex doll", "=sex dolls",
        "=vibrator", "=dildo", "=dildos", "=sex toy", "=sex toys", "=kinky", "=kink", "=fetishes",

        // ---------- Arabic (substring, normalized) ----------
        "=سكس", "سكس عربي", "سكس محارم", "افلام سكس", "فيلم سكس", "مقاطع سكس", "صور سكس", "قصص سكس", "اباحي", "=بورن", "بورنو", "افلام جنس", "فيلم جنس", "فلم جنس", "مقاطع جنس", "مقطع جنس", "صور جنس",
        "فيديو جنس", "فيديوهات جنس", "قصص جنس", "قصص جنسيه", "صور جنسيه", "افلام جنسيه", "مشاهد جنسيه",
        "صور عاري", "صور عاريه", "صور بنات عاري", "مقاطع ساخن", "مقطع ساخن", "فيديو ساخن", "فيديوهات ساخن",
        "افلام ساخن", "فيلم ساخن", "بنات ساخن", "نسوان ساخن", "صور ساخن", "لقطات ساخن", "مشاهد ساخن",
        "مساج ساخن", "شرموط", "قحاب", "منيوك", "نياك", "متناك", "تتناك", "يتناك", "ينيك", "=نيكه",         "نيك طيز", "طيزها", "طيزك", "=كسها", "=كسك", "كس امك", "كس اختك", "=زبي", "=زبه", "=زبك", "بزازها",
        "بزازك", "ممحون", "لحس كس", "مص زب", "مص بزاز", "عاهره", "عاهرات", "داعره", "دعاره", "=سحاق",
        "سحاقيات", "لواط", "رقص اغراء", "رقص مغري", "بنات اغراء", "صور اغراء", "فيديو اغراء", "اغراء بنات",
        "لبس اغراء", "قمصان نوم", "قميص نوم", "ملابس داخليه نسائي", "لانجري", "ستربتيز", "عاريات",
        "صدر عاري", "طياز", "افلام للكبار", "للكبار فقط", "مقاطع للكبار", "فيديو للكبار",
        "قصص محارم", "نيك محارم", "افلام محارم", "رقص شرقي ساخن", "رقص ساخن", "بدله رقص",
        "بنات تيك توك ساخن", "بنات عريانه", "=عريانه", "=عريان", "بنت عاريه", "مشاهير عاريات", "فضايح بنات",
        "فضيحه بنت", "تسريبات بنات", "مقاطع فاضحه", "صور فاضحه", "فيديو فاضح", "مقطع فاضح", "شات جنسي",
        "دردشه جنسيه", "كام جنسي", "جنس فموي", "جنس شرجي", "جنس جماعي", "العاده السريه", "استمناء",
        "نيكني", "انيكك", "انيكها", "متناكه", "شراميط", "قحبه", "منايك",
        "=طيز", "=زبر", "=بزاز", "كسمك", "كسختك", "فحل نيك", "نسوانجي", "نسوان جي",
        "ساخنه جدا", "صور سكسي", "بنات سكسي", "رقص سكسي",

        // ---------- Arabic (whole words) ----------
        "=نيك", "=انيك", "=تنيك", "=نيج", "=ينيج", "=نيجني", "=كس", "=زب", "=بز", "=سكسي", "=عاري",
        "=عاريه", "=عراه", "=ممحونه", "=شهواني", "=شهوانيه", "=مثيره جنسيا", "=تعري", "=متعري",
        "=اغراء", "=قحب", "=شرموطه", "=لبوه", "=متعه جنسيه",

        // ---------- Franco-Arabic / transliteration ----------
        "sharmota", "sharmouta", "sharmoota", "sharmuta", "kos omak", "kos okhtak", "kosomak", "teezha",
        "sex arab", "arab sex", "arabsex", "sex 3arab", "aflam sex", "sexy arab", "arab porn", "egypt sex",
        "masry sex", "sex masry", "neek", "nayek", "=zebi",
        "=teez", "=sharmo", "=a7a sex", "=sex3", "=s3x", "=seks", "=sexx", "=sexxx", "=secks",

        // ---------- Other languages ----------
        "порно", "секс", "голые", "голая", "эротик", "шлюх", "проститутк",                // Russian
        "sikis", "sikiş", "türk porno", "=çıplak", "=seks", "=amcık", "=yarrak",               // Turkish
        "=سکس", "=کس", "=کیر", "سوپر ایرانی", "فیلم سوپر", "=جنده",                      // Persian
        "=salope", "=salopes", "=baise", "=nue", "=nues", "=sexe", "=chatte",        // French
        "=desnuda", "=desnudas", "=desnudo", "=puta", "=putas", "=tetas", "=coño", "=follar",
        "=sexo", "=culo",                                                                       // Spanish
        "=nackt", "=fick", "=ficken", "=titten",                                                // German
        "=chudai", "=chut", "desi mms", "desi sex", "bhabhi sex", "=randi",            // Hindi/Urdu
        "エロ", "アダルト", "セックス", "無修正", "黄色网站", "色情",                                 // JP / CN
        "야동", "섹스",                                                                         // Korean
    };

    private BuiltinWords() { }
}
