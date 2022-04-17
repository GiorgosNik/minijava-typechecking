package src;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import src.method;
import src.variable;

public class classMap {
    String Name;
    public Map<String, method> methods;
    public Map<String, variable> fields;

    public classMap(String name) {
        Name = name;
        methods = new HashMap<String, method>();
        fields = new HashMap<String, variable>();
    }

    public void addField(String name, String type)throws Exception{
        if (fields.containsKey(name)) {
            throw new Exception("Field Exists");
         } else {
            fields.put(name, new variable(name,type));
         }
    }
    public void addMethod(method newMethod)throws Exception{
        if(methods.containsKey(newMethod.Name)){
            throw new Exception("Method Exists");
        }else{
            methods.put(newMethod.Name, newMethod);
        }
    }
    public void print(){
        System.out.println();
        System.out.println("Class: "+Name);
        System.out.println("Fields:");
        
        for (variable value : fields.values()) {
            value.print();
        }
        System.out.println();
        System.out.println();
        System.out.println("Methods:");
        System.out.println();
        for (method value : methods.values()) {
            value.print();
        }
    }
}