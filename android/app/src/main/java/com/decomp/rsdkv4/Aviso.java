package com.decomp.rsdkv4;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

public class Aviso extends Activity {

    private static final long DURACAO_MS = 3500;

    private final Handler handler = new Handler();
    private boolean seguiu = false;
    private final Runnable seguir = new Runnable() {
        public void run() {
            irParaMenu();
        }
    };

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setGravity(Gravity.CENTER);
        raiz.setBackgroundColor(Color.BLACK);
        raiz.setPadding(dp(32), dp(16), dp(32), dp(16));

        TextView aviso = new TextView(this);
        aviso.setText("Não associado a:");
        aviso.setTextColor(Color.WHITE);
        aviso.setTextSize(20);
        aviso.setGravity(Gravity.CENTER);
        raiz.addView(aviso);

        // Marca: imagem escolhida no menu ("Logo do aviso") ou, sem imagem, o nome em texto
        Bitmap bmp = carregarMarca();
        LinearLayout.LayoutParams lpMarca;
        if (bmp != null) {
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(bmp);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            lpMarca = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(110));
            lpMarca.topMargin = dp(14);
            raiz.addView(iv, lpMarca);
        } else {
            TextView t = new TextView(this);
            t.setText("SEGA");
            t.setTextColor(Color.WHITE);
            t.setTextSize(52);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setLetterSpacing(0.12f);
            t.setGravity(Gravity.CENTER);
            lpMarca = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpMarca.topMargin = dp(10);
            raiz.addView(t, lpMarca);
        }

        TextView creditos = new TextView(this);
        creditos.setText("Projeto de fã, sem fins lucrativos.\n"
                + "Sonic e SEGA são marcas de seus respectivos donos.\n\n"
                + "Motor: RSDKv3 e RSDKv4 Decompilation\n"
                + "(Rubberduckycooly e RSDKModding)");
        creditos.setTextColor(Color.rgb(170, 175, 190));
        creditos.setTextSize(12);
        creditos.setGravity(Gravity.CENTER);
        creditos.setPadding(0, dp(18), 0, 0);
        raiz.addView(creditos);

        TextView dica = new TextView(this);
        dica.setText("Toque para continuar");
        dica.setTextColor(Color.rgb(110, 115, 130));
        dica.setTextSize(11);
        dica.setGravity(Gravity.CENTER);
        dica.setPadding(0, dp(14), 0, 0);
        raiz.addView(dica);

        raiz.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                irParaMenu();
            }
        });

        raiz.setAlpha(0f);
        setContentView(raiz);
        raiz.animate().alpha(1f).setDuration(500).start();

        handler.postDelayed(seguir, DURACAO_MS);
    }

    private Bitmap carregarMarca() {
        try {
            File f = new File(Environment.getExternalStorageDirectory(),
                    "SonicOriginsMOBILE/Visual/aviso.img");
            if (!f.exists()) {
                return null;
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            int amostra = 1;
            while (o.outHeight / (amostra * 2) >= 400) {
                amostra *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = amostra;
            return BitmapFactory.decodeFile(f.getAbsolutePath(), o2);
        } catch (Throwable t) {
            return null;
        }
    }

    private void irParaMenu() {
        if (seguiu) {
            return;
        }
        seguiu = true;
        handler.removeCallbacks(seguir);
        startActivity(new Intent(this, Inicio.class));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(seguir);
    }
}
