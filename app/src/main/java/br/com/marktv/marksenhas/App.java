package br.com.marktv.marksenhas;

import android.os.Environment;
import android.widget.RelativeLayout;

import java.io.File;

/**
 * Created by Gustavo on 02/09/2016.
 */
public class App {
    public static EPOSPrintSampleActivity ePOSPrintSampleActivity = null;

    //Tag do log
    public static String LOG_TAG = "[MARKSENHAS_LOG]";

    //Sdcard
    public static File sdcard = Environment.getExternalStorageDirectory();

    //Config Ini
    public static String pathFileIni = sdcard.getAbsolutePath() + "/wikipix.ini";

    //Atividade principal
    public static RelativeLayout BODY;

    //Servidor de informações
    //public static String URL_SERVER = "http://gestor.wikipix.com.br/subsystem/marktv-senhas/";
    public static String URL_SERVER = "";

    //Lista os serviços
    public static String URL_SERVICES = "";

    //Recupera a próxima senha
    public static String URL_GET_NEXT = "";

    //Mensagens
    public static String MSG_DESC_SERVICE = "ESCOLHA O SEU ATENDIMENTO";
    public static String MSG_DESC_TYPE = "Selecione tipo de atendimento";

    //Permissões
    public static int PERMISSION_READ_EXTERNAL_STORAGE = 1;

    //Status de impressão
    public static int PRINT_SUCCESS = 16777222;
}
