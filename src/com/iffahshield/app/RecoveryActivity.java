package com.iffahshield.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Recovery plan: what works (CBT/ACT + Islamic guidance), your triggers, your if-then plan, and what to do after a slip. */
public class RecoveryActivity extends Activity {
    private Store s;
    private LinearLayout content;

    static final int[] MILESTONES = { 1, 3, 7, 14, 30, 40, 60, 90, 180, 365 };
    static final String[] MEDALS = { "🌱", "🌿", "⭐", "🌟", "🥉", "🥈", "🥇", "🏆", "💎", "👑" };

    private static final String[][] SECTIONS_AR = {
        { "١. افهم الدائرة",
          "تسير العادة هكذا: مُحفّز ← رغبة ← فعل ← راحة قصيرة ← ندم. "
          + "لا نستطيع منع الرغبة من المجيء، لكن نستطيع قطع الدائرة من أولها: نبتعد عن المحفّز، ونعرف ماذا نفعل حين تأتي الرغبة." },
        { "٢. الرغبة موجة وستنحسر",
          "تصل الرغبة إلى ذروتها ثم تنحسر وحدها خلال 10 إلى 20 دقيقة إن لم تطاوعها. "
          + "لا تحارب الفكرة ولا تجادلها، بل لاحظها فقط: «هذه رغبة، وستمضي». تنفّس، وغيّر مكانك، وعُدّ الدقائق. "
          + "تُسمّى هذه المهارة «ركوب الموجة»، وهي جزء من علاج القبول والالتزام (ACT) الذي أثبت فعاليته في تجارب علمية." },
        { "٣. أغلق الأبواب (الأهم)",
          "• لا تدخل بالهاتف إلى السرير ولا إلى الحمّام. اشحنه خارج غرفة النوم.\n"
          + "• نم مبكراً وعلى وضوء، واقرأ أذكار النوم.\n"
          + "• لا تبقَ وحيداً فارغاً. الفراغ والوحدة أكبر محفّزين.\n"
          + "• فعّل «قفل الليل» ليُقفل الهاتف ليلاً تلقائياً.\n"
          + "• غضّ البصر من أول نظرة: «لك الأولى وليست لك الآخرة»." },
        { "٤. ابنِ حياة أقوى من العادة",
          "• رياضة يومية ولو مشياً نصف ساعة، فهي تخفف التوتر وتمنح الراحة بطريقة حلال.\n"
          + "• الصلاة في وقتها، في المسجد ما استطعت، والصحبة الصالحة.\n"
          + "• صيام الاثنين والخميس: «ومن لم يستطع فعليه بالصوم فإنه له وِجاء» (متفق عليه).\n"
          + "• هدف حقيقي في حياتك: عمل، دراسة، حفظ القرآن. النفس إن لم تشغلها بالحق شغلتك بالباطل.\n"
          + "• الزواج لمن استطاع، فهو الحل الذي دلّنا عليه النبي ﷺ." },
        { "٥. إن وقعت (وهذا يحدث)",
          "أخطر ما بعد الزلّة ليس الزلّة نفسها، بل شعور «لقد أفسدت كل شيء، فلأكمل». يسمّيه العلماء «أثر خرق الامتناع»، وهو ما يحوّل زلّة واحدة إلى أسبوع.\n"
          + "• توضّأ وصلِّ ركعتين توبة، وعُد فوراً.\n"
          + "• «قل يا عبادي الذين أسرفوا على أنفسهم لا تقنطوا من رحمة الله» (الزمر 53).\n"
          + "• اسأل نفسك: ما المحفّز؟ ثم أغلق هذا الباب.\n"
          + "• يعود العدّاد إلى الصفر، لكن كل الأيام التي صمدتها محسوبة لك ولم تضع." },
        { "٦. إن لم تستطع وحدك",
          "إذا أصبحت العادة قوية جداً وتؤثر في نومك أو عملك أو نفسيتك، فالحديث مع معالج نفسي (CBT أو ACT) ليس عيباً، "
          + "وهو من أقوى الطرق التي أثبتتها الدراسات. وشريك المحاسبة الموثوق يصنع فرقاً كبيراً." },
    };

