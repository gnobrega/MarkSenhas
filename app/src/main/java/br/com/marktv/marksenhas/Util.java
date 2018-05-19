package br.com.marktv.marksenhas;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.ini4j.Ini;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Gustavo on 01/05/2018.
 */

public class Util {

    public static Context context = null;
    public static QueueActivity ACTIVITY = null;

    /*
     * Roda em modo fullscreen
     */
    public static void setFullscreen(Activity activity, View view) {
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        //Remove title bar
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //App.MAIN_ACTIVITY.requestWindowFeature(Window.FEATURE_NO_TITLE);


    }

    /**
     * Recupera a chave do cliente
     */
    public static String loadUrlServer() {
        String path = App.sdcard + "/wikipix.ini";
        String url = "";
        if( !new File(path).exists() ) {
            Util.toast("Arquivo de configuração não encontrado ("+path+")");
        } else {
            try {
                Ini.Section section = loadConfig(path, "queue");
                url = section.get("server");
                if( url == null ) {
                    throw new Exception("Erro ao recuperar a url do servidor");
                }
            } catch (Exception e) {
                Util.toast("Falha na obtenção da URL do servidor no arquivo .INI");
                if( App.ePOSPrintSampleActivity != null ) {
                    App.ePOSPrintSampleActivity.finish();
                }
            }
        }
        return url;
    }

    /**
     * Recupera o IP da impressora
     */
    public static String getPrinterIp() {
        String path = App.sdcard + "/wikipix.ini";
        String ip = "";
        if( !new File(path).exists() ) {
            Util.toast("Arquivo de configuração não encontrado ("+path+")");
        } else {
            try {
                Ini.Section section = loadConfig(path, "queue");
                ip = section.get("printer_ip");
                if( ip == null ) {
                    throw new Exception("Erro ao recuperar o ip da impressora");
                }
            } catch (Exception e) {
                Util.toast(e.getMessage());
            }
        }
        return ip;
    }

    /**
     * Exibe uma mensagem na tela
     */
    public static void toast(String msg) {
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        toast.show();
    }

    /**
     * Carrega os dados de configurações
     */
    public static Ini.Section loadConfig(String strFile, String section) {
        File configFile = new File(strFile);
        if( configFile.exists() ) {
            try {
                Ini configIni = null;
                configIni = new Ini(configFile);
                Ini.Section ftpConfig = configIni.get(section);
                return ftpConfig;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Util.log("Arquivo INI não encontrado ("+strFile+")");
        }
        return null;
    }

    //Registra um evento
    public static void log(String val) {
        Log.e(App.LOG_TAG, val);
    }

    /**
     * Envia uma requisição HTTP
     */
    public static String requestHttp(String urlStr) {
        return requestHttp(urlStr, "GET", null);
    }

    public static String requestHttp(String urlStr, String method, HashMap<String, String> postDataParams) {
        final String url = urlStr;
        final String response = null;
        int BUFFER_SIZE = 2000;
        InputStream in = null;
        Message msg = Message.obtain();
        msg.what=2;
        try {
            in = openHttpConnection(url, method, postDataParams);
            if( in == null ) {
                return null;
            }
            InputStreamReader isr = new InputStreamReader(in);
            int charRead;
            String content = "";
            char[] inputBuffer = new char[BUFFER_SIZE];

            while ((charRead = isr.read(inputBuffer))>0) {
                String readString = String.copyValueOf(inputBuffer, 0, charRead);
                content += readString;
                inputBuffer = new char[BUFFER_SIZE];
            }
            in.close();
            return content;
        }
        catch (IOException e2) {
            e2.printStackTrace();
            return null;
        }

    }

    /**
     * Carrega o conteúdo de uma url
     */
    public static InputStream openHttpConnection(String urlStr, String method, HashMap<String, String> postDataParams) {
        InputStream in = null;
        int resCode = -1;

        try {
            URL url = new URL(urlStr);
            URLConnection urlConn = url.openConnection();

            if (!(urlConn instanceof HttpURLConnection)) {
                throw new IOException("URL is not an Http URL");
            }

            HttpURLConnection httpConn = (HttpURLConnection)urlConn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod(method);

            //Adiciona parâmetros
            if( postDataParams != null ) {
                OutputStream os = httpConn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getPostDataString(postDataParams));
                writer.flush();
                writer.close();
                os.close();
            }

            httpConn.connect();

            resCode = httpConn.getResponseCode();
            if (resCode == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return in;
    }

    static private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for(Map.Entry<String, String> entry : params.entrySet()){
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    public static void toastMainThread(Activity mainActivity, final String msg) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public synchronized void run() {
                Util.toast(msg);
            }
        });
    }

    public static void sleep(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static float getSizeResponsive(Activity activity, float ref) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        float factor = width / 800;
        float newVal = ref * (factor/100);
        return newVal;
    }
}
