package src;

import java.util.Map;
import java.util.HashMap;
import src.variable;

public class method {
    public String Name;
    public String Type;
    public Map<String, variable> formalParams;
    public Map<String, variable> definedVars;

    public method(String name, String type) {
        formalParams = new HashMap<String, variable>();
        definedVars = new HashMap<String, variable>();
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
        } else {
            definedVars.put(name, new variable(name, type));
        }
    }

    public void print() {
        System.out.println("Function: "+Name+" "+Type);
        System.out.println("Formal Arguments");
        for (variable value : formalParams.values()) {
            value.print();
        }
        System.out.println();
        System.out.println("Variables");
        for (variable value : definedVars.values()) {
            value.print();
        }
    }
}