    private static final String[][] SECTIONS_EN = {
        { "1. Understand the loop",
          "A habit runs like this: trigger → urge → act → short relief → regret. "
          + "You can't stop urges from coming, but you can break the loop early: avoid the trigger, and know exactly what you'll do when the urge arrives." },
        { "2. An urge is a wave, and it passes",
          "Urges peak and then fade on their own within 10 to 20 minutes if you don't act on them. "
          + "Don't fight or argue with the thought. Just notice it: \"this is an urge, and it will pass\". Breathe, change rooms, count the minutes. "
          + "This is called \"urge surfing\", part of Acceptance and Commitment Therapy (ACT), which has shown results in clinical trials." },
        { "3. Close the doors (most important)",
          "• Never take the phone to bed or to the bathroom. Charge it outside the bedroom.\n"
          + "• Sleep early, in a state of wudu, and read the bedtime adhkar.\n"
          + "• Don't stay alone and idle. Boredom and loneliness are the biggest triggers.\n"
          + "• Turn on \"Night lock\" so the phone locks itself at night.\n"
          + "• Lower your gaze at the first glance: \"The first is for you, the second is against you\"." },
        { "4. Build a life stronger than the habit",
          "• Exercise daily, even a 30-minute walk. It eases stress and gives relief the halal way.\n"
          + "• Pray on time, in the mosque when you can, and keep good company.\n"
          + "• Fast Mondays and Thursdays: \"Whoever cannot, let him fast, for it is a shield for him\" (Bukhari & Muslim).\n"
          + "• Have a real goal: work, study, memorising Quran. An idle soul gets busy with the wrong things.\n"
          + "• Marriage, for whoever is able, is the way the Prophet ﷺ guided us to." },
        { "5. If you slip (it happens)",
          "The most dangerous part of a slip is not the slip itself, but the feeling \"I've ruined it anyway, might as well continue\". Researchers call it the abstinence violation effect, and it turns one slip into a week.\n"
          + "• Make wudu, pray two rak'ahs of repentance, and come back right away.\n"
          + "• \"Say: O My servants who have transgressed against themselves, do not despair of the mercy of Allah\" (39:53).\n"
          + "• Ask yourself: what was the trigger? Then close that door.\n"
          + "• The counter goes back to zero, but every day you held on still counts." },
        { "6. If you can't do it alone",
          "If the habit has become very strong and affects your sleep, work or mood, talking to a therapist (CBT or ACT) is nothing to be ashamed of. "
          + "It is one of the best-supported approaches. A trusted accountability partner also makes a big difference." },
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        s = new Store(this);
        L.init(this);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.BG);
        sv.setFitsSystemWindows(true);
        content = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        content.setPadding(p, Ui.dp(this, 22), p, Ui.dp(this, 36));
        sv.addView(content);
        setContentView(sv);
        render();
    }

    private void render() {
        content.removeAllViews();
        content.addView(Ui.text(this, L.t("خطة التعافي 🌱", "Recovery plan 🌱"), 26, Ui.TEXT, true));
        content.addView(Ui.text(this, L.t("مبنية على العلاج السلوكي المعرفي (CBT) وعلاج القبول والالتزام (ACT)، وعلى هدي القرآن والسنة.",
            "Based on CBT and ACT, the best-studied therapies, and on the guidance of the Quran and Sunnah."), 13, Ui.MUTED, false));
        content.addView(Ui.gap(this, 12));
        content.addView(medals(this, s));

        // ---- 40-day journey (Arabic content) ----
        if (!L.en) {
            LinearLayout jc = Ui.card(this);
            int done = s.journeyDay();
            boolean today = s.journeyDoneToday();
            int idx = Math.min(Journey.DAYS.length - 1, today ? Math.max(0, done - 1) : done);
            String[] day = Journey.DAYS[idx];
            jc.addView(Ui.text(this, "رحلة الحرية: اليوم " + (idx + 1) + " من " + Journey.DAYS.length, 17, Ui.ACCENT, true));
            jc.addView(Ui.text(this, day[0], 19, Ui.TEXT, true));
            jc.addView(Ui.text(this, day[1], 15, Ui.TEXT, false));
            jc.addView(Ui.text(this, "\n🎯 مهمة اليوم: " + day[2], 15, Ui.ACCENT, false));
            if (done >= Journey.DAYS.length && today) jc.addView(Ui.text(this, "\nأتممت الأربعين 🏆 ثبّتك الله.", 15, Ui.ACCENT, true));
            else if (today) jc.addView(Ui.text(this, "\n✅ أنهيت مهمة اليوم. غداً مهمة جديدة.", 14, Ui.MUTED, false));
            else {
                Button jb = Ui.button(this, "أنهيت مهمة اليوم ✅", Ui.ACCENT_DARK, Ui.TEXT);
                jb.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
                    s.completeJourney();
                    Toast.makeText(RecoveryActivity.this, "بارك الله فيك 🤍 خطوة جديدة نحو الحرية.", Toast.LENGTH_SHORT).show();
                    render();
                } });
                jc.addView(jb);
            }
            content.addView(jc);
        }

        // ---- daily self check ----
        LinearLayout cc = Ui.card(this);
        cc.addView(Ui.text(this, L.t("محاسبة اليوم", "Today's self-check"), 17, Ui.TEXT, true));
        cc.addView(Ui.text(this, L.t("آخر 14 يوماً:  ", "Last 14 days:  ") + s.lastDays(14), 15, Ui.TEXT, false));
        int ci = s.checkinToday();
        if (ci >= 0) cc.addView(Ui.text(this, ci == 0 ? L.t("اليوم: نظيف 🟢 الحمد لله.", "Today: clean 🟢 Alhamdulillah.") : ci == 1 ? L.t("اليوم: قاومت وانتصرت 💪", "Today: you resisted and won 💪") : L.t("اليوم: وقعت ثم رجعت 🔴 والتوبة تمحو ما قبلها.", "Today: you slipped and came back 🔴 Repentance wipes it away."), 14, Ui.MUTED, false));
        else {
            cc.addView(Ui.text(this, L.t("كيف كان يومك؟ أجب بصدق، بينك وبين الله.", "How was your day? Answer honestly, between you and Allah."), 14, Ui.MUTED, false));
            String[] labels = { L.t("نظيف 🟢", "Clean 🟢"), L.t("جاءتني رغبة وقاومتها 💪", "I had an urge and resisted 💪"), L.t("وقعت 🔴", "I slipped 🔴") };
            for (int i = 0; i < 3; i++) {
                final int v = i;
                Button cb = Ui.button(this, labels[i], i == 2 ? Ui.alpha(Ui.DANGER, 60) : Ui.ACCENT_DARK, Ui.TEXT);
                cb.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View x) {
                    s.setCheckin(v);
                    if (v == 1) s.logUrgeWon();
                    if (v == 2) slipFlow(); else render();
                } });
                cc.addView(cb);
            }
        }
        int rh = s.riskyHour();
        if (rh >= 0) cc.addView(Ui.text(this, L.t("\n⏰ أخطر وقت عندك حسب سجلّك: حوالي الساعة ", "\n⏰ Your riskiest time by your own log: around ") + rh + L.t(":00. استعد قبله: الهاتف بعيد، وضوء، وعمل آخر.", ":00. Prepare before it: phone away, wudu, something else to do."), 14, Ui.DANGER, false));
        content.addView(cc);

        String[] trig = Store.triggers();
        LinearLayout tc = Ui.card(this);
        tc.addView(Ui.text(this, L.t("ما الذي يحرّكك أكثر؟", "What triggers you most?"), 17, Ui.TEXT, true));
        int total = 0;
        for (int i = 0; i < trig.length; i++) total += s.trigger(i);
        if (total == 0) tc.addView(Ui.text(this, L.t("كلما ضغطت «أشعر بالضعف الآن» اختر ما الذي حرّكك. مع الوقت تظهر الأنماط هنا، فتعرف أي باب عليك أن تغلقه.",
            "Each time you press \"I feel weak right now\", pick what triggered it. Over time the patterns show up here, so you know which door to close."), 14, Ui.MUTED, false));
        else for (int i = 0; i < trig.length; i++) if (s.trigger(i) > 0)
            tc.addView(Ui.text(this, "• " + trig[i] + ": " + s.trigger(i) + L.t(" مرة", "×"), 15, Ui.TEXT, false));
        tc.addView(Ui.text(this, L.t("\nانتصرت على الرغبة " + s.urgesWon() + " مرة 💪", "\nUrges beaten: " + s.urgesWon() + " 💪"), 15, Ui.ACCENT, true));
        content.addView(tc);

        LinearLayout pc = Ui.card(this);
        pc.addView(Ui.text(this, L.t("خطتي: «إذا… سوف…»", "My plan: \"If… then I will…\""), 17, Ui.TEXT, true));
        String plan = s.plan();
        pc.addView(Ui.text(this, plan.length() > 0 ? plan : L.t(
            "مثلاً:\nإذا كنت وحدي ليلاً ← سأخرج الهاتف من الغرفة.\nإذا شعرت بالملل ← سأخرج للمشي.\nإذا رأيت صورة ← سأغلقها فوراً وأقول أستغفر الله.\nالخطط المكتوبة بهذا الشكل ترفع فرص النجاح كثيراً بحسب الدراسات.",
            "For example:\nIf I'm alone at night → I'll put the phone outside the room.\nIf I feel bored → I'll go for a walk.\nIf I see an image → I'll close it at once and say astaghfirullah.\nStudies show plans written this way raise the chance of success a lot."),
            14, plan.length() > 0 ? Ui.TEXT : Ui.MUTED, false));
        Button eb = Ui.button(this, L.t("اكتب خطتك", "Write your plan"), Ui.ACCENT_DARK, Ui.TEXT);
        eb.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { editPlan(); } });
        pc.addView(eb);
        content.addView(pc);

        for (String[] sec : (L.en ? SECTIONS_EN : SECTIONS_AR)) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, sec[0], 17, Ui.TEXT, true));
            c.addView(Ui.text(this, sec[1], 15, Ui.MUTED, false));
            content.addView(c);
        }

        Button slip = Ui.button(this, L.t("زللت 💔 (أريد العودة)", "I slipped 💔 (I want to come back)"), Ui.alpha(Ui.DANGER, 60), Ui.TEXT);
        slip.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { slipFlow(); } });
        content.addView(slip);

        content.addView(Ui.text(this, L.t("\nالمصادر: تجربة عشوائية على علاج ACT (Crosby & Twohig، 2016)، وتحليل شامل لعشرين دراسة عن العلاج النفسي (2025).",
            "\nSources: randomized trial of ACT (Crosby & Twohig, 2016), and a 2025 meta-analysis of 20 psychotherapy studies."), 11, Ui.MUTED, false));
    }

    static View medals(Activity a, Store s) {
        LinearLayout c = Ui.card(a);
        int d = s.streakDays();
        StringBuilder got = new StringBuilder();
        int next = -1;
        for (int i = 0; i < MILESTONES.length; i++) {
            if (d >= MILESTONES[i]) got.append(MEDALS[i]).append(' ');
            else if (next < 0) next = i;
        }
        c.addView(Ui.text(a, L.t("أوسمتك", "Your badges"), 17, Ui.TEXT, true));
        c.addView(Ui.text(a, got.length() > 0 ? got.toString() : L.t("أول وسام بعد يوم واحد 🌱", "First badge after one day 🌱"), 24, Ui.TEXT, false));
        if (next >= 0) {
            int left = MILESTONES[next] - d;
            c.addView(Ui.text(a, L.t("الوسام التالي " + MEDALS[next] + " بعد " + left + " يوم (هدف " + MILESTONES[next] + " يوماً)",
                "Next badge " + MEDALS[next] + " in " + left + " day(s) (goal: " + MILESTONES[next] + " days)"), 14, Ui.MUTED, false));
        } else c.addView(Ui.text(a, L.t("سنة كاملة! ثبّتك الله 👑", "A full year! May Allah keep you firm 👑"), 14, Ui.ACCENT, true));
        return c;
    }

    private void editPlan() {
        final EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        e.setMinLines(4);
        e.setText(s.plan());
        new AlertDialog.Builder(this).setTitle(L.t("إذا… سوف…", "If… then I will…")).setView(e)
            .setPositiveButton(L.t("حفظ", "Save"), new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int w) {
                s.setPlan(e.getText().toString()); render(); } })
            .setNegativeButton(L.t("إلغاء", "Cancel"), null).show();
    }

    private void slipFlow() {
        final String[] trig = Store.triggers();
        new AlertDialog.Builder(this).setTitle(L.t("لا بأس، المهم أن تعود 🤍", "It's okay, what matters is coming back 🤍"))
            .setMessage(L.t("إن الله يحب التوابين. توضّأ وصلِّ ركعتين، وعُد من الآن.\n\nسيعود العدّاد إلى الصفر، وتبقى أطول مدة لك محفوظة. ما الذي حرّكك هذه المرة؟",
                "Allah loves those who turn back to Him. Make wudu, pray two rak'ahs, and start again now.\n\nThe counter resets, your best streak stays saved. What triggered it this time?"))
            .setItems(trig, new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface d, int i) {
                s.logTrigger(i); s.logSlip();
                Toast.makeText(RecoveryActivity.this, L.t("بداية جديدة. أغلق باب «" + trig[i] + "» بخطتك.", "A fresh start. Close the \"" + trig[i] + "\" door with your plan."), Toast.LENGTH_LONG).show();
                render();
            } })
            .setNegativeButton(L.t("رجوع", "Back"), null).show();
    }
}
