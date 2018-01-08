/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APFE.input.models;

import APFE.DBConf;
import java.util.List;
import static com.mongodb.client.model.Filters.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import org.bson.Document;

/**
 *
 * @author victor
 */
public class OPInfo {

    private static final Logger LOG = Logger.getLogger(OPInfo.class.getName());
    
    private String id;
    private String name;
    private String type;
    private String description;
    private List<Step> proceduralSteps;
    
    /**
     * Recursively create an step object from a mongodb document
     * @param d
     * @return 
     */
    private static Step createStep(Document d) {
        Step s = new Step();
        s.setName(d.getString("name"));
        s.setLabel(d.getString("label"));
        s.setType(d.getString("type"));
        s.setMaximumStepDuration(d.getInteger("ESD"));
        s.setSteps(new ArrayList<>());
        
        ArrayList<Object> subSteps = d.get("steps", ArrayList.class);
        if (subSteps!= null) {
            s.getSteps().addAll(
                    subSteps
                    .stream()
                    .map((Object _o) -> {
                        return createStep((Document) _o);
                    })
                    .collect(Collectors.toList())
            );
        }
        
        return s;
    }
    
    private static Step createStep(JsonObject j) {
        Step s = new Step();
        s.setName(j.getString("name"));
        s.setLabel(j.getString("label"));
        s.setType(j.getString("type"));
        s.setMaximumStepDuration(
            Optional.ofNullable(j.getJsonNumber("maximumStepDuration"))
                .flatMap(x -> Optional.ofNullable(x.intValue()))
                .orElse(null)
        );
        s.setSteps(new ArrayList<>());
        JsonArray subSteps = j.getJsonArray("steps");
        if (subSteps != null) {
            s.getSteps().addAll(
                    subSteps.stream()
                    .map((JsonValue v) -> (JsonObject) v)
                    .map((JsonObject jo) -> createStep(jo))
                    .collect(Collectors.toList())
            );
        }
        
        return s;
    }
    
    /**
     * Get from mongo db database. Only 2-depth EOPS maximum
     * @param id
     * @return 
     */
    public static OPInfo retrieveFromMongoDB(String id) {
        
        Document mongoEOP = DBConf.currentDB.getCollection(DBConf.EOP_COLLECTION_NAME).find(
                eq("_id",id)
        ).first();
        
        if (mongoEOP == null) return null;
        else {
            OPInfo result = new OPInfo(id);
            result.setName(mongoEOP.getString("name"));
            result.setDescription(mongoEOP.getString("description"));
            result.setType(mongoEOP.getString("type"));
            result.setProceduralSteps(new ArrayList<>());
            ArrayList<Object> ia = mongoEOP.get("immediateActions",ArrayList.class);
            if (ia != null) {
                result.setProceduralSteps(
                        ia
                        .stream()
                        .map((Object _o) -> {
                            Document _d = (Document) _o;
                            return createStep(_d);
                        })
                        .collect(Collectors.toList())
                );
            }
            
            return result;
        }
    }
    
    public static OPInfo retrieveFromJSON(String filePath) throws FileNotFoundException {
        
        OPInfo result;
        
        InputStream fis = new FileInputStream(filePath);
        JsonReader reader = Json.createReader(fis);
        JsonObject opObject = reader.readObject();
        reader.close();
        
        if (opObject == null)
            result = null;
        else {
                result = new OPInfo(opObject.getString("_id"));
                result.setName(opObject.getString("name"));
                result.setDescription(opObject.getString("description"));
                result.setType(opObject.getString("type"));
                result.setProceduralSteps(new ArrayList<>());
                JsonArray proceduralSteps = opObject.getJsonArray("proceduralSteps");
                if (proceduralSteps != null) {
                    result.setProceduralSteps(
                        proceduralSteps.stream()
                            .map((JsonValue v) -> (JsonObject) v)
                            .map((JsonObject jo) -> createStep(jo))
                            .collect(Collectors.toList())
                    );
                }
                //IntStream.range(0, proceduralSteps.size()).stream()
            }
                
        return result;
    }
    
    /**
     * Constructors
     */
    public OPInfo(String id) {
        this.id = id;
    }
    
    /**
     * Methods
     */
    public Step findStep(String label) {
        
        Step result = this
                .getProceduralSteps()
                .stream()
                .map((Step _s) -> {
                    return _s.deepFind(label);
                })
                .filter((Step _s) -> _s!=null)
                .findFirst()
                .orElse(null);
        
        return result;
    }
    
    public static class Step {
        private String name;
        private String label;
        private String type;
        private List<Step> steps;
        private Integer maximumStepDuration;

        public Step() {
        }

        public Step deepFind(String label) {
            if (this.labelMatch2(label)) return this;
            else {
                return  this
                        .getSteps()
                        .stream()
                        .map((Step _s) -> {
                            return _s.deepFind(label);
                        })
                        .filter((Step _s) -> {
                            return _s!=null;
                        })
                        .findFirst()
                        .orElse(null);
            }
        }
        
        public boolean labelMatch(String l) {
            //See if the label of this step is contained in l
            String[] labelTokens = l.split(":");
            for (String labelToken : labelTokens) {
                if (labelToken.equals(this.getLabel())) return true;
            }
            return false;
        }
        
        public boolean labelMatch2(String l) {
            return (
                !l.equals("") && 
                !this.getLabel().equals("") &&
                (
                    l.contains(this.getLabel()) || 
                    this.getLabel().contains(l)
                )
            );
        }
        
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<Step> getSteps() {
            return steps;
        }

        public void setSteps(List<Step> steps) {
            this.steps = steps;
        }

        public Integer getMaximumStepDuration() {
            return maximumStepDuration;
        }

        public void setMaximumStepDuration(Integer ESD) {
            this.maximumStepDuration = ESD;
        }
    }

    /**
     * Getters & Setters
     */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Step> getProceduralSteps() {
        return proceduralSteps;
    }

    public void setProceduralSteps(List<Step> immediateActions) {
        this.proceduralSteps = immediateActions;
    }
    
}
