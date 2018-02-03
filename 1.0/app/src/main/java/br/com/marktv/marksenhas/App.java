package br.com.marktv.marksenhas;

import android.os.Environment;

import java.io.File;

/**
 * Created by Gustavo on 02/09/2016.
 */
public class App {

    //Tag do log
    public static String LOG_TAG = "[MARKSENHAS_LOG]";

    //Sdcard
    public static File sdcard = Environment.getExternalStorageDirectory();

    //Config Ini
    public static String pathFileIni = sdcard.getAbsolutePath() + "/wikipix.ini";

    //Atividade principal
    public static MainActivity MAIN_ACTIVITY = null;

    //Servidor de informações
    public static String URL_SERVER = "http://gestor.wikipix.com.br/subsystem/marktv-senhas/";

    //Lista os serviços
    public static String URL_SERVICES = URL_SERVER + "api.php?action=getServices";

    //Recupera a próxima senha
    public static String URL_GET_NEXT = URL_SERVER + "api.php?action=getNext";

    //Mensagens
    public static String MSG_DESC_SERVICE = "ESCOLHA O SEU ATENDIMENTO";
    public static String MSG_DESC_TYPE = "Selecione tipo de atendimento";

    //Permissões
    public static int PERMISSION_READ_EXTERNAL_STORAGE = 1;
}
