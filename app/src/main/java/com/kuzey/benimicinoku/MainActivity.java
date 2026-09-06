package com.kuzey.benimicinoku;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final int REQ_SPEECH = 10;
    private static final int REQ_MIC = 11;

    static class Book {
        final String title;
        final String author;
        final String text;
        Book(String title, String author, String text) {
            this.title = title;
            this.author = author;
            this.text = text;
        }
    }

    private final List<Book> books = Arrays.asList(
        new Book("Küçük Prens Demo", "Örnek içerik", "Merhaba. Bu, Benim İçin Oku uygulamasının örnek sesli kitap metnidir. Gerçek sürümde kamu malı eserler veya kullanıcının kendi kitapları eklenebilir."),
        new Book("Sherlock Holmes Demo", "Örnek içerik", "Londra sisli bir sabahta sessizdi. Bu kısa metin yalnızca uygulamanın sesli kitap deneyimini göstermek için hazırlanmıştır."),
        new Book("Masallar Demo", "Örnek içerik", "Bir varmış bir yokmuş. Uzak bir köyde kitapları çok seven bir çocuk yaşarmış. Her gece yeni bir hikâye dinlermiş.")
    );

    private TextToSpeech tts;
    private TextView status;
    private TextView currentBook;
    private Book selectedBook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(this, this);
        buildUi();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private Button button(String text, String description) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(21);
        b.setAllCaps(false);
        b.setMinHeight(dp(64));
        b.setContentDescription(description);
        b.setPadding(dp(18), dp(12), dp(18), dp(12));
        return b;
    }

    private TextView label(String text, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(24,24,24));
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setLineSpacing(dp(3), 1f);
        return t;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(40));
        root.setBackgroundColor(Color.WHITE);
        scroll.addView(root);

        TextView title = label("Benim İçin Oku", 30, true);
        title.setContentDescription("Benim İçin Oku, görme engelliler için erişilebilir sesli kitap uygulaması");
        root.addView(title);

        TextView intro = label("Konuşarak kitap seçebilir, oynatabilir ve uygulamayı mümkün olduğunca ekrana bakmadan kullanabilirsin.", 18, false);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2);
        ip.setMargins(0, dp(8), 0, dp(18));
        root.addView(intro, ip);

        currentBook = label("Seçili kitap: Henüz seçilmedi", 21, true);
        currentBook.setContentDescription("Henüz kitap seçilmedi");
        root.addView(currentBook);

        status = label("Hazır", 18, false);
        status.setTextColor(Color.rgb(11,110,79));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, dp(8), 0, dp(20));
        root.addView(status, sp);

        Button voice = button("🎙 Konuşarak seç", "Konuşarak kitap seç veya oynatma komutu ver");
        voice.setOnClickListener(v -> startVoice());
        root.addView(voice, new LinearLayout.LayoutParams(-1, dp(72)));

        Button play = button("▶ Oynat", "Seçili kitabı sesli oynat");
        play.setOnClickListener(v -> playSelected());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(68));
        bp.setMargins(0, dp(12), 0, 0);
        root.addView(play, bp);

        Button pause = button("⏸ Duraklat", "Sesli okumayı duraklat");
        pause.setOnClickListener(v -> stopReading("Okuma duraklatıldı"));
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(68));
        pp.setMargins(0, dp(12), 0, 0);
        root.addView(pause, pp);

        TextView help = label("Sesli komut örnekleri:\n• “Küçük Prens'i aç”\n• “Sherlock Holmes'u oynat”\n• “Oynat”\n• “Duraklat”\n• “Hangi kitaplar var?”", 18, false);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.setMargins(0, dp(24), 0, 0);
        root.addView(help, hp);

        setContentView(scroll);
    }

    private void startVoice() {
        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Kitap adı veya komut söyle");
        try {
            startActivityForResult(intent, REQ_SPEECH);
            status.setText("Dinliyorum…");
        } catch (Exception e) {
            status.setText("Bu cihazda konuşma tanıma kullanılamıyor");
            speak("Bu cihazda konuşma tanıma kullanılamıyor");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoice();
        } else if (requestCode == REQ_MIC) {
            status.setText("Mikrofon izni verilmedi");
            speak("Konuşarak seçim için mikrofon izni gerekiyor");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) handleVoice(results.get(0));
        }
    }

    private void handleVoice(String spoken) {
        String q = normalize(spoken);
        status.setText("Duydum: " + spoken);

        if (q.contains("hangi kitap") || q.contains("kitaplar var") || q.contains("liste")) {
            StringBuilder sb = new StringBuilder("Mevcut örnek kitaplar: ");
            for (int i = 0; i < books.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(books.get(i).title);
            }
            speak(sb.toString());
            status.setText(sb.toString());
            return;
        }
        if (q.contains("duraklat") || q.equals("dur") || q.contains("durdur")) {
            stopReading("Okuma duraklatıldı");
            return;
        }
        if (q.equals("oynat") || q.contains("devam et") || q.contains("okumaya devam")) {
            playSelected();
            return;
        }

        Book match = findBestBook(q);
        if (match != null) {
            selectedBook = match;
            currentBook.setText("Seçili kitap: " + match.title + " — " + match.author);
            currentBook.setContentDescription("Seçili kitap " + match.title + ", yazar " + match.author);
            String message = match.title + " seçildi";
            status.setText(message);
            if (q.contains("oynat") || q.contains("oku") || q.contains("aç")) {
                playSelected();
            } else {
                speak(message);
            }
            return;
        }

        status.setText("Kitap veya komut bulunamadı");
        speak("Söylediğini anlayamadım. Kitap adını tekrar söyleyebilirsin.");
    }

    private String normalize(String s) {
        return s.toLowerCase(new Locale("tr", "TR"))
                .replace("ı", "i").replace("ş", "s").replace("ğ", "g")
                .replace("ü", "u").replace("ö", "o").replace("ç", "c")
                .replace("’", "'");
    }

    private Book findBestBook(String query) {
        Book best = null;
        int score = 0;
        for (Book b : books) {
            String title = normalize(b.title.replace(" Demo", ""));
            int s = 0;
            for (String part : title.split(" ")) if (query.contains(part)) s++;
            if (query.contains(title)) s += 5;
            if (s > score) { score = s; best = b; }
        }
        return score > 0 ? best : null;
    }

    private void playSelected() {
        if (selectedBook == null) {
            status.setText("Önce bir kitap seç");
            speak("Önce bir kitap seç. Konuşarak seç düğmesine basabilirsin.");
            return;
        }
        status.setText(selectedBook.title + " okunuyor");
        speak(selectedBook.text);
    }

    private void stopReading(String message) {
        if (tts != null) tts.stop();
        status.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void speak(String text) {
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "benimicinoku");
    }

    @Override
    public void onInit(int statusCode) {
        if (statusCode == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("tr", "TR"));
            tts.setSpeechRate(0.95f);
            speak("Benim İçin Oku hazır. Konuşarak kitap seçebilirsin.");
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
