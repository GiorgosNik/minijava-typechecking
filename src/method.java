package src;

import java.util.Map;
import java.util.LinkedHashMap;
import src.variable;

public class method {
    public String Name;
    public String Type;
    public classMap belongsTo;
    public LinkedHashMap<String, variable> formalParams;
    public LinkedHashMap<String, variable> definedVars;

    public method(String name, String type, classMap scope) {
        belongsTo = scope;
        formalParams = new LinkedHashMap<String, variable>();
        definedVars = new LinkedHashMap<String, variable>();
        Name = name;
        Type = type;
    }

    public void addFormalParam(String name, String type) throws Exception {
        if (formalParams.containsKey(name)) {
            throw new Exception("Parameter Exists");
        } else {
            formalParams.put(name, new variable(name, type));
        }
    }

    public void addDefinedVar(String name, String type) throws Exception {
        if (definedVars.containsKey(name)) {
            throw new Exception("Variable Exists");
        } else if(formalParams.containsKey(name)){
            throw new Exception("Variable Exists");
        }else{
            definedVars.put(name, new variable(name, type));
        }
    }

    public void print() {
        System.out.println("Function: "+Name+" "+Type);
        System.out.println("-----Formal Arguments-----");
        for (variable value : formalParams.values()) {
            value.print();
        }
        System.out.println("----Variables-----");
        for (variable value : definedVars.values()) {
            value.print();
        }
        System.out.println();
    }
}
