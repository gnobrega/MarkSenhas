package br.com.marktv.marksenhas;

/**
 * Created by Gustavo on 02/09/2016.
 */
public class App {

    //Tag do log
    public static String LOG_TAG = "[MARKSENHAS_LOG]";

    //Atividade principal
    public static MainActivity MAIN_ACTIVITY = null;

    //Servidor de informações
    public static String URL_SERVER = "http://gestor.wikipix.com.br/subsystem/marktv-senhas/";

    //Lista os serviços
    public static String URL_SERVICES = URL_SERVER + "api.php?action=getServices";

    //Recupera a próxima senha
    public static String URL_GET_NEXT = URL_SERVER + "api.php?action=getNext";

    //Estabelecimento
    public static String unidade = "1";

    //Token
    public static String URL_TOKEN = URL_SERVER + "/api/token";
    public static String URL_TOKEN_CHECK = URL_SERVER + "/api/check";
    public static String ACCESS_GRANT_TYPE = "password";
    public static String ACCESS_USER = "gnobrega";
    public static String ACCESS_PASS = "090288";
    public static String ACCESS_CLIENT_ID = "totem01";
    public static String ACCESS_CLIENT_SECRET = "090288";
    public static Boolean TOKEN_VALID = false;

    //Mensagens
    public static String MSG_DESC_SERVICE = "Selecione o serviço";
    public static String MSG_DESC_TYPE = "Selecione tipo de atendimento";

}
