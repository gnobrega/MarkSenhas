package br.com.marktv.marksenhas;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Gustavo on 02/09/2016.
 */
public class Util {

    public static Context context = null;
    public static MainActivity ACTIVITY = null;

    //Registra um evento
    public static void log(String val) {
        Log.e(App.LOG_TAG, val);
    }

    //Carrega o conteúdo de um arquivo na pasta assets
    public static String getFileAssetsContent(Activity activity, String file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(activity.getAssets().open(file)));

            // do reading, usually loop until end of file reading
            String mLine;
            String content = "";
            while ((mLine = reader.readLine()) != null) {
                content += mLine;
            }
            return content;
        } catch (IOException e) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {}
            }
        }
    }

    /**
     * Recupera o currentTime
     */
    public static Long getCurrentTime() {
        Time time = new Time();
        time.setToNow();
        return time.toMillis(false);
    }

    /**
     * Carrega o conteudo de um arquivo
     */
    public static String getFileContent(String filePath) {
        File fl = new File(filePath);
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(fl);
            String ret = convertStreamToString(fin);
            //Make sure you close all streams.
            fin.close();
            return ret;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Converte stream em string
     */
    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Exibe uma mensagem na tela
     */
    public static void toast(String msg) {
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        toast.show();
    }

    /**
     * Converte o erro em string
     */
    public static String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static void logFile(String str) {
        if( str != null ) {
            File sdcard = Environment.getExternalStorageDirectory();
            File folderParent = new File(sdcard.getAbsolutePath() + "/wikipix");
            if (!folderParent.exists()) {
                folderParent.mkdir();
            }

            try {
                String strFile = folderParent + "/error.log";

                //Limpa o arquivo se tiver ficado muito grane (10mb)
                File file = new File(strFile);
                if (file.exists() && strFile.length() > 10240) {
                    file.delete();
                }

                //Abre o arquivo
                FileWriter fw = new FileWriter(strFile, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw);

                //Monta o conteudo
                String content = "";
                Date date = new Date();
                content = date.toString() + " >>> " + str + "\n";

                out.append(content);
                Log.e("logFile", content);

                //Fecha o arquivo
                out.close();
                bw.close();
                fw.close();
            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }
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

    public static void sleep(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void toastMainThread(MainActivity mainActivity, final String msg) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public synchronized void run() {
                Util.toast(msg);
            }
        });
    }
}