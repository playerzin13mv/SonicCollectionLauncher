package com.sonic.collection;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends Activity {

    static final String[] NAMES   = {"Sonic 1", "Sonic 2", "Sonic CD"};
    static final String[] FOLDERS = {
            "SonicCollectionLauncher/Sonic1",
            "SonicCollectionLauncher/Sonic2",
            "SonicCollectionLauncher/SonicCD"};
    static final String[] PKGS    = {"com.decomp.rsdkv4", "com.decomp.sonic2", "com.decomp.soniccd"};

    final TextView[] status = new TextView[3];
    Button accessBtn;

    int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(8, 12, 36));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("SONIC COLLECTION");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Coloque o seu Data.rsdk na pasta de cada jogo");
        sub.setTextColor(Color.rgb(170, 180, 220));
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(4), 0, dp(20));
        root.addView(sub);

        accessBtn = new Button(this);
        accessBtn.setText("Permitir acesso ao armazenamento");
        accessBtn.setOnClickListener(v -> askAccess());
        root.addView(accessBtn);

        for (int i = 0; i < 3; i++) {
            final int idx = i;

            Button btn = new Button(this);
            btn.setText(NAMES[i]);
            btn.setTextSize(22);
            btn.setAllCaps(false);
            btn.setOnClickListener(v -> play(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(64));
            lp.topMargin = dp(14);
            root.addView(btn, lp);

            TextView st = new TextView(this);
            st.setTextSize(13);
            st.setGravity(Gravity.CENTER);
            st.setPadding(0, dp(4), 0, 0);
            status[i] = st;
            root.addView(st);
        }

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    boolean hasAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    void askAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            Intent it = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(it);
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    boolean isInstalled(String pkg) {
        return getPackageManager().getLaunchIntentForPackage(pkg) != null;
    }

    File dataFile(int i) {
        File dir = new File(Environment.getExternalStorageDirectory(), FOLDERS[i]);
        return new File(dir, "Data.rsdk");
    }

    void refresh() {
        boolean ok = hasAccess();
        accessBtn.setVisibility(ok ? View.GONE : View.VISIBLE);

        for (int i = 0; i < 3; i++) {
            if (!ok) {
                status[i].setText("Sem permissão de armazenamento");
                status[i].setTextColor(Color.rgb(200, 150, 80));
                continue;
            }
            File dir = new File(Environment.getExternalStorageDirectory(), FOLDERS[i]);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            boolean hasData = dataFile(i).exists();
            boolean installed = isInstalled(PKGS[i]);

            String text = (hasData ? "Data.rsdk: OK" : "Data.rsdk: faltando em " + FOLDERS[i])
                    + "\n" + (installed ? "Jogo instalado" : "Jogo não instalado");
            status[i].setText(text);
            status[i].setTextColor(hasData && installed
                    ? Color.rgb(120, 220, 140)
                    : Color.rgb(230, 130, 120));
        }
    }

    void play(int i) {
        if (!hasAccess()) {
            askAccess();
            return;
        }
        if (!dataFile(i).exists()) {
            msg(NAMES[i], "Coloque o Data.rsdk de " + NAMES[i]
                    + " na pasta " + FOLDERS[i] + " para o jogo rodar!");
            return;
        }
        Intent it = getPackageManager().getLaunchIntentForPackage(PKGS[i]);
        if (it == null) {
            msg(NAMES[i], "O jogo ainda não está instalado (pacote " + PKGS[i] + ").");
            return;
        }
        startActivity(it);
    }

    void msg(String title, String text) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(text)
                .setPositiveButton("OK", null)
                .show();
    }
}
