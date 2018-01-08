/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import java.util.Arrays;

/**
 *
 * @author victor
 */
public class DBConf {
    
    //public static final String HOST = "savier.ii.uam.es";
    public static final String HOST = "150.244.58.188";
    public static final int PORT = 27017;
    //public static final String DB_NAME = "DroneWatchAndRescue";
    public static final String DB_NAME = "UAS_Training";
    public static final String USERNAME = "admin";
    //public static final char[] PASSWORD = new char[]{'d','w','r'};
    public static final char[] PASSWORD = new char[]
    {'m','o','n','g','o','P','a','s','s','w','o','r','d','1','6','2','0'};
    public static final String SIMULATIONS_COLLECTION = "simulations";
    public static final String SS_COLLECTION_NAME = "simulationSnapshots";
    public static final String DS_COLLECTION_NAME = "droneSnapshots";
    public static final String MBPFE_COLLECTION_NAME = "MBPFE";
    public static final String EOP_COLLECTION_NAME = "EOPs";
    
    public static MongoCredential credential = MongoCredential.createCredential(
            USERNAME,
            "admin", 
            PASSWORD);
    public static MongoClient mongoClient = new MongoClient(
            new ServerAddress(HOST,PORT),
            Arrays.asList(credential)
    );
    public static MongoDatabase currentDB  = mongoClient.getDatabase(DB_NAME);
}
