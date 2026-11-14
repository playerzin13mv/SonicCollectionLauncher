package com.decomp.rsdkv4;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Inicio extends Activity {

    private static final String RAIZ = "SonicOriginsMOBILE";
    private static final String[] JOGOS = {"Sonic the Hedgehog", "Sonic the Hedgehog 2", "Sonic CD"};
    private static final String[] PASTAS = {RAIZ + "/Sonic1", RAIZ + "/Sonic2", RAIZ + "/SonicCD"};
    private static final String PASTA_VISUAL = RAIZ + "/Visual";

    private static final int REQ_CAPA = 100;     // + indice do jogo (100, 101, 102)
    private static final int REQ_LOGO = 150;     // + indice do jogo (150, 151, 152)
    private static final int REQ_MUSICA = 200;
    private static final int REQ_FUNDO = 300;
    private static final int REQ_AVISO = 400;
    private static final int REQ_MOD = 500;      // + indice do jogo (500, 501, 502)
    private static final int REQ_ORIGINS = 600;

    private static final int ESTADO_CREDITOS = 0;
    private static final int ESTADO_START = 1;
    private static final int ESTADO_MENU = 2;
    private static final long DURACAO_CREDITOS_MS = 3500;

    private static final int ESTILO_PRIMARIO = 0;
    private static final int ESTILO_SECUNDARIO = 1;
    private static final int ESTILO_SETA = 2;

    // Nomes aceitos na pasta Visual (o primeiro que existir é usado)
    private static final String[] EXT_IMG = {".img", ".png", ".jpg", ".jpeg", ".webp"};
    private static final String[] EXT_AUDIO = {".audio", ".mp3", ".ogg", ".m4a", ".wav"};
    private static final String[] EXT_VIDEO = {".video", ".mp4", ".webm"};

    // Usados para recriar a tela depois de trocar o idioma
    private static boolean pularCreditos = false;
    private static int estadoSalvo = ESTADO_START;
    private static int jogoSalvo = 0;

    private final Handler handler = new Handler();
    private final Runnable fimCreditos = new Runnable() {
        public void run() {
            if (estado == ESTADO_CREDITOS) {
                irPara(ESTADO_START);
            }
        }
    };

    private int estado = ESTADO_CREDITOS;
    private int atual = 0;
    private boolean pediuAcesso = false;
    private boolean animando = false;
    private boolean musicaLigada = true;
    private MediaPlayer player;
    private SharedPreferences prefs;
    private GestureDetector gestos;

    // vídeo de fundo
    private TextureView fundo;
    private Surface superficie;
    private MediaPlayer videoPlayer;
    private int videoW = 0;
    private int videoH = 0;

    // camadas
    private FrameLayout start;
    private FrameLayout menu;
    private LinearLayout creditos;

    // tela Start
    private ImageView logoOrigins;
    private LinearLayout fallbackOrigins;

    // menu
    private FrameLayout pagina;
    private ImageView capa;
    private ImageView logo;
    private TextView titulo;
    private TextView status;
    private LinearLayout pontosLayout;
    private View[] pontosViews;
    private Button botaoEsq;
    private Button botaoDir;
    private Button botaoAcesso;
    private Button botaoMusica;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------------
    // Idioma (português / inglês)
    // ------------------------------------------------------------------
    private boolean ingles() {
        return "en".equals(prefs.getString("idioma", "pt"));
    }

    private String t(String pt, String en) {
        return ingles() ? en : pt;
    }

    // ------------------------------------------------------------------
    // Estilo dos botões
    // ------------------------------------------------------------------
    private int escurecer(int cor) {
        float[] hsv = new float[3];
        Color.colorToHSV(cor, hsv);
        hsv[2] = hsv[2] * 0.8f;
        return Color.HSVToColor(Color.alpha(cor), hsv);
    }

    private Drawable fundoBotao(int topo, int base, int raioDp, int borda, int bordaDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{topo, base});
        g.setCornerRadius(dp(raioDp));
        if (bordaDp > 0) {
            g.setStroke(dp(bordaDp), borda);
        }
        return g;
    }

    private void estilizar(Button b, int estilo) {
        int topo;
        int base;
        int borda;
        int texto;
        int raio;
        int bordaDp;
        if (estilo == ESTILO_PRIMARIO) {
            topo = Color.parseColor("#FFD23A");
            base = Color.parseColor("#F59E0B");
            borda = Color.parseColor("#FFF3B0");
            texto = Color.parseColor("#0B1A6B");
            raio = 16;
            bordaDp = 2;
        } else if (estilo == ESTILO_SETA) {
            topo = Color.parseColor("#66FFFFFF");
            base = Color.parseColor("#33FFFFFF");
            borda = Color.parseColor("#99FFFFFF");
            texto = Color.WHITE;
            raio = 28;
            bordaDp = 1;
        } else {
            topo = Color.parseColor("#E63B63D8");
            base = Color.parseColor("#E61B3AA8");
            borda = Color.parseColor("#B3FFFFFF");
            texto = Color.WHITE;
            raio = 12;
            bordaDp = 1;
        }

        StateListDrawable sl = new StateListDrawable();
        sl.addState(new int[]{android.R.attr.state_pressed},
                fundoBotao(escurecer(topo), escurecer(base), raio, borda, bordaDp));
        sl.addState(new int[]{},
                fundoBotao(topo, base, raio, borda, bordaDp));
        b.setBackground(sl);
        b.setTextColor(texto);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setStateListAnimator(null);
    }

    private Button seta(String texto, final int delta) {
        Button b = new Button(this);
        b.setText(texto);
        b.setTextSize(26);
        estilizar(b, ESTILO_SETA);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ir(delta);
            }
        });
        return b;
    }

    private Button pequeno(String texto, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(texto);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        estilizar(b, ESTILO_SECUNDARIO);
        b.setPadding(dp(8), dp(7), dp(8), dp(7));
        b.setOnClickListener(l);
        return b;
    }

    private TextView texto(String s, int sp, int cor, boolean negrito) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(cor);
        tv.setGravity(Gravity.CENTER);
        if (negrito) {
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return tv;
    }

    // ------------------------------------------------------------------
    // Montagem da tela
    // ------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("origins", MODE_PRIVATE);
        musicaLigada = prefs.getBoolean("musica", true);

        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundColor(Color.parseColor("#0B1A6B"));

        construirFundo(raiz);
        construirMenu(raiz);
        construirStart(raiz);
        construirCreditos(raiz);

        // Deslizar para o lado também troca de jogo (só no menu)
        gestos = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null) {
                    return false;
                }
                float dx = e2.getX() - e1.getX();
                if (Math.abs(dx) > dp(80) && Math.abs(vx) > Math.abs(vy)) {
                    if (dx < 0) {
                        ir(1);
                    } else {
                        ir(-1);
                    }
                    return true;
                }
                return false;
            }
        });

        setContentView(raiz);

        if (pularCreditos) {
            // tela recriada (troca de idioma): não repete os créditos
            pularCreditos = false;
            atual = jogoSalvo;
            irPara(estadoSalvo);
        } else {
            estado = ESTADO_CREDITOS;
            creditos.setVisibility(View.VISIBLE);
            handler.postDelayed(fimCreditos, DURACAO_CREDITOS_MS);
        }
    }

    // Vídeo de fundo (sem camada escura): é a primeira coisa a carregar, começa por baixo da tela de créditos
    private void construirFundo(FrameLayout raiz) {
        fundo = new TextureView(this);
        fundo.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
                superficie = new Surface(st);
                iniciarVideo();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {
                ajustarVideo();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
                pararVideo();
                if (superficie != null) {
                    superficie.release();
                    superficie = null;
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture st) {
            }
        });
        raiz.addView(fundo, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // Tela de créditos (fundo branco, por cima de tudo)
    private void construirCreditos(FrameLayout raiz) {
        creditos = new LinearLayout(this);
        creditos.setOrientation(LinearLayout.VERTICAL);
        creditos.setGravity(Gravity.CENTER);
        creditos.setBackgroundColor(Color.WHITE);
        creditos.setPadding(dp(32), dp(16), dp(32), dp(16));

        creditos.addView(texto(t("Não associado a:", "Not associated with:"), 20,
                Color.parseColor("#222222"), false));

        // Marca: imagem escolhida em Configurações ou, sem imagem, o nome em texto
        Bitmap bmp = null;
        File fAviso = achar("aviso", EXT_IMG);
        if (fAviso != null) {
            bmp = decodificar(fAviso.getAbsolutePath(), 2);
        }
        if (bmp != null) {
            ImageView iv = new ImageView(this);
            iv.setImageBitmap(bmp);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams lpIv = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(110));
            lpIv.topMargin = dp(14);
            creditos.addView(iv, lpIv);
        } else {
            TextView marca = texto("SEGA", 52, Color.parseColor("#222222"), true);
            marca.setLetterSpacing(0.12f);
            LinearLayout.LayoutParams lpMarca = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpMarca.topMargin = dp(10);
            creditos.addView(marca, lpMarca);
        }

        TextView cred = texto(
                t("Projeto de fã, sem fins lucrativos.\n"
                                + "Sonic e SEGA são marcas de seus respectivos donos.\n\n"
                                + "Motor: RSDKv3 e RSDKv4 Decompilation\n"
                                + "(Rubberduckycooly e RSDKModding)",
                        "Fan project, non-profit.\n"
                                + "Sonic and SEGA are trademarks of their respective owners.\n\n"
                                + "Engine: RSDKv3 and RSDKv4 Decompilation\n"
                                + "(Rubberduckycooly and RSDKModding)"),
                12, Color.parseColor("#555555"), false);
        cred.setPadding(0, dp(18), 0, 0);
        creditos.addView(cred);

        TextView dica = texto(t("Toque para continuar", "Tap to continue"), 11,
                Color.parseColor("#888888"), false);
        dica.setPadding(0, dp(14), 0, 0);
        creditos.addView(dica);

        creditos.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (estado == ESTADO_CREDITOS) {
                    irPara(ESTADO_START);
                }
            }
        });
        creditos.setVisibility(View.GONE);
        raiz.addView(creditos, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // Ícone redondo dourado dos itens do menu (desenhado no código: 0 = play, 1 = mais)
    private static class IconeView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final int tipo;

        IconeView(Context c, int tipo) {
            super(c);
            this.tipo = tipo;
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth();
            float h = getHeight();
            float r = Math.min(w, h) / 2f;
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.parseColor("#F5B800"));
            c.drawCircle(w / 2f, h / 2f, r, p);
            p.setColor(Color.parseColor("#0B1A6B"));
            if (tipo == 0) {
                path.reset();
                path.moveTo(w * 0.40f, h * 0.28f);
                path.lineTo(w * 0.40f, h * 0.72f);
                path.lineTo(w * 0.74f, h * 0.50f);
                path.close();
                c.drawPath(path, p);
            } else {
                float e = r * 0.28f;
                c.drawRect(w / 2f - r * 0.5f, h / 2f - e / 2f, w / 2f + r * 0.5f, h / 2f + e / 2f, p);
                c.drawRect(w / 2f - e / 2f, h / 2f - r * 0.5f, w / 2f + e / 2f, h / 2f + r * 0.5f, p);
            }
        }
    }

    // Item do menu no estilo do original: barra escura arredondada com ícone dourado
    private LinearLayout itemMenu(String rotulo, int tipoIcone, boolean destaque, View.OnClickListener l) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(10), 0, dp(14), 0);
        item.setClickable(true);

        int corNormal = destaque ? Color.parseColor("#E60B1238") : Color.parseColor("#B3000000");
        int corPress = destaque ? Color.parseColor("#E61B2A6B") : Color.parseColor("#B3303030");
        int borda = destaque ? Color.parseColor("#FFD23A") : Color.parseColor("#66FFFFFF");
        int bordaDp = destaque ? 2 : 1;

        StateListDrawable sl = new StateListDrawable();
        sl.addState(new int[]{android.R.attr.state_pressed},
                fundoBotao(corPress, corPress, 12, borda, bordaDp));
        sl.addState(new int[]{},
                fundoBotao(corNormal, corNormal, 12, borda, bordaDp));
        item.setBackground(sl);

        IconeView ic = new IconeView(this, tipoIcone);
        item.addView(ic, new LinearLayout.LayoutParams(dp(30), dp(30)));

        TextView tv = texto(rotulo, 17, Color.WHITE, true);
        tv.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        tv.setShadowLayer(3f, 0f, 1f, Color.BLACK);
        tv.setPadding(dp(12), 0, 0, 0);
        item.addView(tv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        item.setOnClickListener(l);
        return item;
    }

    // Tela Start (igual ao original): logo grande no alto, "TAP ANYWHERE" embaixo; tocar em qualquer lugar entra
    private void construirStart(FrameLayout raiz) {
        start = new FrameLayout(this);
        start.setVisibility(View.GONE);
        start.setClickable(true);
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                irPara(ESTADO_MENU);
            }
        });

        int alt = getResources().getDisplayMetrics().heightPixels;

        // Logo do Sonic Origins (imagem que VOCÊ escolhe), grande e no alto
        logoOrigins = new ImageView(this);
        logoOrigins.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams lpLogo = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (alt * 0.64f),
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lpLogo.topMargin = (int) (alt * 0.04f);
        start.addView(logoOrigins, lpLogo);

        // Sem imagem: nome em texto no mesmo lugar
        fallbackOrigins = new LinearLayout(this);
        fallbackOrigins.setOrientation(LinearLayout.VERTICAL);
        fallbackOrigins.setGravity(Gravity.CENTER);
        TextView textoOrigins = texto("SONIC ORIGINS", 48, Color.WHITE, true);
        textoOrigins.setShadowLayer(8f, 0f, 3f, Color.BLACK);
        TextView subOrigins = texto("(UNOFFICIAL)", 18, Color.WHITE, true);
        subOrigins.setShadowLayer(6f, 0f, 2f, Color.BLACK);
        fallbackOrigins.addView(textoOrigins);
        fallbackOrigins.addView(subOrigins);
        FrameLayout.LayoutParams lpFb = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (alt * 0.64f),
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lpFb.topMargin = (int) (alt * 0.04f);
        start.addView(fallbackOrigins, lpFb);

        // "TAP ANYWHERE" piscando (no lugar do "PRESS X" do original)
        TextView toque = texto("TAP ANYWHERE", 20, Color.WHITE, true);
        toque.setLetterSpacing(0.15f);
        toque.setShadowLayer(8f, 0f, 2f, Color.BLACK);
        AlphaAnimation piscar = new AlphaAnimation(1f, 0.15f);
        piscar.setDuration(800);
        piscar.setRepeatMode(Animation.REVERSE);
        piscar.setRepeatCount(Animation.INFINITE);
        toque.startAnimation(piscar);
        FrameLayout.LayoutParams lpToque = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lpToque.bottomMargin = (int) (alt * 0.09f);
        start.addView(toque, lpToque);

        Button config = pequeno(t("Configurações", "Settings"), new View.OnClickListener() {
            public void onClick(View v) {
                abrirConfig();
            }
        });
        FrameLayout.LayoutParams lpCfg = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.RIGHT);
        lpCfg.bottomMargin = dp(10);
        lpCfg.rightMargin = dp(12);
        start.addView(config, lpCfg);

        raiz.addView(start, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // Menu (igual ao original): logo do jogo no alto à esquerda, itens embaixo, capa no alto à direita
    private void construirMenu(FrameLayout raiz) {
        menu = new FrameLayout(this);
        menu.setVisibility(View.GONE);

        // Página: muda quando passamos de um jogo para o outro
        pagina = new FrameLayout(this);
        menu.addView(pagina, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout esquerda = new LinearLayout(this);
        esquerda.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams lpEsquerda = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.LEFT);
        lpEsquerda.leftMargin = dp(28);
        lpEsquerda.topMargin = dp(12);
        pagina.addView(esquerda, lpEsquerda);

        TextView rotulo = texto("MAIN MENU", 11, Color.WHITE, true);
        rotulo.setGravity(Gravity.LEFT);
        rotulo.setShadowLayer(4f, 0f, 1f, Color.BLACK);
        esquerda.addView(rotulo);

        // Logo do jogo (imagem que VOCÊ escolhe); sem logo, aparece o nome em texto
        logo = new ImageView(this);
        logo.setScaleType(ImageView.ScaleType.FIT_START);
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(dp(300), dp(96));
        lpLogo.topMargin = dp(6);
        esquerda.addView(logo, lpLogo);

        titulo = texto("", 26, Color.WHITE, true);
        titulo.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        titulo.setShadowLayer(6f, 0f, 2f, Color.BLACK);
        LinearLayout.LayoutParams lpTitulo = new LinearLayout.LayoutParams(dp(300), dp(96));
        lpTitulo.topMargin = dp(6);
        esquerda.addView(titulo, lpTitulo);

        // Itens do menu: START GAME (no lugar do Anniversary Mode) e MODS (no lugar do Classic Mode)
        LinearLayout itemStart = itemMenu("START GAME", 0, true, new View.OnClickListener() {
            public void onClick(View v) {
                jogar();
            }
        });
        LinearLayout.LayoutParams lpItem1 = new LinearLayout.LayoutParams(dp(300), dp(46));
        lpItem1.topMargin = dp(10);
        esquerda.addView(itemStart, lpItem1);

        LinearLayout itemMods = itemMenu("MODS", 1, false, new View.OnClickListener() {
            public void onClick(View v) {
                abrirMods();
            }
        });
        LinearLayout.LayoutParams lpItem2 = new LinearLayout.LayoutParams(dp(300), dp(46));
        lpItem2.topMargin = dp(10);
        esquerda.addView(itemMods, lpItem2);

        status = texto("", 12, Color.WHITE, true);
        status.setGravity(Gravity.LEFT);
        status.setShadowLayer(4f, 0f, 1f, Color.BLACK);
        LinearLayout.LayoutParams lpStatus = new LinearLayout.LayoutParams(dp(300),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lpStatus.topMargin = dp(10);
        esquerda.addView(status, lpStatus);

        // Capa do jogo no alto à direita
        capa = new ImageView(this);
        capa.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams lpCapa = new FrameLayout.LayoutParams(
                dp(135), dp(190), Gravity.TOP | Gravity.RIGHT);
        lpCapa.topMargin = dp(46);
        lpCapa.rightMargin = dp(28);
        pagina.addView(capa, lpCapa);

        // Botões do topo, à direita: música e configurações
        LinearLayout barraTopo = new LinearLayout(this);
        barraTopo.setOrientation(LinearLayout.HORIZONTAL);
        botaoMusica = pequeno("", new View.OnClickListener() {
            public void onClick(View v) {
                alternarMusica();
            }
        });
        Button config = pequeno(t("Configurações", "Settings"), new View.OnClickListener() {
            public void onClick(View v) {
                abrirConfig();
            }
        });
        LinearLayout.LayoutParams lpBt = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpBt.leftMargin = dp(6);
        barraTopo.addView(botaoMusica, lpBt);
        barraTopo.addView(config, lpBt);
        FrameLayout.LayoutParams lpBarra = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.RIGHT);
        lpBarra.topMargin = dp(8);
        lpBarra.rightMargin = dp(14);
        menu.addView(barraTopo, lpBarra);

        // Setas nos cantos de baixo
        botaoEsq = seta("<", -1);
        FrameLayout.LayoutParams lpEsq = new FrameLayout.LayoutParams(
                dp(52), dp(52), Gravity.BOTTOM | Gravity.LEFT);
        lpEsq.leftMargin = dp(16);
        lpEsq.bottomMargin = dp(12);
        menu.addView(botaoEsq, lpEsq);

        botaoDir = seta(">", 1);
        FrameLayout.LayoutParams lpDir = new FrameLayout.LayoutParams(
                dp(52), dp(52), Gravity.BOTTOM | Gravity.RIGHT);
        lpDir.rightMargin = dp(16);
        lpDir.bottomMargin = dp(12);
        menu.addView(botaoDir, lpDir);

        // Pontinhos do carrossel, embaixo no centro
        pontosLayout = new LinearLayout(this);
        pontosLayout.setOrientation(LinearLayout.HORIZONTAL);
        pontosLayout.setGravity(Gravity.CENTER);
        pontosViews = new View[JOGOS.length];
        for (int i = 0; i < JOGOS.length; i++) {
            View d = new View(this);
            LinearLayout.LayoutParams lpd = new LinearLayout.LayoutParams(dp(9), dp(9));
            lpd.setMargins(dp(5), 0, dp(5), 0);
            pontosLayout.addView(d, lpd);
            pontosViews[i] = d;
        }
        FrameLayout.LayoutParams lpPontos = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lpPontos.bottomMargin = dp(26);
        menu.addView(pontosLayout, lpPontos);

        // Botão de permissão (só aparece sem acesso aos arquivos)
        botaoAcesso = new Button(this);
        botaoAcesso.setText(t("Permitir acesso aos arquivos", "Allow file access"));
        estilizar(botaoAcesso, ESTILO_PRIMARIO);
        botaoAcesso.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pedirAcesso();
            }
        });
        FrameLayout.LayoutParams lpAcesso = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lpAcesso.bottomMargin = dp(60);
        menu.addView(botaoAcesso, lpAcesso);

        raiz.addView(menu, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    // Pontinhos: o jogo atual fica maior e dourado
    private void atualizarPontos() {
        for (int i = 0; i < pontosViews.length; i++) {
            boolean ativo = (i == atual);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(ativo ? Color.parseColor("#FFD23A") : Color.parseColor("#99FFFFFF"));
            if (ativo) {
                g.setStroke(dp(1), Color.WHITE);
            }
            pontosViews[i].setBackground(g);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) pontosViews[i].getLayoutParams();
            int tam = dp(ativo ? 13 : 9);
            lp.width = tam;
            lp.height = tam;
            pontosViews[i].setLayoutParams(lp);
        }
    }

    // ------------------------------------------------------------------
    // Estados: créditos -> start -> menu
    // ------------------------------------------------------------------
    private void irPara(int novo) {
        estado = novo;
        handler.removeCallbacks(fimCreditos);

        if (novo == ESTADO_CREDITOS) {
            creditos.animate().cancel();
            creditos.setAlpha(1f);
            creditos.setVisibility(View.VISIBLE);
        } else if (creditos.getVisibility() == View.VISIBLE) {
            creditos.animate().alpha(0f).setDuration(400).withEndAction(new Runnable() {
                public void run() {
                    creditos.setVisibility(View.GONE);
                }
            }).start();
        }

        exibir(start, novo == ESTADO_START);
        exibir(menu, novo == ESTADO_MENU);

        if (novo == ESTADO_START) {
            carregarLogoOrigins();
        }
        if (novo == ESTADO_MENU) {
            mostrarJogo();
        }
        if (novo == ESTADO_CREDITOS) {
            pararMusica();
        } else {
            tocarMusica();
        }
    }

    private void exibir(View v, boolean mostrar) {
        v.animate().cancel();
        if (!mostrar) {
            v.setVisibility(View.GONE);
            return;
        }
        v.setAlpha(0f);
        v.setVisibility(View.VISIBLE);
        v.animate().alpha(1f).setDuration(300).start();
    }

    @Override
    public void onBackPressed() {
        if (estado == ESTADO_MENU) {
            irPara(ESTADO_START);
        } else if (estado == ESTADO_CREDITOS) {
            irPara(ESTADO_START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (estado == ESTADO_MENU && gestos != null) {
            gestos.onTouchEvent(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        boolean ok = temAcesso();
        botaoAcesso.setVisibility(ok ? View.GONE : View.VISIBLE);
        if (!ok && !pediuAcesso) {
            pediuAcesso = true;
            pedirAcesso();
        }
        if (estado == ESTADO_MENU) {
            mostrarJogo();
        } else if (estado == ESTADO_START) {
            carregarLogoOrigins();
        }
        if (estado != ESTADO_CREDITOS) {
            tocarMusica();
        }
        iniciarVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pararMusica();
        pararVideo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(fimCreditos);
        pararMusica();
        pararVideo();
    }

    // ------------------------------------------------------------------
    // Pastas e arquivos
    // ------------------------------------------------------------------
    private File raizArmazenamento() {
        return Environment.getExternalStorageDirectory();
    }

    private File pasta(int i) {
        return new File(raizArmazenamento(), PASTAS[i]);
    }

    private File arquivoData(int i) {
        return new File(pasta(i), "Data.rsdk");
    }

    private File pastaVisual() {
        return new File(raizArmazenamento(), PASTA_VISUAL);
    }

    // Procura um arquivo da pasta Visual pelo nome-base (ex.: origins_logo.png, capa0.img...)
    private File achar(String base, String[] extensoes) {
        File dir = pastaVisual();
        for (int i = 0; i < extensoes.length; i++) {
            File f = new File(dir, base + extensoes[i]);
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }

    private File destino(String base, String extensao) {
        return new File(pastaVisual(), base + extensao);
    }

    private boolean temAcesso() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Object r = Environment.class.getMethod("isExternalStorageManager").invoke(null);
                return (r instanceof Boolean) && ((Boolean) r).booleanValue();
            } catch (Exception e) {
                return false;
            }
        }
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void pedirAcesso() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Intent i = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION",
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (Exception e) {
                try {
                    startActivity(new Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION"));
                } catch (Exception e2) {
                    Toast.makeText(this, t("Abra as configurações do app e ative o acesso a todos os arquivos",
                            "Open the app settings and allow access to all files"), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    // ------------------------------------------------------------------
    // settings.ini do jogo (HardwareRenderer e Dev Menu)
    // ------------------------------------------------------------------
    private void definirIni(File ini, String secao, String chave, String valor) {
        try {
            ArrayList<String> linhas = new ArrayList<String>();
            if (ini.exists()) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(new FileInputStream(ini), "UTF-8"));
                String l;
                while ((l = br.readLine()) != null) {
                    linhas.add(l);
                }
                br.close();
            }

            int iSecao = -1;
            for (int i = 0; i < linhas.size(); i++) {
                if (linhas.get(i).trim().equalsIgnoreCase("[" + secao + "]")) {
                    iSecao = i;
                    break;
                }
            }

            if (iSecao < 0) {
                if (!linhas.isEmpty() && linhas.get(linhas.size() - 1).trim().length() > 0) {
                    linhas.add("");
                }
                linhas.add("[" + secao + "]");
                linhas.add(chave + "=" + valor);
            } else {
                int fim = linhas.size();
                for (int i = iSecao + 1; i < linhas.size(); i++) {
                    if (linhas.get(i).trim().startsWith("[")) {
                        fim = i;
                        break;
                    }
                }
                boolean achou = false;
                for (int i = iSecao + 1; i < fim; i++) {
                    String tl = linhas.get(i).trim();
                    int eq = tl.indexOf('=');
                    if (eq > 0 && tl.substring(0, eq).trim().equalsIgnoreCase(chave)) {
                        // mantém a grafia da chave que já estava no arquivo
                        linhas.set(i, tl.substring(0, eq).trim() + "=" + valor);
                        achou = true;
                        break;
                    }
                }
                if (!achou) {
                    linhas.add(iSecao + 1, chave + "=" + valor);
                }
            }

            ini.getParentFile().mkdirs();
            OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(ini), "UTF-8");
            for (int i = 0; i < linhas.size(); i++) {
                w.write(linhas.get(i));
                w.write("\n");
            }
            w.close();
        } catch (Exception e) {
            Toast.makeText(this, t("Não consegui editar o settings.ini", "Could not edit settings.ini"),
                    Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------
    // Configurações (idioma e arquivos visuais)
    // ------------------------------------------------------------------
    private void abrirConfig() {
        final ArrayList<String> itens = new ArrayList<String>();
        final ArrayList<Integer> acoes = new ArrayList<Integer>();
        final int jogoAlvo = atual;

        itens.add(ingles() ? "Language: English  (tap for Português)"
                : "Idioma: Português  (toque para English)");
        acoes.add(0);
        itens.add(t("Trocar logo do Sonic Origins (tela inicial)", "Change Sonic Origins logo (start screen)"));
        acoes.add(1);
        itens.add(t("Trocar logo do aviso (créditos)", "Change notice logo (credits)"));
        acoes.add(2);
        itens.add(t("Trocar fundo (vídeo)", "Change background (video)"));
        acoes.add(3);
        itens.add(t("Trocar música", "Change music"));
        acoes.add(4);
        if (estado == ESTADO_MENU) {
            itens.add(t("Trocar capa: ", "Change cover: ") + JOGOS[jogoAlvo]);
            acoes.add(5);
            itens.add(t("Trocar logo: ", "Change logo: ") + JOGOS[jogoAlvo]);
            acoes.add(6);
        }
        itens.add(t("Sonic CD - renderizador: ", "Sonic CD - renderer: ")
                + (prefs.getBoolean("hw", true) ? "HW" : "SW")
                + t("  (toque para trocar)", "  (tap to switch)"));
        acoes.add(7);

        new AlertDialog.Builder(this)
                .setTitle(t("Configurações", "Settings"))
                .setItems(itens.toArray(new String[itens.size()]), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        int a = acoes.get(which).intValue();
                        if (a == 0) {
                            trocarIdioma();
                        } else if (a == 1) {
                            escolherArquivo("image/*", REQ_ORIGINS);
                        } else if (a == 2) {
                            escolherArquivo("image/*", REQ_AVISO);
                        } else if (a == 3) {
                            escolherArquivo("video/*", REQ_FUNDO);
                        } else if (a == 4) {
                            escolherArquivo("audio/*", REQ_MUSICA);
                        } else if (a == 5) {
                            escolherArquivo("image/*", REQ_CAPA + jogoAlvo);
                        } else if (a == 6) {
                            escolherArquivo("image/*", REQ_LOGO + jogoAlvo);
                        } else if (a == 7) {
                            alternarRender();
                        }
                    }
                })
                .setNegativeButton(t("Fechar", "Close"), null)
                .show();
    }

    private void trocarIdioma() {
        prefs.edit().putString("idioma", ingles() ? "pt" : "en").apply();
        pularCreditos = true;
        estadoSalvo = estado;
        jogoSalvo = atual;
        recreate();
    }

    // ------------------------------------------------------------------
    // Renderizador do Sonic CD (HW = hardware, SW = software)
    // ------------------------------------------------------------------
    private void alternarRender() {
        boolean hw = !prefs.getBoolean("hw", true);
        prefs.edit().putBoolean("hw", hw).apply();
        Toast.makeText(this, hw ? t("Sonic CD: renderizador de hardware (HW)", "Sonic CD: hardware renderer (HW)")
                : t("Sonic CD: renderizador de software (SW)", "Sonic CD: software renderer (SW)"),
                Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------
    // Mods (ver pasta, importar .zip e ligar o Dev Menu)
    // ------------------------------------------------------------------
    private void abrirMods() {
        if (!temAcesso()) {
            pedirAcesso();
            return;
        }
        final int jogo = atual;
        File dir = new File(pasta(jogo), "mods");
        dir.mkdirs();

        StringBuilder lista = new StringBuilder();
        int n = 0;
        File[] subs = dir.listFiles();
        if (subs != null) {
            Arrays.sort(subs);
            for (int i = 0; i < subs.length; i++) {
                if (subs[i].isDirectory()) {
                    lista.append("\u2022 ").append(subs[i].getName()).append("\n");
                    n++;
                }
            }
        }

        String msg = t("Pasta dos mods:\n", "Mods folder:\n") + dir.getAbsolutePath() + "/\n\n";
        if (n == 0) {
            msg += t("Nenhum mod encontrado. Toque em \"Importar mod (.zip)\".\n",
                    "No mods found. Tap \"Import mod (.zip)\".\n");
        } else {
            msg += t("Mods encontrados:\n", "Mods found:\n") + lista;
        }
        msg += t("\nDepois de importar, ative o mod dentro do jogo: ative o Dev Menu (botão abaixo), "
                        + "abra o jogo, abra o Dev Menu e escolha Mods.",
                "\nAfter importing, enable the mod inside the game: enable the Dev Menu (button below), "
                        + "open the game, open the Dev Menu and choose Mods.");

        new AlertDialog.Builder(this)
                .setTitle("Mods - " + JOGOS[jogo])
                .setMessage(msg)
                .setPositiveButton(t("Importar mod (.zip)", "Import mod (.zip)"),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                escolherZip(jogo);
                            }
                        })
                .setNeutralButton(t("Ativar Dev Menu", "Enable Dev Menu"),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                definirIni(new File(pasta(jogo), "settings.ini"), "Dev", "DevMenu", "y");
                                Toast.makeText(Inicio.this, t("Dev Menu ativado", "Dev Menu enabled"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
                .setNegativeButton(t("Fechar", "Close"), null)
                .show();
    }

    private void escolherZip(int jogo) {
        try {
            Intent it = new Intent(Intent.ACTION_GET_CONTENT);
            it.setType("*/*");
            it.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/zip", "application/x-zip-compressed", "application/octet-stream"});
            it.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(it, t("Escolher mod (.zip)", "Choose mod (.zip)")),
                    REQ_MOD + jogo);
        } catch (Exception e) {
            Toast.makeText(this, t("Não consegui abrir o seletor de arquivos", "Could not open the file picker"),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void importarMod(final Uri uri, final int jogo) {
        Toast.makeText(this, t("Importando mod...", "Importing mod..."), Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                final String resultado = extrairMod(uri, new File(pasta(jogo), "mods"));
                runOnUiThread(new Runnable() {
                    public void run() {
                        new AlertDialog.Builder(Inicio.this)
                                .setTitle("Mods - " + JOGOS[jogo])
                                .setMessage(resultado)
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
            }
        }).start();
    }

    private String nomeDoArquivo(Uri uri) {
        String nome = null;
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (c != null && c.moveToFirst()) {
                nome = c.getString(0);
            }
        } catch (Exception e) {
            nome = null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
        if (nome == null) {
            nome = "mod";
        }
        if (nome.toLowerCase().endsWith(".zip")) {
            nome = nome.substring(0, nome.length() - 4);
        }
        return nome;
    }

    private String nomeSeguro(String nome) {
        String s = nome.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (s.length() == 0) {
            s = "mod";
        }
        return s;
    }

    // Extrai o zip para a pasta mods. Se o mod.ini estiver solto na raiz do zip,
    // cria uma pasta com o nome do arquivo; se já vier dentro de uma pasta, mantém a pasta.
    private String extrairMod(Uri uri, File modsDir) {
        File tmp = new File(getCacheDir(), "mod_importado.zip");
        ZipFile zf = null;
        try {
            modsDir.mkdirs();
            if (!copiar(uri, tmp)) {
                return t("Não consegui ler o arquivo.", "Could not read the file.");
            }
            zf = new ZipFile(tmp);

            boolean iniNaRaiz = false;
            ArrayList<String> topos = new ArrayList<String>();
            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String nome = e.getName().replace('\\', '/');
                if (nome.length() == 0 || nome.startsWith("__MACOSX/")) {
                    continue;
                }
                if (nome.equalsIgnoreCase("mod.ini")) {
                    iniNaRaiz = true;
                }
                int barra = nome.indexOf('/');
                if (barra > 0) {
                    String topo = nome.substring(0, barra);
                    if (!topo.equals("..") && !topos.contains(topo)) {
                        topos.add(topo);
                    }
                }
            }

            File base = modsDir;
            String nomeMod = "";
            if (iniNaRaiz) {
                nomeMod = nomeSeguro(nomeDoArquivo(uri));
                base = new File(modsDir, nomeMod);
                base.mkdirs();
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < topos.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(topos.get(i));
                }
                nomeMod = sb.toString();
            }
            String baseCanon = base.getCanonicalPath() + File.separator;

            int arquivos = 0;
            byte[] buf = new byte[65536];
            Enumeration<? extends ZipEntry> en2 = zf.entries();
            while (en2.hasMoreElements()) {
                ZipEntry e = en2.nextElement();
                String nome = e.getName().replace('\\', '/');
                if (nome.length() == 0 || nome.startsWith("__MACOSX/")) {
                    continue;
                }
                File saida = new File(base, nome);
                // proteção contra caminhos perigosos dentro do zip
                if (!(saida.getCanonicalPath() + File.separator).startsWith(baseCanon)) {
                    continue;
                }
                if (e.isDirectory()) {
                    saida.mkdirs();
                    continue;
                }
                saida.getParentFile().mkdirs();
                InputStream in = zf.getInputStream(e);
                OutputStream out = new FileOutputStream(saida);
                try {
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                    }
                } finally {
                    try {
                        in.close();
                    } catch (Exception x) {
                    }
                    try {
                        out.close();
                    } catch (Exception x) {
                    }
                }
                arquivos++;
            }

            if (arquivos == 0) {
                return t("O zip está vazio.", "The zip is empty.");
            }
            return t("Mod importado: ", "Mod imported: ") + nomeMod + "\n(" + arquivos
                    + t(" arquivos)", " files)") + "\n\n"
                    + t("Agora ative o mod dentro do jogo (Dev Menu > Mods).",
                    "Now enable the mod inside the game (Dev Menu > Mods).");
        } catch (Exception e) {
            return t("Falha ao importar o mod: ", "Failed to import the mod: ") + e.getMessage();
        } finally {
            try {
                if (zf != null) {
                    zf.close();
                }
            } catch (Exception e) {
            }
            tmp.delete();
        }
    }

    // ------------------------------------------------------------------
    // Navegação entre os jogos
    // ------------------------------------------------------------------
    private void ir(final int delta) {
        final int novo = atual + delta;
        if (novo < 0 || novo >= JOGOS.length || animando) {
            return;
        }
        animando = true;
        final float largura = pagina.getWidth();
        final float sinal = (delta > 0) ? -1f : 1f;

        pagina.animate().translationX(sinal * largura).alpha(0f).setDuration(180)
                .withEndAction(new Runnable() {
                    public void run() {
                        atual = novo;
                        mostrarJogo();
                        pagina.setTranslationX(-sinal * largura);
                        pagina.animate().translationX(0f).alpha(1f).setDuration(180)
                                .withEndAction(new Runnable() {
                                    public void run() {
                                        animando = false;
                                    }
                                }).start();
                    }
                }).start();
    }

    private void mostrarJogo() {
        titulo.setText(JOGOS[atual]);
        botaoEsq.setVisibility(atual > 0 ? View.VISIBLE : View.INVISIBLE);
        botaoDir.setVisibility(atual < JOGOS.length - 1 ? View.VISIBLE : View.INVISIBLE);
        atualizarPontos();

        carregarCapa();
        carregarLogo();
        atualizarStatus();
        atualizarBotaoMusica();
    }

    private void atualizarStatus() {
        if (!temAcesso()) {
            status.setText(t("Sem permissão de acesso aos arquivos", "No file access permission"));
            status.setTextColor(Color.rgb(255, 190, 100));
            return;
        }
        pasta(atual).mkdirs();
        new File(pasta(atual), "mods").mkdirs();

        if (arquivoData(atual).exists()) {
            status.setText("Data.rsdk: OK");
            status.setTextColor(Color.rgb(140, 240, 160));
        } else {
            status.setText(t("Data.rsdk faltando em ", "Data.rsdk missing in ") + PASTAS[atual]);
            status.setTextColor(Color.rgb(255, 150, 140));
        }
    }

    private void jogar() {
        if (!temAcesso()) {
            pedirAcesso();
            return;
        }
        if (!arquivoData(atual).exists()) {
            new AlertDialog.Builder(this)
                    .setTitle(JOGOS[atual])
                    .setMessage(t("Coloque o Data.rsdk de ", "Put the Data.rsdk of ") + JOGOS[atual]
                            + t(" na pasta ", " in the folder ") + PASTAS[atual]
                            + t(" para o jogo rodar!", " for the game to run!")
                            + "\n\n" + pasta(atual).getAbsolutePath() + "/")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Sonic CD: aplica a escolha de renderizador (HW é o necessário para dados de celular)
        if (atual == 2) {
            definirIni(new File(pasta(2), "settings.ini"), "Window", "HardwareRenderer",
                    prefs.getBoolean("hw", true) ? "y" : "n");
        }

        Intent it = new Intent(this, (atual == 2) ? RSDKv3.class : RSDKv4.class);
        it.putExtra("pasta", PASTAS[atual]);
        startActivity(it);
    }

    // ------------------------------------------------------------------
    // Imagens (capas, logos): arquivos que VOCÊ escolhe; ficam na pasta Visual
    // ------------------------------------------------------------------
    private void carregarCapa() {
        Bitmap bmp = null;
        if (temAcesso()) {
            File f = achar("capa" + atual, EXT_IMG);
            if (f != null) {
                bmp = decodificar(f.getAbsolutePath(), 1);
            }
        }
        if (bmp != null) {
            capa.setBackgroundColor(Color.TRANSPARENT);
            capa.setImageBitmap(bmp);
        } else {
            capa.setImageDrawable(null);
            capa.setBackgroundColor(Color.parseColor("#55000000"));
        }
    }

    private void carregarLogo() {
        Bitmap bmp = null;
        if (temAcesso()) {
            File f = achar("logo" + atual, EXT_IMG);
            if (f != null) {
                bmp = decodificar(f.getAbsolutePath(), 2);
            }
        }
        if (bmp != null) {
            logo.setImageBitmap(bmp);
            logo.setVisibility(View.VISIBLE);
            titulo.setVisibility(View.GONE);
        } else {
            logo.setImageDrawable(null);
            logo.setVisibility(View.GONE);
            titulo.setVisibility(View.VISIBLE);
        }
    }

    // Logo do Sonic Origins da tela Start: arquivo origins_logo.png (ou .jpg, .jpeg, .webp)
    private void carregarLogoOrigins() {
        Bitmap bmp = null;
        if (temAcesso()) {
            File f = achar("origins_logo", EXT_IMG);
            if (f != null) {
                bmp = decodificar(f.getAbsolutePath(), 1);
            }
        }
        if (bmp != null) {
            logoOrigins.setImageBitmap(bmp);
            logoOrigins.setVisibility(View.VISIBLE);
            fallbackOrigins.setVisibility(View.GONE);
        } else {
            logoOrigins.setImageDrawable(null);
            logoOrigins.setVisibility(View.GONE);
            fallbackOrigins.setVisibility(View.VISIBLE);
        }
    }

    // reducao: 1 = altura da tela; 2 = metade da altura da tela
    private Bitmap decodificar(String caminho, int reducao) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(caminho, o);
            int alvo = Math.max(300, getResources().getDisplayMetrics().heightPixels / reducao);
            int amostra = 1;
            while (o.outHeight / (amostra * 2) >= alvo) {
                amostra *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = amostra;
            return BitmapFactory.decodeFile(caminho, o2);
        } catch (Throwable th) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Escolher e copiar arquivos
    // ------------------------------------------------------------------
    private void escolherArquivo(String tipo, int codigo) {
        if (!temAcesso()) {
            pedirAcesso();
            return;
        }
        try {
            Intent it = new Intent(Intent.ACTION_GET_CONTENT);
            it.setType(tipo);
            it.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(it, t("Escolher arquivo", "Choose a file")), codigo);
        } catch (Exception e) {
            Toast.makeText(this, t("Não consegui abrir o seletor de arquivos", "Could not open the file picker"),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (req >= REQ_CAPA && req < REQ_CAPA + JOGOS.length) {
            copiarEm(uri, destino("capa" + (req - REQ_CAPA), ".img"), new Runnable() {
                public void run() {
                    carregarCapa();
                }
            });
        } else if (req >= REQ_LOGO && req < REQ_LOGO + JOGOS.length) {
            copiarEm(uri, destino("logo" + (req - REQ_LOGO), ".img"), new Runnable() {
                public void run() {
                    carregarLogo();
                }
            });
        } else if (req == REQ_MUSICA) {
            copiarEm(uri, destino("musica", ".audio"), new Runnable() {
                public void run() {
                    musicaLigada = true;
                    prefs.edit().putBoolean("musica", true).apply();
                    atualizarBotaoMusica();
                    tocarMusica();
                }
            });
        } else if (req == REQ_FUNDO) {
            copiarEm(uri, destino("fundo", ".video"), new Runnable() {
                public void run() {
                    iniciarVideo();
                }
            });
        } else if (req == REQ_AVISO) {
            copiarEm(uri, destino("aviso", ".img"), new Runnable() {
                public void run() {
                    Toast.makeText(Inicio.this, t("Logo do aviso salvo (aparece na próxima abertura)",
                            "Notice logo saved (shown next time you open the app)"), Toast.LENGTH_LONG).show();
                }
            });
        } else if (req == REQ_ORIGINS) {
            copiarEm(uri, destino("origins_logo", ".img"), new Runnable() {
                public void run() {
                    carregarLogoOrigins();
                }
            });
        } else if (req >= REQ_MOD && req < REQ_MOD + JOGOS.length) {
            importarMod(uri, req - REQ_MOD);
        }
    }

    // Copia em segundo plano (vídeos podem ser grandes) e depois executa "depois" na tela
    private void copiarEm(final Uri origem, final File dest, final Runnable depois) {
        Toast.makeText(this, t("Copiando...", "Copying..."), Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                final boolean ok = copiar(origem, dest);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (ok) {
                            depois.run();
                        } else {
                            Toast.makeText(Inicio.this, t("Não consegui copiar o arquivo",
                                    "Could not copy the file"), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    private boolean copiar(Uri origem, File dest) {
        InputStream in = null;
        OutputStream out = null;
        boolean ok = false;
        try {
            dest.getParentFile().mkdirs();
            in = getContentResolver().openInputStream(origem);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
            ok = true;
        } catch (Exception e) {
            ok = false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
            }
        }
        if (!ok) {
            dest.delete();
        }
        return ok;
    }

    // ------------------------------------------------------------------
    // Música do menu (arquivo que VOCÊ escolhe)
    // ------------------------------------------------------------------
    private void atualizarBotaoMusica() {
        botaoMusica.setText(ingles() ? (musicaLigada ? "Music: ON" : "Music: OFF")
                : (musicaLigada ? "Música: ON" : "Música: OFF"));
    }

    private void alternarMusica() {
        if (achar("musica", EXT_AUDIO) == null) {
            escolherArquivo("audio/*", REQ_MUSICA);
            return;
        }
        musicaLigada = !musicaLigada;
        prefs.edit().putBoolean("musica", musicaLigada).apply();
        atualizarBotaoMusica();
        if (musicaLigada) {
            tocarMusica();
        } else {
            pararMusica();
        }
    }

    private void tocarMusica() {
        pararMusica();
        if (!musicaLigada || !temAcesso() || estado == ESTADO_CREDITOS) {
            return;
        }
        File f = achar("musica", EXT_AUDIO);
        if (f == null) {
            return;
        }
        try {
            player = new MediaPlayer();
            player.setDataSource(f.getAbsolutePath());
            player.setLooping(true);
            player.setVolume(0.8f, 0.8f);
            player.prepare();
            player.start();
        } catch (Exception e) {
            pararMusica();
        }
    }

    private void pararMusica() {
        if (player != null) {
            try {
                player.stop();
            } catch (Exception e) {
            }
            try {
                player.release();
            } catch (Exception e) {
            }
            player = null;
        }
    }

    // ------------------------------------------------------------------
    // Vídeo de fundo (arquivo que VOCÊ escolhe; toca em loop e sem som)
    // ------------------------------------------------------------------
    private void iniciarVideo() {
        pararVideo();
        if (superficie == null || !temAcesso()) {
            return;
        }
        File f = achar("fundo", EXT_VIDEO);
        if (f == null) {
            return;
        }
        try {
            videoPlayer = new MediaPlayer();
            videoPlayer.setSurface(superficie);
            videoPlayer.setDataSource(f.getAbsolutePath());
            videoPlayer.setLooping(true);
            videoPlayer.setVolume(0f, 0f);
            videoPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                public void onVideoSizeChanged(MediaPlayer mp, int w, int h) {
                    videoW = w;
                    videoH = h;
                    ajustarVideo();
                }
            });
            videoPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            videoPlayer.prepareAsync();
        } catch (Exception e) {
            pararVideo();
        }
    }

    private void pararVideo() {
        if (videoPlayer != null) {
            try {
                videoPlayer.stop();
            } catch (Exception e) {
            }
            try {
                videoPlayer.release();
            } catch (Exception e) {
            }
            videoPlayer = null;
        }
    }

    // Preenche a tela inteira cortando as sobras (sem esticar o vídeo)
    private void ajustarVideo() {
        int vw = fundo.getWidth();
        int vh = fundo.getHeight();
        if (videoW <= 0 || videoH <= 0 || vw <= 0 || vh <= 0) {
            return;
        }
        float escala = Math.max((float) vw / videoW, (float) vh / videoH);
        float sx = (videoW * escala) / vw;
        float sy = (videoH * escala) / vh;
        Matrix m = new Matrix();
        m.setScale(sx, sy, vw / 2f, vh / 2f);
        fundo.setTransform(m);
    }
}
